"use client";

import { useRouter } from "next/navigation";
import { useRef, useState, useTransition } from "react";
import Button from "@/components/Button";
import { groupSeatsByRow } from "@/lib/seats/group-seats";
import { MAX_SEATS } from "@/lib/seats/limits";
import type { Seat, SeatStatus, SeatType } from "@/lib/seats/schemas";

type SeatPickerProps = {
  seats: readonly Seat[];
  // Creates the booking for the chosen seats. Passed in by the server page (a server action), so
  // this component never imports server code. It resolves only when booking did not work;
  // on success the action redirects to the booking page.
  onBook: (
    seatIds: readonly string[],
    idempotencyKey: string,
  ) => Promise<{ error?: string } | void>;
};

const typeLabel: Record<SeatType, string> = {
  SILVER: "Silver",
  GOLD: "Gold",
  PLATINUM: "Platinum",
};

// The tier is the colour of an available seat's outline. It is also in every seat's label and at
// the end of each row, so it never depends on colour alone.
const typeOutline: Record<SeatType, string> = {
  SILVER: "border-zinc-500 text-zinc-200 hover:bg-zinc-500/25",
  GOLD: "border-amber-400 text-amber-100 hover:bg-amber-400/25",
  PLATINUM: "border-violet-400 text-violet-100 hover:bg-violet-400/25",
};
const typeDot: Record<SeatType, string> = {
  SILVER: "bg-zinc-400",
  GOLD: "bg-amber-400",
  PLATINUM: "bg-violet-400",
};

// Every state has a text label and a symbol as well as a colour, so none relies on colour alone.
const stateStyle = {
  available: { label: "Available", symbol: "", color: "border-zinc-500" },
  selected: {
    label: "Selected",
    symbol: "✓",
    color: "border-brand bg-brand text-white shadow-lg shadow-brand/40",
  },
  held: { label: "Held", symbol: "…", color: "border-amber-700 bg-amber-950 text-amber-200" },
  booked: { label: "Booked", symbol: "✕", color: "border-line bg-raised text-zinc-400" },
  blocked: { label: "Blocked", symbol: "⊘", color: "border-dashed border-zinc-600 text-zinc-400" },
} as const;
type SeatState = keyof typeof stateStyle;

const unavailableState: Record<Exclude<SeatStatus, "AVAILABLE">, SeatState> = {
  HELD: "held",
  BOOKED: "booked",
  BLOCKED: "blocked",
};

const seatBase =
  "flex h-10 w-10 shrink-0 flex-col items-center justify-center rounded-t-xl rounded-b-md border text-xs leading-none transition-colors disabled:cursor-not-allowed";

// One tier for the whole row ("Gold"), or "Mixed" when a row has more than one.
function rowTier(seats: readonly Seat[]): string {
  const types = new Set(seats.map((seat) => seat.type));
  return types.size === 1 ? typeLabel[seats[0].type] : "Mixed";
}

