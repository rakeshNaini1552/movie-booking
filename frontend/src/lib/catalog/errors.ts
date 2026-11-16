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
  // Next keeps an existing string `digest` when it hides a server error from the browser, so this
  // is how error.tsx (a client component) learns the failure kind without seeing anything internal.
  readonly digest: string;

  constructor(
    readonly kind: CatalogFailure,
    // HTTP status for "http", GraphQL error classifications (for example NOT_FOUND) for "graphql".
    readonly detail?: { status?: number; classifications?: readonly string[] },
  ) {
    super(`Catalog request failed: ${kind}`);
    this.name = "CatalogError";
    this.digest = `CATALOG_${kind.toUpperCase().replace("-", "_")}`;
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

// What the error page tells the user. Anything that is not a catalog digest gets the generic text.
export function catalogErrorMessage(digest: string | undefined): string {
  switch (digest) {
    case "CATALOG_TIMEOUT":
      return "The catalog took too long to respond. Please try again in a moment.";
    case "CATALOG_UNREACHABLE":
      return "We could not reach the catalog. Check your connection or try again shortly.";
    default:
      return "Something went wrong while loading movies. Please try again in a moment.";
  }
}

export const isCatalogDigest = (digest: string | undefined): boolean =>
  digest?.startsWith("CATALOG_") ?? false;
