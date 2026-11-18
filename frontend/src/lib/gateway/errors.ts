// No "server-only" here: these are plain classes, and tests import them.

// The gateway rejected the token (401) or there is no session. Pages turn this into a redirect to /signin.
export class NotSignedInError extends Error {
  constructor() {
    super("Not signed in");
    this.name = "NotSignedInError";
  }
}

export type GatewayFailure = "timeout" | "unreachable" | "http" | "graphql" | "invalid-response";

// A REST endpoint answered 404. Callers decide what "not found" means (an empty page, a 404 page).
export class GatewayNotFoundError extends Error {
  constructor() {
    super("Gateway resource not found");
    this.name = "GatewayNotFoundError";
  }
}

// Messages are fixed strings: they can reach logs, so they never carry tokens, URLs or response bodies.
export class GatewayError extends Error {
  // Next keeps an existing string `digest` when it hides a server error from the browser, so this
  // is how error.tsx (a client component) learns the failure kind without seeing anything internal.
  readonly digest: string;

  constructor(
    readonly kind: GatewayFailure,
    // HTTP status for "http", GraphQL error classifications (for example NOT_FOUND) for "graphql".
    readonly detail?: { status?: number; classifications?: readonly string[] },
  ) {
    super(`Gateway request failed: ${kind}`);
    this.name = "GatewayError";
    this.digest = `GATEWAY_${kind.toUpperCase().replace("-", "_")}`;
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

// The HTTP status of a failed gateway call, or undefined when the failure was not an HTTP answer
// (timeout, unreachable, bad body). Callers use it to tell "seats taken" (409) from an outage.
export function httpStatusOf(error: unknown): number | undefined {
  return error instanceof GatewayError && error.kind === "http" ? error.detail?.status : undefined;
}

// What an error page tells the user. `subject` names what failed to load ("movies", "the seat map");
// anything that is not a gateway digest gets the generic text.
export function gatewayErrorMessage(digest: string | undefined, subject = "movies"): string {
  switch (digest) {
    case "GATEWAY_TIMEOUT":
      return "The service took too long to respond. Please try again in a moment.";
    case "GATEWAY_UNREACHABLE":
      return "We could not reach the service. Check your connection or try again shortly.";
    default:
      return `Something went wrong while loading ${subject}. Please try again in a moment.`;
  }
}

export const isGatewayDigest = (digest: string | undefined): boolean =>
  digest?.startsWith("GATEWAY_") ?? false;
