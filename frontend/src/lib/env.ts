import "server-only";
import { parseEnv } from "./env-schema";

// Importing this from a client component fails the build via "server-only".
export const env = parseEnv(process.env);
