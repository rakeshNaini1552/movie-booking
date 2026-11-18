// For the Kubernetes probes. It answers as soon as the server can handle a request and does not
// call the gateway: if it did, a gateway outage would make Kubernetes restart healthy frontend pods.
export function GET() {
  return Response.json({ status: "UP" });
}