export default function SeatPicker({ seats, onBook }: SeatPickerProps) {
  const router = useRouter();
  const [chosen, setChosen] = useState<readonly string[]>([]);
  const [error, setError] = useState<string | null>(null);
  // useTransition gives `booking` (true while the server action runs), which disables the button.
  const [booking, startBooking] = useTransition();
  // One idempotency key per selection. A double click, or a retry after a slow answer, sends the
  // same key, so booking-service returns the first booking instead of making a second one.
  // Changing the selection is a new request, so it clears the key.
  const idempotencyKey = useRef<string | null>(null);

  // After a refresh a chosen seat may have been taken by someone else: drop it from the selection.
  const availableIds = new Set(seats.filter((s) => s.status === "AVAILABLE").map((s) => s.seatId));
  const selected = chosen.filter((id) => availableIds.has(id));
  const dropped = chosen.length - selected.length;
  const limitReached = selected.length >= MAX_SEATS;

  function toggle(seatId: string) {
    if (selected.includes(seatId)) {
      setChosen(selected.filter((id) => id !== seatId));
    } else if (!limitReached) {
      setChosen([...selected, seatId]);
    } else {
      return;
    }
    idempotencyKey.current = null;
    setError(null);
  }

  function book() {
    if (selected.length === 0) return;
    idempotencyKey.current ??= crypto.randomUUID();
    const key = idempotencyKey.current;
    setError(null);
    startBooking(async () => {
      const result = await onBook(selected, key);
      if (result?.error) {
        setError(result.error);
        // The seat map is probably stale (someone took a seat): load the current one.
        router.refresh();
      }
    });
  }

  return (
    <div className="flex flex-col gap-8">
      <div className="border-line bg-panel overflow-x-auto rounded-3xl border p-6 sm:p-8">
        <div className="mx-auto flex w-max min-w-full flex-col items-center gap-2">
          {/* The cinema screen, so it is clear which way the seats face. */}
          <div aria-hidden className="mb-8 w-full max-w-xl">
            <div className="from-brand-soft/80 h-2 rounded-[100%] bg-gradient-to-b to-transparent shadow-[0_18px_50px_10px_rgb(251_113_133/0.25)]" />
            <p className="mt-4 text-center text-[0.65rem] font-semibold tracking-[0.4em] text-zinc-400 uppercase">
              Screen
            </p>
          </div>

          <div role="group" aria-label="Seats" className="flex flex-col gap-2">
            {groupSeatsByRow(seats).map(({ row, seats: rowSeats }) => (
              <div
                key={row}
                role="group"
                aria-label={`Row ${row}`}
                className="flex items-center gap-2"
              >
                <span aria-hidden className="w-6 shrink-0 text-center text-sm text-zinc-400">
                  {row}
                </span>
                {rowSeats.map((seat) => {
                  const isSelected = selected.includes(seat.seatId);
                  const bookable = seat.status === "AVAILABLE";
                  const state: SeatState = isSelected
                    ? "selected"
                    : seat.status === "AVAILABLE"
                      ? "available"
                      : unavailableState[seat.status];
                  const color =
                    state === "available" ? typeOutline[seat.type] : stateStyle[state].color;
                  return (
                    <button
                      key={seat.seatId}
                      type="button"
                      disabled={!bookable || booking}
                      aria-pressed={bookable ? isSelected : undefined}
                      aria-label={`Seat ${seat.seatId}, ${typeLabel[seat.type]}, ${stateStyle[state].label.toLowerCase()}`}
                      onClick={() => toggle(seat.seatId)}
                      className={`${seatBase} ${color}`}
                    >
                      <span aria-hidden className="font-medium">
                        {stateStyle[state].symbol || seat.number}
                      </span>
                    </button>
                  );
                })}
                <span aria-hidden className="w-16 shrink-0 pl-1 text-xs text-zinc-400">
                  {rowTier(rowSeats)}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>

      <ul
        aria-label="Seat legend"
        className="flex flex-wrap justify-center gap-x-5 gap-y-2 text-sm text-zinc-300"
      >
        {(Object.keys(typeLabel) as SeatType[]).map((type) => (
          <li key={type} className="flex items-center gap-2">
            <span aria-hidden className={`h-3 w-3 rounded-full ${typeDot[type]}`} />
            {typeLabel[type]}
          </li>
        ))}
        {(["selected", "held", "booked", "blocked"] as const).map((state) => (
          <li key={state} className="flex items-center gap-2">
            <span
              aria-hidden
              className={`inline-flex h-6 w-6 items-center justify-center rounded-t-md rounded-b-sm border text-[0.65rem] ${stateStyle[state].color}`}
            >
              {stateStyle[state].symbol}
            </span>
            {stateStyle[state].label}
          </li>
        ))}
      </ul>

      <div className="flex justify-center">
        <Button variant="secondary" size="sm" onClick={() => router.refresh()} disabled={booking}>
          Refresh availability
        </Button>
      </div>

      {/* Stays at the bottom of the window while the seat grid scrolls. */}
      <div className="border-line bg-panel/95 sticky bottom-4 z-30 flex items-center justify-between gap-4 rounded-2xl border p-4 shadow-2xl shadow-black/60 backdrop-blur">
        <div aria-live="polite" className="flex min-w-0 flex-col gap-1">
          <p className="font-medium">
            {selected.length === 0
              ? "No seats selected."
              : `Selected ${selected.length} of ${MAX_SEATS}: ${selected.join(", ")}`}
          </p>
          {limitReached && (
            <p role="status" className="text-sm text-amber-200">
              You can select up to {MAX_SEATS} seats. Deselect one to pick another.
            </p>
          )}
          {dropped > 0 && (
            <p role="status" className="text-sm text-amber-200">
              {dropped === 1 ? "A seat you selected is" : `${dropped} seats you selected are`} no
              longer available.
            </p>
          )}
          {error && (
            <p role="alert" className="text-sm text-red-300">
              {error}
            </p>
          )}
        </div>
        <Button onClick={book} disabled={selected.length === 0 || booking} className="shrink-0">
          {booking ? "Holding your seats..." : "Continue to booking"}
        </Button>
      </div>
    </div>
  );
}
