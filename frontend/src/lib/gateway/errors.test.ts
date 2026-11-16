import { describe, expect, it } from "vitest";
import { GatewayError, gatewayErrorMessage, isGatewayDigest } from "./errors";

describe("GatewayError digest", () => {
  it("carries the failure kind, so the error page can pick a message", () => {
    expect(new GatewayError("timeout").digest).toBe("GATEWAY_TIMEOUT");
    expect(new GatewayError("invalid-response").digest).toBe("GATEWAY_INVALID_RESPONSE");
  });
});

describe("gatewayErrorMessage", () => {
  it("has a different message for timeout, unreachable and everything else", () => {
    const messages = new Set([
      gatewayErrorMessage("GATEWAY_TIMEOUT"),
      gatewayErrorMessage("GATEWAY_UNREACHABLE"),
      gatewayErrorMessage("GATEWAY_HTTP"),
    ]);
    expect(messages.size).toBe(3);
    expect(gatewayErrorMessage(undefined)).toBe(gatewayErrorMessage("GATEWAY_GRAPHQL"));
    expect(gatewayErrorMessage("12345")).toBe(gatewayErrorMessage("GATEWAY_HTTP"));
  });

  it("recognises gateway digests", () => {
    expect(isGatewayDigest("GATEWAY_HTTP")).toBe(true);
    expect(isGatewayDigest("1234567")).toBe(false);
    expect(isGatewayDigest(undefined)).toBe(false);
  });
});
