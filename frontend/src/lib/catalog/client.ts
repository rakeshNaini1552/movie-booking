import "server-only";
import { z } from "zod";
import { getAccessToken } from "@/lib/auth/access-token";
import { env } from "@/lib/env";
import { CatalogError, type CatalogFailure, NotSignedInError } from "./errors";

const envelopeSchema = z.object({
  data: z.unknown().nullish(),
  errors: z
    .array(
      z.object({
        message: z.string(),
        extensions: z.object({ classification: z.string().optional() }).loose().optional(),
      }),
    )
    .optional(),
});

// One GraphQL call to the gateway with the signed-in user's token. The token is read here, on the
// server, and only ever goes into the Authorization header of this request.
export async function gatewayQuery<S extends z.ZodType>(
  query: string,
  variables: Record<string, unknown>,
  dataSchema: S,
): Promise<z.output<S>> {
  const token = await getAccessToken();
  if (!token) throw new NotSignedInError();

  const startedAt = performance.now();
  // One log line per failure: kind, HTTP status if there was a response, duration. Nothing else
  // goes in it, so no token, header, URL or response body can leak through the log.
  const fail = (kind: CatalogFailure, status?: number, classifications?: string[]) => {
    const error = new CatalogError(kind, { status, classifications });
    // A missing movie is an expected answer (getMovie turns it into a 404), not an outage.
    if (!error.isNotFound) {
      const durationMs = Math.round(performance.now() - startedAt);
      console.error(
        `[catalog] request failed kind=${kind} status=${status ?? "none"} durationMs=${durationMs}`,
      );
    }
    return error;
  };

  let response: Response;
  try {
    response = await fetch(new URL("/graphql", env.GATEWAY_URL), {
      method: "POST",
      headers: {
        authorization: `Bearer ${token}`,
        "content-type": "application/json",
        accept: "application/json",
      },
      body: JSON.stringify({ query, variables }),
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
  if (!response.ok) throw fail("http", response.status);

  const envelope = envelopeSchema.safeParse(await response.json().catch(() => undefined));
  if (!envelope.success) throw fail("invalid-response", response.status);

  // GraphQL reports failures inside a 200 response.
  const { data, errors } = envelope.data;
  if (errors && errors.length > 0) {
    throw fail(
      "graphql",
      response.status,
      errors.map((e) => e.extensions?.classification ?? "UNKNOWN"),
    );
  }

  const parsed = dataSchema.safeParse(data);
  if (!parsed.success) throw fail("invalid-response", response.status);
  return parsed.data;
}
