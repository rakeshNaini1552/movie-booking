import "server-only";
import { z } from "zod";
import { getAccessToken } from "@/lib/auth/access-token";
import { env } from "@/lib/env";
import { CatalogError, NotSignedInError } from "./errors";

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
    throw new CatalogError(
      error instanceof DOMException && error.name === "TimeoutError" ? "timeout" : "unreachable",
    );
  }

  if (response.status === 401) throw new NotSignedInError();
  if (!response.ok) throw new CatalogError("http", { status: response.status });

  const envelope = envelopeSchema.safeParse(await response.json().catch(() => undefined));
  if (!envelope.success) throw new CatalogError("invalid-response");

  // GraphQL reports failures inside a 200 response.
  const { data, errors } = envelope.data;
  if (errors && errors.length > 0) {
    throw new CatalogError("graphql", {
      classifications: errors.map((e) => e.extensions?.classification ?? "UNKNOWN"),
    });
  }

  const parsed = dataSchema.safeParse(data);
  if (!parsed.success) throw new CatalogError("invalid-response");
  return parsed.data;
}
