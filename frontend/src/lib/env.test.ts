import { describe, expect, it } from "vitest";
import { parseEnv } from "./env-schema";

describe("parseEnv", () => {
  it("applies defaults when nothing is set", () => {
    expect(parseEnv({})).toEqual({
      GATEWAY_URL: "http://localhost:8090",
      KEYCLOAK_ISSUER_URI: "http://localhost:8180/realms/movie-booking",
      PORT: 3001,
    });
  });

  it("accepts valid values and coerces PORT to a number", () => {
    const env = parseEnv({
      GATEWAY_URL: "https://api.example.com",
      KEYCLOAK_ISSUER_URI: "https://sso.example.com/realms/x",
      PORT: "4000",
    });
    expect(env.PORT).toBe(4000);
    expect(env.GATEWAY_URL).toBe("https://api.example.com");
  });

  it("returns a frozen object", () => {
    expect(Object.isFrozen(parseEnv({}))).toBe(true);
  });

  it("rejects an invalid URL and names the variable", () => {
    expect(() => parseEnv({ GATEWAY_URL: "not-a-url" })).toThrow(/GATEWAY_URL/);
  });

  it("rejects a non-numeric PORT", () => {
    expect(() => parseEnv({ PORT: "abc" })).toThrow(/PORT/);
  });

  it("lists every invalid variable in one error", () => {
    expect(() => parseEnv({ GATEWAY_URL: "x", KEYCLOAK_ISSUER_URI: "y", PORT: "z" })).toThrow(
      /GATEWAY_URL, KEYCLOAK_ISSUER_URI, PORT/,
    );
  });

  it("never includes the invalid value in the message", () => {
    const bad = "super-secret-bad-value";
    for (const name of ["GATEWAY_URL", "KEYCLOAK_ISSUER_URI", "PORT"]) {
      let message = "";
      try {
        parseEnv({ [name]: bad });
      } catch (e) {
        message = (e as Error).message;
      }
      expect(message).toContain(name);
      expect(message).not.toContain(bad);
    }
  });
});
