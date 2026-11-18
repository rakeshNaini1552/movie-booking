import { describe, expect, it } from "vitest";
import { parseEnv } from "./env-schema";

// Fake values: the secrets have no defaults, so every test that expects success supplies them.
const secrets = {
  AUTH_SECRET: "test-only-secret-with-32-or-more-chars",
  AUTH_KEYCLOAK_SECRET: "test-only-keycloak-client-secret",
};

describe("parseEnv", () => {
  it("applies defaults when only the required secrets are set", () => {
    expect(parseEnv(secrets)).toEqual({
      GATEWAY_URL: "http://localhost:8090",
      GATEWAY_TIMEOUT_MS: 5000,
      GATEWAY_WRITE_TIMEOUT_MS: 15000,
      KEYCLOAK_ISSUER_URI: "http://localhost:8180/realms/movie-booking",
      PORT: 3001,
      AUTH_KEYCLOAK_ID: "movie-booking-web",
      AUTH_URL: "http://localhost:3001",
      CATALOG_ADMIN_ROLE: "catalog-admin",
      ...secrets,
    });
  });

  it("accepts a GATEWAY_WRITE_TIMEOUT_MS override and coerces it to a number", () => {
    expect(
      parseEnv({ ...secrets, GATEWAY_WRITE_TIMEOUT_MS: "20000" }).GATEWAY_WRITE_TIMEOUT_MS,
    ).toBe(20000);
  });

  it("rejects a GATEWAY_WRITE_TIMEOUT_MS outside 100 to 60000, or not a whole number", () => {
    for (const bad of ["99", "60001", "abc", "1500.5"]) {
      expect(() => parseEnv({ ...secrets, GATEWAY_WRITE_TIMEOUT_MS: bad })).toThrow(
        /GATEWAY_WRITE_TIMEOUT_MS/,
      );
    }
  });

  it("fails when the secrets are missing, naming them", () => {
    expect(() => parseEnv({})).toThrow(/AUTH_SECRET, AUTH_KEYCLOAK_SECRET/);
  });

  it("rejects an AUTH_SECRET shorter than 32 characters", () => {
    expect(() => parseEnv({ ...secrets, AUTH_SECRET: "too-short" })).toThrow(/AUTH_SECRET/);
  });

  it("rejects an empty AUTH_KEYCLOAK_SECRET", () => {
    expect(() => parseEnv({ ...secrets, AUTH_KEYCLOAK_SECRET: "" })).toThrow(
      /AUTH_KEYCLOAK_SECRET/,
    );
  });

  it("accepts AUTH_URL and AUTH_KEYCLOAK_ID overrides", () => {
    const env = parseEnv({
      ...secrets,
      AUTH_URL: "https://movies.example.com",
      AUTH_KEYCLOAK_ID: "other-client",
    });
    expect(env.AUTH_URL).toBe("https://movies.example.com");
    expect(env.AUTH_KEYCLOAK_ID).toBe("other-client");
  });

  it("accepts valid values and coerces PORT to a number", () => {
    const env = parseEnv({
      ...secrets,
      GATEWAY_URL: "https://api.example.com",
      KEYCLOAK_ISSUER_URI: "https://sso.example.com/realms/x",
      PORT: "4000",
    });
    expect(env.PORT).toBe(4000);
    expect(env.GATEWAY_URL).toBe("https://api.example.com");
  });

  it("returns a frozen object", () => {
    expect(Object.isFrozen(parseEnv(secrets))).toBe(true);
  });

  it("rejects an invalid URL and names the variable", () => {
    expect(() => parseEnv({ ...secrets, GATEWAY_URL: "not-a-url" })).toThrow(/GATEWAY_URL/);
  });

  it("rejects a non-numeric PORT", () => {
    expect(() => parseEnv({ ...secrets, PORT: "abc" })).toThrow(/PORT/);
  });

  it("lists every invalid variable in one error", () => {
    expect(() =>
      parseEnv({ ...secrets, GATEWAY_URL: "x", KEYCLOAK_ISSUER_URI: "y", PORT: "z" }),
    ).toThrow(/GATEWAY_URL, KEYCLOAK_ISSUER_URI, PORT/);
  });

  it("never includes the invalid value in the message", () => {
    const bad = "super-secret-bad-value";
    for (const name of ["GATEWAY_URL", "KEYCLOAK_ISSUER_URI", "PORT", "AUTH_URL", "AUTH_SECRET"]) {
      let message = "";
      try {
        parseEnv({ ...secrets, [name]: bad });
      } catch (e) {
        message = (e as Error).message;
      }
      expect(message).toContain(name);
      expect(message).not.toContain(bad);
    }
  });
});
