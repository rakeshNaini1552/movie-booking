import { describe, expect, it } from "vitest";
import { buildLogoutUrl, safeCallbackPath } from "./urls";

describe("safeCallbackPath", () => {
  it.each(["/account", "/movies?page=2", "/account/settings#top", "/"])(
    "keeps the same-site path %s",
    (path) => expect(safeCallbackPath(path)).toBe(path),
  );

  it.each([
    "https://evil.example/steal",
    "http://localhost:3001/account",
    "//evil.example",
    "/\\evil.example",
    "/\t/evil.example",
    "javascript:alert(1)",
    "account",
    "",
    undefined,
    null,
    42,
  ])("falls back to / for %j", (value) => expect(safeCallbackPath(value)).toBe("/"));
});

describe("buildLogoutUrl", () => {
  it("targets the realm logout endpoint with client_id and a post-logout redirect, no tokens", () => {
    const url = new URL(
      buildLogoutUrl("http://kc.test/realms/r/", "web", "http://localhost:3001/"),
    );
    expect(url.origin + url.pathname).toBe(
      "http://kc.test/realms/r/protocol/openid-connect/logout",
    );
    expect(url.searchParams.get("client_id")).toBe("web");
    expect(url.searchParams.get("post_logout_redirect_uri")).toBe("http://localhost:3001/");
    expect(url.searchParams.has("id_token_hint")).toBe(false);
  });
});
