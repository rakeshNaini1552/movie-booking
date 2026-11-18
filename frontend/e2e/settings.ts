// Fixed values for the end-to-end run. None of these are real credentials: the app under test
// is started with them by playwright.config.ts and talks only to the mock gateway.
export const E2E_PORT = 3100;
export const E2E_BASE_URL = `http://localhost:${E2E_PORT}`;
export const MOCK_GATEWAY_PORT = 8099;
export const MOCK_GATEWAY_URL = `http://127.0.0.1:${MOCK_GATEWAY_PORT}`;
export const E2E_AUTH_SECRET = "e2e-only-secret-not-used-anywhere-else-0123456789";
