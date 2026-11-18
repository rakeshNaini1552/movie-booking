import "server-only";
import { getAccessToken } from "@/lib/auth/access-token";
import { env } from "@/lib/env";
import {
  GatewayError,
  type GatewayFailure,
  GatewayNotFoundError,
  NotSignedInError,
} from "./errors";

export type GatewayRequest = {
  // Short name for the log line, for example "catalog" or "seats".
  scope: string;
  path: string;
  method: "GET" | "POST" | "PUT" | "DELETE";
  body?: string;
  // Extra request headers, for example an idempotency key. Never put the token here.
  headers?: Record<string, string>;
  // REST endpoints answer 404 for "no such thing": throw GatewayNotFoundError instead of an outage error.
  notFoundIs404?: boolean;
  // Overrides GATEWAY_TIMEOUT_MS for calls that legitimately take longer (booking writes).
  timeoutMs?: number;
};

export type GatewayResponse = {
  response: Response;
  // Builds the error for a failure after the response arrived (bad body, GraphQL errors), and logs it.
  fail: (kind: GatewayFailure, status?: number, classifications?: string[]) => GatewayError;
};

// The part every gateway call shares: the signed-in user's token, a timeout, error mapping and
// failure logging. The token is read here, on the server, and only ever goes into the
// Authorization header of this request. Returns once the response is a success (2xx).
export async function gatewayFetch(request: GatewayRequest): Promise<GatewayResponse> {
  const token = await getAccessToken();
  if (!token) throw new NotSignedInError();

  const startedAt = performance.now();
  // One log line per failure: kind, HTTP status if there was a response, duration. Nothing else
  // goes in it, so no token, header, URL or response body can leak through the log.
  const fail = (kind: GatewayFailure, status?: number, classifications?: string[]) => {
    const error = new GatewayError(kind, { status, classifications });
    // A missing movie is an expected answer (getMovie turns it into a 404), not an outage.
    if (!error.isNotFound) {
      const durationMs = Math.round(performance.now() - startedAt);
      console.error(
        `[${request.scope}] request failed kind=${kind} status=${status ?? "none"} durationMs=${durationMs}`,
      );
    }
    return error;
  };

  let response: Response;
  try {
    response = await fetch(new URL(request.path, env.GATEWAY_URL), {
      method: request.method,
      headers: {
        ...request.headers,
        authorization: `Bearer ${token}`,
        accept: "application/json",
        ...(request.body === undefined ? {} : { "content-type": "application/json" }),
      },
      body: request.body,
      signal: AbortSignal.timeout(request.timeoutMs ?? env.GATEWAY_TIMEOUT_MS),
      // Per-user data: never share it through the fetch cache.
      cache: "no-store",
    });
  } catch (error) {
    const timedOut = error instanceof DOMException && error.name === "TimeoutError";
    // fetch reports a network failure as a TypeError. Anything else is not a gateway problem:
    // Next.js rejects fetch() on purpose when it stops a prerender (link prefetching does this),
    // and that must pass through untouched, without an error line in the log.
    if (!timedOut && !(error instanceof TypeError)) throw error;
    throw fail(timedOut ? "timeout" : "unreachable");
  }

  if (response.status === 401) {
    fail("http", 401);
    throw new NotSignedInError();
  }
  if (response.status === 404 && request.notFoundIs404) throw new GatewayNotFoundError();
  if (!response.ok) throw fail("http", response.status);
  return { response, fail };
}
