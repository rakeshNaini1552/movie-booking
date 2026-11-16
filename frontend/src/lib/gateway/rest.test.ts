import { afterEach, beforeEach, describe, expect, it, vi, type MockInstance } from "vitest";
import { z } from "zod";

const { getAccessToken } = vi.hoisted(() => ({ getAccessToken: vi.fn() }));
vi.mock("server-only", () => ({}));
vi.mock("@/lib/auth/access-token", () => ({ getAccessToken }));
vi.mock("@/lib/env", () => ({
  env: { GATEWAY_URL: "http://gateway.test:8090", GATEWAY_TIMEOUT_MS: 1000 },
}));

import { GatewayError, GatewayNotFoundError, NotSignedInError } from "./errors";
import { gatewayGet } from "./rest";

const schema = z.array(z.object({ id: z.string() }));
const fetchMock = vi.fn();
const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });
const get = () => gatewayGet("seats", "/api/v1/things", schema);
async function failure(): Promise<GatewayError> {
  const error = await get().catch((e: unknown) => e);
  expect(error).toBeInstanceOf(GatewayError);
  return error as GatewayError;
}

describe("gatewayGet", () => {
  let consoleError: MockInstance<typeof console.error>;
  beforeEach(() => {
    getAccessToken.mockResolvedValue("test-token");
    vi.stubGlobal("fetch", fetchMock);
    consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
  });
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
    vi.clearAllMocks();
  });
  const logged = () => consoleError.mock.calls.map((c) => c.join(" "));

  it("sends a GET with the bearer token and returns the validated body", async () => {
    fetchMock.mockResolvedValue(json([{ id: "a" }]));
    await expect(get()).resolves.toEqual([{ id: "a" }]);

    const [url, init] = fetchMock.mock.calls[0] as [URL, RequestInit];
    expect(url.toString()).toBe("http://gateway.test:8090/api/v1/things");
    expect(init.method).toBe("GET");
    expect(init.body).toBeUndefined();
    expect((init.headers as Record<string, string>).authorization).toBe("Bearer test-token");
    expect(init.signal).toBeInstanceOf(AbortSignal);
  });

  it("throws NotSignedInError without calling the gateway when there is no token", async () => {
    getAccessToken.mockResolvedValue(null);
    await expect(get()).rejects.toBeInstanceOf(NotSignedInError);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("maps 401 to NotSignedInError", async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 401 }));
    await expect(get()).rejects.toBeInstanceOf(NotSignedInError);
  });

  it("maps 404 to GatewayNotFoundError and does not log it as an outage", async () => {
    fetchMock.mockResolvedValue(new Response("{}", { status: 404 }));
    await expect(get()).rejects.toBeInstanceOf(GatewayNotFoundError);
    expect(consoleError).not.toHaveBeenCalled();
  });

  it("maps other statuses to a typed error and logs one line without the body", async () => {
    fetchMock.mockResolvedValue(new Response("secret upstream body", { status: 502 }));
    const error = await failure();
    expect(error.kind).toBe("http");
    expect(error.detail?.status).toBe(502);
    expect(logged()).toHaveLength(1);
    expect(logged()[0]).toMatch(/^\[seats\] request failed kind=http status=502 durationMs=\d+$/);
    expect(logged()[0]).not.toMatch(/secret|test-token|gateway\.test/);
  });

  it("maps a timeout and a network failure", async () => {
    fetchMock.mockRejectedValueOnce(new DOMException("t", "TimeoutError"));
    expect((await failure()).kind).toBe("timeout");
    fetchMock.mockRejectedValueOnce(new TypeError("fetch failed"));
    expect((await failure()).kind).toBe("unreachable");
  });

  it("times out for real when the gateway never answers", async () => {
    fetchMock.mockImplementation(
      (_url: URL, init: RequestInit) =>
        new Promise((_resolve, reject) => {
          init.signal?.addEventListener("abort", () => reject(init.signal?.reason));
        }),
    );
    expect((await failure()).kind).toBe("timeout");
  });

  it("rejects a body of the wrong shape, and a body that is not JSON", async () => {
    fetchMock.mockResolvedValueOnce(json([{ id: 5 }]));
    expect((await failure()).kind).toBe("invalid-response");
    fetchMock.mockResolvedValueOnce(new Response("<html>", { status: 200 }));
    expect((await failure()).kind).toBe("invalid-response");
    expect(logged().every((l) => l.includes("kind=invalid-response status=200"))).toBe(true);
  });
});
