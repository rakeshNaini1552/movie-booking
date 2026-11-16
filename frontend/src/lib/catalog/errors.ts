// No "server-only" here: these are plain classes, and tests import them.

// The gateway rejected the token (401) or there is no session. Pages turn this into a redirect to /signin.
export class NotSignedInError extends Error {
  constructor() {
    super("Not signed in");
    this.name = "NotSignedInError";
  }
}

export type CatalogFailure = "timeout" | "unreachable" | "http" | "graphql" | "invalid-response";

// Messages are fixed strings: they can reach logs, so they never carry tokens, URLs or response bodies.
export class CatalogError extends Error {
  constructor(
    readonly kind: CatalogFailure,
    // HTTP status for "http", GraphQL error classifications (for example NOT_FOUND) for "graphql".
    readonly detail?: { status?: number; classifications?: readonly string[] },
  ) {
    super(`Catalog request failed: ${kind}`);
    this.name = "CatalogError";
  }

  get isNotFound(): boolean {
    const classifications = this.detail?.classifications ?? [];
    return (
      this.kind === "graphql" &&
      classifications.length > 0 &&
      classifications.every((c) => c === "NOT_FOUND")
    );
  }
}
