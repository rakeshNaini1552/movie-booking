import { afterEach, describe, expect, it, vi } from "vitest";
import {
  type AppToken,
  type KeycloakConfig,
  REFRESH_LEEWAY_SECONDS,
  extractRealmRoles,
  isExpiring,
  refreshTokens,
  rolesFromAccessToken,
} from "./tokens";

const config: KeycloakConfig = {
  issuer: "http://keycloak.test/realms/movie-booking",
  clientId: "test-client",
  clientSecret: "test-only-client-secret",
};

// A fake access token: the payload is all that matters, the signature is not checked.
const jwtWith = (payload: object) =>
  `header.${Buffer.from(JSON.stringify(payload)).toString("base64url")}.signature`;

const tokenResponse = (body: object, status = 200) =>
  Promise.resolve(new Response(JSON.stringify(body), { status }));

afterEach(() => vi.restoreAllMocks());

describe("role mapping", () => {
  it("reads realm roles from the access token payload", () => {
    const token = jwtWith({ realm_access: { roles: ["customer", "staff"] } });
    expect(rolesFromAccessToken(token)).toEqual(["customer", "staff"]);
  });

  it("ignores client roles and non-string entries", () => {
    expect(
      extractRealmRoles({
        realm_access: { roles: ["customer", 7, null] },
        resource_access: { "movie-booking-web": { roles: ["not-a-realm-role"] } },
      }),
    ).toEqual(["customer"]);
  });

  it("returns no roles for a missing claim, a malformed claim or a broken token", () => {
    expect(extractRealmRoles({})).toEqual([]);
    expect(extractRealmRoles({ realm_access: { roles: "customer" } })).toEqual([]);
    expect(extractRealmRoles(null)).toEqual([]);
    expect(rolesFromAccessToken("not-a-jwt")).toEqual([]);
  });
});

describe("isExpiring", () => {
  const token = (expiresAt?: number): AppToken => ({ expiresAt });

  it("is false while more than the leeway remains", () => {
    expect(isExpiring(token(1_000 + REFRESH_LEEWAY_SECONDS + 1), 1_000)).toBe(false);
  });

  it("is true once inside the leeway window, or when the expiry is unknown", () => {
    expect(isExpiring(token(1_000 + REFRESH_LEEWAY_SECONDS), 1_000)).toBe(true);
    expect(isExpiring(token(undefined), 1_000)).toBe(true);
  });
});

describe("refreshTokens", () => {
  const old: AppToken = {
    name: "Test User",
    accessToken: "old-access",
    refreshToken: "old-refresh",
    expiresAt: 1_000,
    roles: ["customer"],
  };

  it("calls the Keycloak token endpoint and stores the new tokens", async () => {
    const newAccess = jwtWith({ realm_access: { roles: ["customer", "staff"] } });
    const fetchMock = vi.fn(() =>
      tokenResponse({ access_token: newAccess, refresh_token: "new-refresh", expires_in: 300 }),
    );

    const result = await refreshTokens({ ...old, refreshToken: "r-success" }, config, {
      fetch: fetchMock as unknown as typeof fetch,
      now: () => 2_000,
    });

    expect(result).toMatchObject({
      name: "Test User",
      accessToken: newAccess,
      refreshToken: "new-refresh",
      expiresAt: 2_300,
      roles: ["customer", "staff"],
    });
    const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toBe("http://keycloak.test/realms/movie-booking/protocol/openid-connect/token");
    const body = init.body as URLSearchParams;
    expect(body.get("grant_type")).toBe("refresh_token");
    expect(body.get("refresh_token")).toBe("r-success");
    expect(body.get("client_id")).toBe("test-client");
  });

  it("keeps the old refresh token when Keycloak does not send a new one", async () => {
    const fetchMock = vi.fn(() => tokenResponse({ access_token: "a.b.c", expires_in: 300 }));
    const result = await refreshTokens({ ...old, refreshToken: "r-keep" }, config, {
      fetch: fetchMock as unknown as typeof fetch,
    });
    expect(result?.refreshToken).toBe("r-keep");
  });

  it.each([
    ["Keycloak rejects the refresh token", () => tokenResponse({ error: "invalid_grant" }, 400)],
    ["the response has no access token", () => tokenResponse({ expires_in: 300 })],
    ["the network fails", () => Promise.reject(new Error("connection refused"))],
  ])("returns null (ends the session) when %s, after a single attempt", async (_name, reply) => {
    vi.spyOn(console, "warn").mockImplementation(() => {});
    const fetchMock = vi.fn(reply);

    const result = await refreshTokens({ ...old, refreshToken: `r-${_name}` }, config, {
      fetch: fetchMock as unknown as typeof fetch,
    });

    expect(result).toBeNull();
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("returns null without calling Keycloak when there is no refresh token", async () => {
    const fetchMock = vi.fn();
    const result = await refreshTokens({ ...old, refreshToken: undefined }, config, {
      fetch: fetchMock as unknown as typeof fetch,
    });
    expect(result).toBeNull();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("shares one request between concurrent refreshes of the same refresh token", async () => {
    const fetchMock = vi.fn(() =>
      tokenResponse({ access_token: "a.b.c", refresh_token: "n", expires_in: 300 }),
    );
    const deps = { fetch: fetchMock as unknown as typeof fetch };
    const token = { ...old, refreshToken: "r-concurrent" };

    await Promise.all([refreshTokens(token, config, deps), refreshTokens(token, config, deps)]);

    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("never logs the secrets or tokens when a refresh fails", async () => {
    const warn = vi.spyOn(console, "warn").mockImplementation(() => {});
    await refreshTokens({ ...old, refreshToken: "r-logging" }, config, {
      fetch: (() => tokenResponse({ error: "invalid_grant" }, 400)) as unknown as typeof fetch,
    });
    const logged = JSON.stringify(warn.mock.calls);
    expect(logged).not.toContain(config.clientSecret);
    expect(logged).not.toContain("r-logging");
  });
});
