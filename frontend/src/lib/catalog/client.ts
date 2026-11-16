import "server-only";
import { z } from "zod";
import { gatewayFetch } from "@/lib/gateway/request";

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

// One GraphQL call to the gateway with the signed-in user's token (see gatewayFetch for the
// token, timeout, error mapping and logging this shares with the REST calls).
export async function gatewayQuery<S extends z.ZodType>(
  query: string,
  variables: Record<string, unknown>,
  dataSchema: S,
): Promise<z.output<S>> {
  const { response, fail } = await gatewayFetch({
    scope: "catalog",
    path: "/graphql",
    method: "POST",
    body: JSON.stringify({ query, variables }),
  });

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
