import AutoRefresh from "@/components/AutoRefresh";
import QrCode from "@/components/QrCode";
import StatusBadge from "@/components/StatusBadge";
import { formatDateTime } from "@/lib/format";
import type { Ticket, TicketStatus } from "@/lib/tickets/schemas";

const TICKET_POLL_INTERVAL_MS = 2000;
const TICKET_POLL_ATTEMPTS = 8;

const ticketStatus: Record<TicketStatus, { label: string; tone: "green" | "zinc" | "red" }> = {
  ISSUED: { label: "Valid", tone: "green" },
  USED: { label: "Used", tone: "zinc" },
  VOID: { label: "Void", tone: "red" },
};

// The ticket of a confirmed booking: its QR code, or a waiting note until it has been issued.
export default function TicketPanel({ ticket }: { ticket: Ticket | null }) {
  if (!ticket) {
    return (
      <section
        aria-labelledby="ticket-heading"
        className="border-line bg-panel rounded-3xl border p-6"
      >
        <h2 id="ticket-heading" className="font-display text-xl font-bold">
          Your ticket
        </h2>
        <p role="status" className="mt-3 text-zinc-300">
          Your ticket is being issued. This page updates by itself in a few seconds.
        </p>
        <AutoRefresh intervalMs={TICKET_POLL_INTERVAL_MS} attempts={TICKET_POLL_ATTEMPTS} />
      </section>
    );
  }

  const status = ticketStatus[ticket.status];
  return (
    <section
      aria-labelledby="ticket-heading"
      className="border-line bg-panel flex flex-col items-center gap-4 rounded-3xl border p-6 text-center"
    >
      <div className="flex w-full items-center justify-between gap-3">
        <h2 id="ticket-heading" className="font-display text-xl font-bold">
          Your ticket
        </h2>
        <StatusBadge label={status.label} tone={status.tone} />
      </div>
      <div className={ticket.status === "ISSUED" ? "" : "opacity-40"}>
        <QrCode payload={ticket.qrPayload} label="Ticket QR code" />
      </div>
      <p className="text-sm text-zinc-400">
        {ticket.status === "ISSUED" && "Show this code at the entrance."}
        {ticket.status === "USED" &&
          `Scanned ${ticket.usedAt ? formatDateTime(ticket.usedAt) : "at the entrance"}.`}
        {ticket.status === "VOID" && "This ticket is no longer valid."}
      </p>
      <p className="font-mono text-xs break-all text-zinc-400">Ticket {ticket.ticketId}</p>
    </section>
  );
}
