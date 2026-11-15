import { defineConfig } from "vitest/config";

export default defineConfig({
  // Vite resolves the "@/*" alias from tsconfig.json; JSX uses tsconfig's "react-jsx".
  resolve: { tsconfigPaths: true },
  test: {
    projects: [
      {
        // Pure logic (env validation): plain Node, no DOM start-up cost.
        extends: true,
        test: { name: "node", environment: "node", include: ["src/**/*.test.ts"] },
      },
      {
        // React components: jsdom plus jest-dom matchers and automatic cleanup.
        extends: true,
        test: {
          name: "dom",
          environment: "jsdom",
          include: ["src/**/*.test.tsx"],
          setupFiles: ["./vitest.setup.ts"],
        },
      },
    ],
  },
});
