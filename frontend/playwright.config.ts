import { defineConfig, devices } from "@playwright/test";
import {
  E2E_AUTH_SECRET,
  E2E_BASE_URL,
  E2E_PORT,
  MOCK_GATEWAY_PORT,
  MOCK_GATEWAY_URL,
} from "./e2e/settings";

// End-to-end tests: a real browser against the real Next.js server (a production build), with
// the gateway replaced by e2e/mock-gateway.mjs and the session cookie created by e2e/session.ts.
// So they test every page and server action of the frontend, and none of the backend.
export default defineConfig({
  testDir: "./e2e",
  testMatch: "**/*.spec.ts",
  // The mock gateway keeps one shared data set, so tests run one at a time.
  workers: 1,
  fullyParallel: false,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [["list"], ["html", { open: "never" }]] : "list",
  use: {
    baseURL: E2E_BASE_URL,
    trace: "retain-on-failure",
    // Lets a machine that already has a Chromium point at it instead of downloading one.
    launchOptions: { executablePath: process.env.PLAYWRIGHT_CHROMIUM_PATH || undefined },
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
  webServer: [
    {
      command: "node e2e/mock-gateway.mjs",
      url: `${MOCK_GATEWAY_URL}/__health`,
      env: { MOCK_GATEWAY_PORT: String(MOCK_GATEWAY_PORT) },
      reuseExistingServer: !process.env.CI,
    },
    {
      // The standalone server is what the Docker image runs, so the tests use the same thing.
      command:
        "npm run build && cp -r .next/static .next/standalone/.next/static && node .next/standalone/server.js",
      url: `${E2E_BASE_URL}/api/health`,
      timeout: 240_000,
      reuseExistingServer: !process.env.CI,
      env: {
        PORT: String(E2E_PORT),
        HOSTNAME: "127.0.0.1",
        AUTH_URL: E2E_BASE_URL,
        AUTH_TRUST_HOST: "true",
        GATEWAY_URL: MOCK_GATEWAY_URL,
        // Test-only placeholders (see e2e/settings.ts). Nothing real is signed in with them.
        AUTH_SECRET: E2E_AUTH_SECRET,
        AUTH_KEYCLOAK_SECRET: "e2e-placeholder-not-a-real-client-secret",
      },
    },
  ],
});
