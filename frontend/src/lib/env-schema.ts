import { z } from "zod";

// Server-only configuration. None of these are secrets, so they have defaults.
// Future secrets (Keycloak client secret, auth secret) must NOT get a default:
// a missing secret has to fail validation, not silently fall back.
// Never prefix these with NEXT_PUBLIC_, that would inline them into the browser bundle.
const schema = z.object({
  GATEWAY_URL: z.url().default("http://localhost:8090"),
  KEYCLOAK_ISSUER_URI: z.url().default("http://localhost:8180/realms/movie-booking"),
  PORT: z.coerce.number().int().min(1).max(65535).default(3001),
});

export type Env = Readonly<z.infer<typeof schema>>;

// Kept free of "server-only" so next.config.ts (plain Node) and unit tests can import it.
// App code must import `env` from "@/lib/env" instead.
export function parseEnv(source: Record<string, string | undefined>): Env {
  const result = schema.safeParse(source);
  if (!result.success) {
    // Names only: the offending values may be mistyped secrets or credentials in a URL.
    const names = [...new Set(result.error.issues.map((i) => i.path.join(".")))];
    throw new Error(`Invalid environment configuration. Fix these variables: ${names.join(", ")}`);
  }
  return Object.freeze(result.data);
}
