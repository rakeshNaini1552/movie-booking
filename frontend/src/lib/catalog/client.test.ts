import { afterEach, beforeEach, describe, expect, it, vi, type MockInstance } from "vitest";
import { z } from "zod";

const { getAccessToken } = vi.hoisted(() => ({ getAccessToken: vi.fn() }));
vi.mock("server-only", () => ({}));
vi.mock("@/lib/auth/access-token", () => ({ getAccessToken }));
vi.mock("@/lib/env", () => ({
  env: { GATEWAY_URL: "http://gateway.test:8090", GATEWAY_TIMEOUT_MS: 1000 },
}));

import { gatewayQuery } from "./client";
import { CatalogError, NotSignedInError } from "./errors";

const schema = z.object({ hello: z.string() });
const fetchMock = vi.fn();

const jsonResponse = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });

// Resolves to the CatalogError a call rejects with, so tests can assert on its fields.
async function failure(): Promise<CatalogError> {
  const error = await gatewayQuery("query { hello }", {}, schema).catch((e: unknown) => e);
  expect(error).toBeInstanceOf(CatalogError);
  return error as CatalogError;
}

describe("gatewayQuery", () => {
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

  it("posts the query to the gateway with the bearer token and returns the parsed data", async () => {
    fetchMock.mockResolvedValue(jsonResponse({ data: { hello: "world" } }));

    await expect(gatewayQuery("query { hello }", { a: 1 }, schema)).resolves.toEqual({
      hello: "world",
    });

    const [url, init] = fetchMock.mock.calls[0] as [URL, RequestInit];
    expect(url.toString()).toBe("http://gateway.test:8090/graphql");
    expect(init.method).toBe("POST");
    expect((init.headers as Record<string, string>).authorization).toBe("Bearer test-token");
    expect(JSON.parse(init.body as string)).toEqual({
      query: "query { hello }",
      variables: { a: 1 },
    });
    expect(init.signal).toBeInstanceOf(AbortSignal);
  });

  it("throws NotSignedInError without calling the gateway when there is no token", async () => {
    getAccessToken.mockResolvedValue(null);
    await expect(gatewayQuery("q", {}, schema)).rejects.toBeInstanceOf(NotSignedInError);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("maps a 401 from the gateway to NotSignedInError", async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 401 }));
    await expect(gatewayQuery("q", {}, schema)).rejects.toBeInstanceOf(NotSignedInError);
  });

  it("maps other HTTP failures to a CatalogError carrying the status", async () => {
    fetchMock.mockResolvedValue(new Response("upstream details", { status: 503 }));
    const error = await failure();
    expect(error.kind).toBe("http");
    expect(error.detail?.status).toBe(503);
    expect(error.message).not.toContain("upstream details");
  });

  it("maps GraphQL errors in a 200 response, keeping the classifications", async () => {
    fetchMock.mockResolvedValue(
      jsonResponse({
        data: null,
        errors: [{ message: "Movie 1 not found", extensions: { classification: "NOT_FOUND" } }],
      }),
    );
    const error = await failure();
    expect(error.kind).toBe("graphql");
    expect(error.isNotFound).toBe(true);
    expect(error.message).not.toContain("Movie 1");
  });

  it("does not treat a mix of GraphQL error types as not found", async () => {
    fetchMock.mockResolvedValue(
      jsonResponse({
        errors: [
          { message: "a", extensions: { classification: "NOT_FOUND" } },
          { message: "b", extensions: { classification: "INTERNAL_ERROR" } },
        ],
      }),
    );
    expect((await failure()).isNotFound).toBe(false);
  });

  it("maps an aborted request to a timeout", async () => {
    fetchMock.mockRejectedValue(new DOMException("The operation timed out", "TimeoutError"));
    expect((await failure()).kind).toBe("timeout");
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

  it("maps a network failure to unreachable", async () => {
    fetchMock.mockRejectedValue(new TypeError("fetch failed"));
    expect((await failure()).kind).toBe("unreachable");
  });

  it("rejects a response whose data does not match the schema", async () => {
    fetchMock.mockResolvedValue(jsonResponse({ data: { hello: 42 } }));
    expect((await failure()).kind).toBe("invalid-response");
  });

  it("rejects a body that is not JSON", async () => {
    fetchMock.mockResolvedValue(new Response("<html>oops</html>", { status: 200 }));
    expect((await failure()).kind).toBe("invalid-response");
  });

  describe("failure logging", () => {
    it("logs one line with kind, status and duration for an HTTP failure", async () => {
      fetchMock.mockResolvedValue(new Response("secret upstream body", { status: 503 }));
      await failure();
      expect(logged()).toHaveLength(1);
      expect(logged()[0]).toMatch(
        /^\[catalog\] request failed kind=http status=503 durationMs=\d+$/,
      );
    });

    it.each([
      ["timeout", new DOMException("t", "TimeoutError"), "kind=timeout status=none"],
      ["unreachable", new TypeError("fetch failed"), "kind=unreachable status=none"],
    ])("logs a %s", async (_name, rejection, expected) => {
      fetchMock.mockRejectedValue(rejection);
      await failure();
      expect(logged()).toHaveLength(1);
      expect(logged()[0]).toContain(expected);
    });

    it("logs graphql and invalid-response failures with the response status", async () => {
      fetchMock.mockResolvedValueOnce(
        jsonResponse({
          errors: [{ message: "x", extensions: { classification: "INTERNAL_ERROR" } }],
        }),
      );
      await failure();
      fetchMock.mockResolvedValueOnce(jsonResponse({ data: { hello: 42 } }));
      await failure();
      expect(logged()[0]).toContain("kind=graphql status=200");
      expect(logged()[1]).toContain("kind=invalid-response status=200");
    });

    it("logs a gateway 401 as an http failure", async () => {
      fetchMock.mockResolvedValue(new Response(null, { status: 401 }));
      await gatewayQuery("q", {}, schema).catch(() => {});
      expect(logged()[0]).toContain("kind=http status=401");
    });

    it("does not log an expected NOT_FOUND, a missing token, or a success", async () => {
      fetchMock.mockResolvedValueOnce(
        jsonResponse({ errors: [{ message: "x", extensions: { classification: "NOT_FOUND" } }] }),
      );
      await failure();
      fetchMock.mockResolvedValueOnce(jsonResponse({ data: { hello: "w" } }));
      await gatewayQuery("q", {}, schema);
      getAccessToken.mockResolvedValueOnce(null);
      await gatewayQuery("q", {}, schema).catch(() => {});
      expect(consoleError).not.toHaveBeenCalled();
    });

    it("never logs the token, the URL or the response body", async () => {
      fetchMock.mockResolvedValue(new Response("secret upstream body", { status: 500 }));
      await failure();
      fetchMock.mockRejectedValue(new TypeError("connect ECONNREFUSED gateway.test:8090"));
      await failure();
      const output = logged().join("\n");
      expect(output).not.toMatch(/test-token|secret upstream|gateway\.test|ECONNREFUSED|Bearer/);
    });
  });
});
