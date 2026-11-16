import type { NextConfig } from "next";
import { parseEnv } from "./src/lib/env-schema";

// next.config.ts is loaded by `next dev`, `next build` and `next start` before anything
// else, after .env files are loaded into process.env, so a bad value stops all three here.
// `next typegen` (npm run typecheck) loads this file too, so it needs a valid env as well: .env.local
// locally, placeholder values in CI. There is deliberately no way to skip the validation.
parseEnv(process.env);

const nextConfig: NextConfig = {
  /* config options here */
  cacheComponents: true,
  partialPrefetching: true,
};

export default nextConfig;
