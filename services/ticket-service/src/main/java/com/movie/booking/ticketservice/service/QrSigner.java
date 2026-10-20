package com.movie.booking.ticketservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Component
public class QrSigner {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    public QrSigner(@Value("${ticket.qr.secret}") String secret) {
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    public String sign(UUID ticketId) {
        return ticketId + "." + signature(ticketId.toString());
    }

    public Optional<UUID> verify(String payload) {
        if (payload == null) {
            return Optional.empty();
        }
        int dot = payload.indexOf('.');
        if (dot < 0) {
            return Optional.empty();
        }

        UUID ticketId;
        try {
            ticketId = UUID.fromString(payload.substring(0, dot));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        byte[] expected = signature(ticketId.toString()).getBytes(StandardCharsets.UTF_8);
        byte[] actual = payload.substring(dot + 1).getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(expected, actual) ? Optional.of(ticketId) : Optional.empty();
    }

    private String signature(String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Unable to compute QR signature", e);
        }
    }
}
