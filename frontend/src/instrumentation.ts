import { registerOTel } from "@vercel/otel";

// Next.js calls register() once when the server starts. With tracing on, every page render and
// every fetch to the gateway becomes a span, and the trace id travels to the gateway in the
// `traceparent` header, so one trace in Tempo shows browser request -> frontend -> gateway -> service.
// The exporter reads OTEL_EXPORTER_OTLP_ENDPOINT itself; without it tracing stays off.
export function register() {
  if (!process.env.OTEL_EXPORTER_OTLP_ENDPOINT) return;
  registerOTel({ serviceName: process.env.OTEL_SERVICE_NAME ?? "frontend" });
}
