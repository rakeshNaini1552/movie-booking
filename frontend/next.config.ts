import type { NextConfig } from "next";
import { parseEnv } from "./src/lib/env-schema";

// next.config.ts is loaded by `next dev`, `next build` and `next start` before anything
// else, after .env files are loaded into process.env, so a bad value stops all three here.
parseEnv(process.env);

const nextConfig: NextConfig = {
  /* config options here */
  cacheComponents: true,
  partialPrefetching: true,
};

export default nextConfig;
