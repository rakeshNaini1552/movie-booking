import { z } from "zod";

// Server-only configuration. Plain settings have defaults. Secrets (AUTH_SECRET,
// AUTH_KEYCLOAK_SECRET) must NOT get a default: a missing secret has to fail validation,
// not silently fall back.
// Never prefix these with NEXT_PUBLIC_, that would inline them into the browser bundle.
const schema = z.object({
  GATEWAY_URL: z.url().default("http://localhost:8090"),
  // Upper bound for one gateway call, so a hung gateway shows the error page instead of hanging the page.
  GATEWAY_TIMEOUT_MS: z.coerce.number().int().min(100).max(60000).default(5000),
  KEYCLOAK_ISSUER_URI: z.url().default("http://localhost:8180/realms/movie-booking"),
  PORT: z.coerce.number().int().min(1).max(65535).default(3001),
  // Auth.js: encrypts the session cookie, so it needs real entropy (openssl rand -base64 32).
  AUTH_SECRET: z.string().min(32),
  AUTH_KEYCLOAK_ID: z.string().min(1).default("movie-booking-web"),
  AUTH_KEYCLOAK_SECRET: z.string().min(1),
  // Public origin of this app: callback and post-logout URLs are built from it.
  AUTH_URL: z.url().default("http://localhost:3001"),
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
