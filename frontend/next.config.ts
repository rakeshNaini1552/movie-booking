import type { NextConfig } from "next";
import { parseEnv } from "./src/lib/env-schema";

// next.config.ts is loaded by `next dev`, `next build` and `next start` before anything
// else, after .env files are loaded into process.env, so a bad value stops all three here.
// `next typegen` (npm run typecheck) loads this file too, so it needs a valid env as well: .env.local
// locally, placeholder values in CI. There is deliberately no way to skip the validation.
parseEnv(process.env);

// Sent with every response. The Content-Security-Policy is set in src/proxy.ts instead, because
// it contains the Keycloak address, which is only known when the server starts.
const securityHeaders = [
  // Do not guess a file's type from its content.
  { key: "X-Content-Type-Options", value: "nosniff" },
  // Never allow this site inside a frame on another site (clickjacking).
  { key: "X-Frame-Options", value: "DENY" },
  // Other sites see only our origin as the referrer, never the full path with ids in it.
  { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
  // The app uses none of these browser features, so no script may ask for them.
  { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
];

const nextConfig: NextConfig = {
  cacheComponents: true,
  partialPrefetching: true,
  // Builds a self-contained server in .next/standalone with only the files it needs, which is
  // what the Docker image copies (see Dockerfile).
  output: "standalone",
  async headers() {
    return [{ source: "/:path*", headers: securityHeaders }];
  },
};

export default nextConfig;
