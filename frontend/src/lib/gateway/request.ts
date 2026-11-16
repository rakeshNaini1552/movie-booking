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
  method: "GET" | "POST";
  body?: string;
  // REST endpoints answer 404 for "no such thing": throw GatewayNotFoundError instead of an outage error.
  notFoundIs404?: boolean;
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
        authorization: `Bearer ${token}`,
        accept: "application/json",
        ...(request.body === undefined ? {} : { "content-type": "application/json" }),
      },
      body: request.body,
      signal: AbortSignal.timeout(env.GATEWAY_TIMEOUT_MS),
      // Per-user data: never share it through the fetch cache.
      cache: "no-store",
    });
  } catch (error) {
    throw fail(
      error instanceof DOMException && error.name === "TimeoutError" ? "timeout" : "unreachable",
    );
  }

  if (response.status === 401) {
    fail("http", 401);
    throw new NotSignedInError();
  }
  if (response.status === 404 && request.notFoundIs404) throw new GatewayNotFoundError();
  if (!response.ok) throw fail("http", response.status);
  return { response, fail };
}
