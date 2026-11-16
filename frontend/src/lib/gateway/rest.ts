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
