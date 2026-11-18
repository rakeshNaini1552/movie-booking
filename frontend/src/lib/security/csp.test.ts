import { describe, expect, it } from "vitest";
import { contentSecurityPolicy } from "./csp";

describe("contentSecurityPolicy", () => {
  const keycloakOrigin = "http://keycloak.test:8180";

  it("allows forms to end at Keycloak and nothing to frame the site", () => {
    const csp = contentSecurityPolicy({ keycloakOrigin, development: false });
    expect(csp).toContain("form-action 'self' http://keycloak.test:8180");
    expect(csp).toContain("frame-ancestors 'none'");
    expect(csp).toContain("default-src 'self'");
  });

  it("allows eval only in development", () => {
    expect(contentSecurityPolicy({ keycloakOrigin, development: false })).not.toContain(
      "unsafe-eval",
    );
    expect(contentSecurityPolicy({ keycloakOrigin, development: true })).toContain("'unsafe-eval'");
  });
});
