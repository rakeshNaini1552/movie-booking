import type { Account, Session } from "next-auth";
import type { JWT } from "next-auth/jwt";
import { describe, expect, it, vi } from "vitest";
import { jwtCallback, sessionCallback } from "./callbacks";
import type { AppToken, KeycloakConfig } from "./tokens";

const config: KeycloakConfig = {
  issuer: "http://keycloak.test/realms/movie-booking",
  clientId: "test-client",
  clientSecret: "test-only-client-secret",
};

const jwtWith = (payload: object) =>
  `header.${Buffer.from(JSON.stringify(payload)).toString("base64url")}.signature`;

describe("sessionCallback", () => {
  const token: AppToken = {
    name: "Test User",
    email: "test@example.com",
    picture: "http://example.com/p.png",
    accessToken: "SECRET-ACCESS-TOKEN",
    refreshToken: "SECRET-REFRESH-TOKEN",
    expiresAt: 123,
    roles: ["customer"],
    idToken: "SECRET-ID-TOKEN",
  };
  const session = { expires: "2099-01-01T00:00:00.000Z", user: {} } as Session;

  it("exposes only name, email and roles", () => {
    expect(sessionCallback({ session, token })).toEqual({
      expires: "2099-01-01T00:00:00.000Z",
      user: { name: "Test User", email: "test@example.com", roles: ["customer"] },
    });
  });

  it("never exposes any token, whatever the JWT holds", () => {
    const json = JSON.stringify(sessionCallback({ session, token }));
    for (const forbidden of ["SECRET-", "accessToken", "refreshToken", "idToken", "expiresAt"]) {
      expect(json).not.toContain(forbidden);
    }
  });

  it("defaults to no roles", () => {
    const { user } = sessionCallback({ session, token: { name: "A" } as JWT });
    expect(user.roles).toEqual([]);
  });
});

describe("jwtCallback", () => {
  const account = {
    provider: "keycloak",
    type: "oidc",
    providerAccountId: "user-1",
    access_token: jwtWith({ realm_access: { roles: ["customer"] } }),
    refresh_token: "refresh-1",
    expires_at: 5_000,
  } as Account;

  it("stores the tokens, expiry and roles at sign-in", async () => {
    const result = (await jwtCallback({ token: { name: "A" }, account }, config)) as AppToken;
    expect(result).toMatchObject({
      name: "A",
      accessToken: account.access_token,
      refreshToken: "refresh-1",
      expiresAt: 5_000,
      roles: ["customer"],
    });
  });

  it("ends the session when sign-in did not return the tokens", async () => {
    const result = await jwtCallback(
      { token: {}, account: { ...account, refresh_token: undefined } },
      config,
    );
    expect(result).toBeNull();
  });

  it("does not call Keycloak while the access token is still valid", async () => {
    const fetchMock = vi.fn();
    const token: AppToken = { accessToken: "a", refreshToken: "r", expiresAt: 10_000 };

    const result = await jwtCallback({ token }, config, {
      fetch: fetchMock as unknown as typeof fetch,
      now: () => 1_000,
    });

    expect(result).toBe(token);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("refreshes an access token that is about to expire", async () => {
    const fetchMock = vi.fn(() =>
      Promise.resolve(
        Response.json({ access_token: "new.a.t", refresh_token: "refresh-2", expires_in: 300 }),
      ),
    );
    const token: AppToken = { accessToken: "a", refreshToken: "refresh-cb-1", expiresAt: 1_020 };

    const result = (await jwtCallback({ token }, config, {
      fetch: fetchMock as unknown as typeof fetch,
      now: () => 1_000,
    })) as AppToken;

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(result).toMatchObject({
      accessToken: "new.a.t",
      refreshToken: "refresh-2",
      expiresAt: 1_300,
    });
  });

  it("ends the session when the refresh fails", async () => {
    vi.spyOn(console, "warn").mockImplementation(() => {});
    const fetchMock = vi.fn(() =>
      Promise.resolve(Response.json({ error: "invalid_grant" }, { status: 400 })),
    );
    const token: AppToken = { accessToken: "a", refreshToken: "refresh-cb-2", expiresAt: 900 };

    const result = await jwtCallback({ token }, config, {
      fetch: fetchMock as unknown as typeof fetch,
      now: () => 1_000,
    });

    expect(result).toBeNull();
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});
