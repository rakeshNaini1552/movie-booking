import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const { getAccessToken } = vi.hoisted(() => ({ getAccessToken: vi.fn() }));
vi.mock("server-only", () => ({}));
vi.mock("@/lib/auth/access-token", () => ({ getAccessToken }));
vi.mock("@/lib/env", () => ({
  env: {
    GATEWAY_URL: "http://gateway.test:8090",
    GATEWAY_TIMEOUT_MS: 1000,
    GATEWAY_WRITE_TIMEOUT_MS: 2500,
  },
}));

import { GatewayError } from "@/lib/gateway/errors";
import { cancelBooking, createBooking, getBooking, listBookings, payBooking } from "./api";

const BOOKING = "3f1c2b0e-8a45-4d6e-9b7a-1c2d3e4f5a6b";
const SHOW = "00000000-0000-0000-0000-000000000001";
const KEY = "9b2e5c1a-7d34-4f6a-8b90-1a2b3c4d5e6f";
const fetchMock = vi.fn();
const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });
const lastCall = () => fetchMock.mock.calls.at(-1) as [URL, RequestInit];

describe("booking API", () => {
  beforeEach(() => {
    getAccessToken.mockResolvedValue("test-token");
    vi.stubGlobal("fetch", fetchMock);
    vi.spyOn(console, "error").mockImplementation(() => {});
  });
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
    vi.clearAllMocks();
  });

  it("creates a booking with the seats in the body and the idempotency key in a header", async () => {
    fetchMock.mockResolvedValue(json({ bookingId: BOOKING }, 201));
    await expect(
      createBooking({ showId: SHOW, seatIds: ["A1", "A2"], idempotencyKey: KEY }),
    ).resolves.toEqual({ ok: true, bookingId: BOOKING });

    const [url, init] = lastCall();
    expect(url.pathname).toBe("/api/v1/bookings");
    expect(init.method).toBe("POST");
    expect(JSON.parse(init.body as string)).toEqual({ showId: SHOW, seatIds: ["A1", "A2"] });
    const headers = init.headers as Record<string, string>;
    expect(headers["idempotency-key"]).toBe(KEY);
    expect(headers.authorization).toBe("Bearer test-token");
  });

  it("gives creating and paying a booking the write timeout, and other calls the normal one", async () => {
    const timeoutSpy = vi.spyOn(AbortSignal, "timeout");

    fetchMock.mockResolvedValue(json({ bookingId: BOOKING }, 201));
    await createBooking({ showId: SHOW, seatIds: ["A1"], idempotencyKey: KEY });
    expect(timeoutSpy).toHaveBeenLastCalledWith(2500);

    fetchMock.mockResolvedValue(
      json({ bookingId: BOOKING, status: "CONFIRMED", paymentStatus: "SUCCESS" }),
    );
    await payBooking({ bookingId: BOOKING, paymentToken: "tok_success", idempotencyKey: KEY });
    expect(timeoutSpy).toHaveBeenLastCalledWith(2500);

    fetchMock.mockResolvedValue(json({ bookingId: BOOKING, status: "CANCELLED" }));
    await cancelBooking(BOOKING);
    expect(timeoutSpy).toHaveBeenLastCalledWith(1000);

    fetchMock.mockResolvedValue(new Response("{}", { status: 404 }));
    await getBooking(BOOKING);
    expect(timeoutSpy).toHaveBeenLastCalledWith(1000);
  });

  it("reports taken seats (409) as a result, and an outage as an error", async () => {
    fetchMock.mockResolvedValue(json({ message: "Seats unavailable" }, 409));
    await expect(
      createBooking({ showId: SHOW, seatIds: ["A1"], idempotencyKey: KEY }),
    ).resolves.toEqual({ ok: false, reason: "seats-unavailable" });

    fetchMock.mockResolvedValue(json({ message: "down" }, 503));
    await expect(
      createBooking({ showId: SHOW, seatIds: ["A1"], idempotencyKey: KEY }),
    ).rejects.toBeInstanceOf(GatewayError);
  });

  it("treats a declined card as an answer, not an error", async () => {
    fetchMock.mockResolvedValue(
      json({ bookingId: BOOKING, status: "PENDING", paymentStatus: "FAILED" }),
    );
    await expect(
      payBooking({ bookingId: BOOKING, paymentToken: "tok_decline", idempotencyKey: KEY }),
    ).resolves.toEqual({ ok: true, status: "PENDING", paid: false });
    expect(lastCall()[0].pathname).toBe(`/api/v1/bookings/${BOOKING}/payments`);
  });

  it("maps a booking that cannot be paid or cancelled (409)", async () => {
    fetchMock.mockResolvedValue(json({ message: "no" }, 409));
    await expect(
      payBooking({ bookingId: BOOKING, paymentToken: "tok_success", idempotencyKey: KEY }),
    ).resolves.toEqual({ ok: false, reason: "not-payable" });
    fetchMock.mockResolvedValue(json({ message: "no" }, 409));
    await expect(cancelBooking(BOOKING)).resolves.toEqual({ ok: false, reason: "not-cancellable" });
  });

  it("returns null for a booking that is missing or belongs to someone else (404)", async () => {
    fetchMock.mockResolvedValue(json({ message: "Booking not found" }, 404));
    await expect(getBooking(BOOKING)).resolves.toBeNull();
  });

  it("lists bookings with page and size, and rejects a body of the wrong shape", async () => {
    const item = {
      bookingId: BOOKING,
      showId: SHOW,
      status: "CONFIRMED",
      totalAmount: 500,
      seatIds: ["A1", "A2"],
      createdAt: "2026-11-17T04:38:43.780479Z",
    };
    fetchMock.mockResolvedValue(
      json({ items: [item], page: 1, size: 5, totalElements: 6, totalPages: 2 }),
    );
    const page = await listBookings(1, 5);
    expect(page.items).toEqual([item]);
    expect(lastCall()[0].search).toBe("?page=1&size=5");

    fetchMock.mockResolvedValue(json({ items: [{ ...item, status: "UNKNOWN" }] }));
    await expect(listBookings(0, 5)).rejects.toBeInstanceOf(GatewayError);
  });
});
