package com.movie.booking.booking_service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static com.movie.booking.booking_service.TestAuth.asUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BookingListTest {

    private static final String BOOKINGS_URL = "/api/v1/bookings";
    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_PAGE_SIZE = 20;

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    private final UUID userA = UUID.randomUUID();
    private final UUID userB = UUID.randomUUID();
    private final Instant base = Instant.parse("2026-01-01T10:00:00Z");

    @BeforeEach
    void cleanUp() {
        jdbc.update("DELETE FROM booking_seat");
        jdbc.update("DELETE FROM booking");
    }

    @Test
    void without_token_returns_401() throws Exception {
        mockMvc.perform(get(BOOKINGS_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void returns_only_the_callers_bookings() throws Exception {
        UUID mine = insertBooking(userA, "CONFIRMED", base, "A1");
        insertBooking(userB, "CONFIRMED", base.plusSeconds(1), "B1");

        mockMvc.perform(get(BOOKINGS_URL).with(asUser(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].bookingId").value(mine.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get(BOOKINGS_URL).with(asUser(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void item_is_a_summary_with_sorted_seat_ids() throws Exception {
        UUID id = insertBooking(userA, "PENDING", base, "B2", "A1", "B1");

        mockMvc.perform(get(BOOKINGS_URL).with(asUser(userA)))
                .andExpect(jsonPath("$.items[0].bookingId").value(id.toString()))
                .andExpect(jsonPath("$.items[0].showId").exists())
                .andExpect(jsonPath("$.items[0].status").value("PENDING"))
                .andExpect(jsonPath("$.items[0].totalAmount").value(200.0))
                .andExpect(jsonPath("$.items[0].createdAt").exists())
                .andExpect(jsonPath("$.items[0].seatIds[0]").value("A1"))
                .andExpect(jsonPath("$.items[0].seatIds[1]").value("B1"))
                .andExpect(jsonPath("$.items[0].seatIds[2]").value("B2"))
                .andExpect(jsonPath("$.items[0].userId").doesNotExist());
    }

    @Test
    void newest_first_and_each_booking_keeps_its_own_seats() throws Exception {
        UUID oldest = insertBooking(userA, "EXPIRED", base, "A1");
        UUID middle = insertBooking(userA, "CANCELLED", base.plusSeconds(60), "B1", "B2");
        UUID newest = insertBooking(userA, "CONFIRMED", base.plusSeconds(120), "C1");

        mockMvc.perform(get(BOOKINGS_URL).with(asUser(userA)))
                .andExpect(jsonPath("$.items[0].bookingId").value(newest.toString()))
                .andExpect(jsonPath("$.items[0].seatIds.length()").value(1))
                .andExpect(jsonPath("$.items[1].bookingId").value(middle.toString()))
                .andExpect(jsonPath("$.items[1].seatIds.length()").value(2))
                .andExpect(jsonPath("$.items[2].bookingId").value(oldest.toString()))
                .andExpect(jsonPath("$.items[2].seatIds[0]").value("A1"));
    }

    @Test
    void paging_splits_the_list_without_overlap() throws Exception {
        UUID third = insertBooking(userA, "CONFIRMED", base.plusSeconds(2), "A3");
        UUID second = insertBooking(userA, "CONFIRMED", base.plusSeconds(1), "A2");
        UUID first = insertBooking(userA, "CONFIRMED", base, "A1");

        mockMvc.perform(get(BOOKINGS_URL).param("page", "0").param("size", "2").with(asUser(userA)))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].bookingId").value(third.toString()))
                .andExpect(jsonPath("$.items[1].bookingId").value(second.toString()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        mockMvc.perform(get(BOOKINGS_URL).param("page", "1").param("size", "2").with(asUser(userA)))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].bookingId").value(first.toString()));
        mockMvc.perform(get(BOOKINGS_URL).param("page", "5").param("size", "2").with(asUser(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void bookings_created_at_the_same_instant_page_in_a_stable_order() throws Exception {
        for (int i = 0; i < 5; i++) {
            insertBooking(userA, "CONFIRMED", base, "A" + i);
        }

        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int page = 0; page < 5; page++) {
            String body = mockMvc.perform(get(BOOKINGS_URL).param("page", "" + page).param("size", "1").with(asUser(userA)))
                    .andReturn().getResponse().getContentAsString();
            seen.add(body.replaceAll(".*\"bookingId\"\\s*:\\s*\"([^\"]+)\".*", "$1"));
        }
        org.assertj.core.api.Assertions.assertThat(seen).hasSize(5);
    }

    @Test
    void default_size_applies_when_omitted() throws Exception {
        for (int i = 0; i < DEFAULT_PAGE_SIZE + 1; i++) {
            insertBooking(userA, "CONFIRMED", base.plusSeconds(i), "A1");
        }

        mockMvc.perform(get(BOOKINGS_URL).with(asUser(userA)))
                .andExpect(jsonPath("$.items.length()").value(DEFAULT_PAGE_SIZE))
                .andExpect(jsonPath("$.size").value(DEFAULT_PAGE_SIZE))
                .andExpect(jsonPath("$.totalElements").value(DEFAULT_PAGE_SIZE + 1));
    }

    @Test
    void size_above_the_cap_is_reduced_to_the_cap() throws Exception {
        for (int i = 0; i < MAX_PAGE_SIZE + 1; i++) {
            insertBooking(userA, "CONFIRMED", base.plusSeconds(i), "A1");
        }

        mockMvc.perform(get(BOOKINGS_URL).param("size", "1000").with(asUser(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(MAX_PAGE_SIZE))
                .andExpect(jsonPath("$.items.length()").value(MAX_PAGE_SIZE))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void invalid_paging_values_are_400_with_the_usual_error_body() throws Exception {
        for (String[] bad : new String[][]{{"page", "-1"}, {"size", "0"}, {"size", "-5"}, {"page", "abc"}, {"size", "x"}}) {
            mockMvc.perform(get(BOOKINGS_URL).param(bad[0], bad[1]).with(asUser(userA)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").isString());
        }
    }

    private UUID insertBooking(UUID userId, String status, Instant createdAt, String... seatIds) {
        UUID bookingId = UUID.randomUUID();
        Timestamp ts = Timestamp.from(createdAt);
        jdbc.update("""
                INSERT INTO booking (booking_id, user_id, show_id, status, total_amount,
                                     created_at, updated_at, idempotency_key)
                VALUES (?, ?, ?, ?, 200.00, ?, ?, ?)
                """,
                bookingId, userId, UUID.randomUUID(), status, ts, ts, UUID.randomUUID());
        for (String seatId : seatIds) {
            jdbc.update("INSERT INTO booking_seat (booking_id, seat_id) VALUES (?, ?)", bookingId, seatId);
        }
        return bookingId;
    }
}
