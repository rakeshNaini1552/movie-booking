package com.movie.booking.ticketservice;

import com.movie.booking.ticketservice.service.QrSigner;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class QrSignerTest {

    private final QrSigner signer = new QrSigner("unit-test-secret");

    @Test
    void signedPayloadVerifiesToSameTicketId() {
        UUID ticketId = UUID.randomUUID();

        String payload = signer.sign(ticketId);

        assertThat(signer.verify(payload)).contains(ticketId);
    }

    @Test
    void payloadWithChangedTicketIdIsRejected() {
        String payload = signer.sign(UUID.randomUUID());
        String signature = payload.substring(payload.indexOf('.') + 1);

        String tampered = UUID.randomUUID() + "." + signature;

        assertThat(signer.verify(tampered)).isEmpty();
    }

    @Test
    void payloadWithChangedSignatureIsRejected() {
        UUID ticketId = UUID.randomUUID();

        String tampered = ticketId + ".not-the-real-signature";

        assertThat(signer.verify(tampered)).isEmpty();
    }

    @Test
    void payloadSignedWithDifferentSecretIsRejected() {
        UUID ticketId = UUID.randomUUID();
        String payload = new QrSigner("another-secret").sign(ticketId);

        assertThat(signer.verify(payload)).isEmpty();
    }

    @Test
    void garbageInputIsRejectedWithoutThrowing() {
        assertThat(signer.verify(null)).isEmpty();
        assertThat(signer.verify("")).isEmpty();
        assertThat(signer.verify("no-dot-here")).isEmpty();
        assertThat(signer.verify("not-a-uuid.abc")).isEmpty();
        assertThat(signer.verify(".")).isEmpty();
    }
}