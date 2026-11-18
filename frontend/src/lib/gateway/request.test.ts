import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const { getAccessToken } = vi.hoisted(() => ({ getAccessToken: vi.fn() }));
vi.mock("server-only", () => ({}));
vi.mock("@/lib/auth/access-token", () => ({ getAccessToken }));
vi.mock("@/lib/env", () => ({
  env: { GATEWAY_URL: "http://gateway.test:8090", GATEWAY_TIMEOUT_MS: 1000 },
}));

import { gatewayFetch } from "./request";

const fetchMock = vi.fn();

describe("gatewayFetch timeout", () => {
  let timeoutSpy: ReturnType<typeof vi.spyOn>;
  beforeEach(() => {
    getAccessToken.mockResolvedValue("test-token");
    fetchMock.mockResolvedValue(new Response("{}", { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    timeoutSpy = vi.spyOn(AbortSignal, "timeout");
  });
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
    vi.clearAllMocks();
  });

  it("uses GATEWAY_TIMEOUT_MS when the request has no timeout of its own", async () => {
    await gatewayFetch({ scope: "t", path: "/x", method: "GET" });
    expect(timeoutSpy).toHaveBeenCalledWith(1000);
  });

  it("uses the request's timeoutMs instead when it is given", async () => {
    await gatewayFetch({ scope: "t", path: "/x", method: "POST", body: "{}", timeoutMs: 15000 });
    expect(timeoutSpy).toHaveBeenCalledWith(15000);
    expect(timeoutSpy).not.toHaveBeenCalledWith(1000);
  });
});
