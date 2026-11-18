import "server-only";
import type { z } from "zod";
import { gatewayFetch } from "./request";

// GET a JSON resource from the gateway and validate it. Throws NotSignedInError (401),
// GatewayNotFoundError (404) or GatewayError (anything else, including a body of the wrong shape).
export async function gatewayGet<S extends z.ZodType>(
  scope: string,
  path: string,
  schema: S,
): Promise<z.output<S>> {
  const { response, fail } = await gatewayFetch({
    scope,
    path,
    method: "GET",
    notFoundIs404: true,
  });
  const body: unknown = await response.json().catch(() => undefined);
  const parsed = schema.safeParse(body);
  if (!parsed.success) throw fail("invalid-response", response.status);
  return parsed.data;
}

export type GatewayWrite = {
  method: "POST" | "PUT" | "DELETE";
  // Sent as JSON. Leave out for calls with no body (cancel, delete).
  body?: unknown;
  headers?: Record<string, string>;
  // Overrides GATEWAY_TIMEOUT_MS for this call.
  timeoutMs?: number;
};

// A write (POST, PUT, DELETE) that answers with JSON, validated like gatewayGet. A failed call
// throws GatewayError; httpStatusOf(error) gives the status so the caller can explain a 409 or 404.
export async function gatewaySend<S extends z.ZodType>(
  scope: string,
  path: string,
  write: GatewayWrite,
  schema: S,
): Promise<z.output<S>> {
  const { response, fail } = await gatewayFetch({
    scope,
    path,
    method: write.method,
    body: write.body === undefined ? undefined : JSON.stringify(write.body),
    headers: write.headers,
    timeoutMs: write.timeoutMs,
  });
  const body: unknown = await response.json().catch(() => undefined);
  const parsed = schema.safeParse(body);
  if (!parsed.success) throw fail("invalid-response", response.status);
  return parsed.data;
}

// A write whose answer has no body we need (204 No Content, for example a delete).
export async function gatewaySendNoContent(
  scope: string,
  path: string,
  write: GatewayWrite,
): Promise<void> {
  await gatewayFetch({
    scope,
    path,
    method: write.method,
    body: write.body === undefined ? undefined : JSON.stringify(write.body),
    headers: write.headers,
  });
}
