"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import Button from "@/components/Button";
import { groupSeatsByRow } from "@/lib/seats/group-seats";
import { MAX_SEATS } from "@/lib/seats/limits";
import type { Seat, SeatStatus, SeatType } from "@/lib/seats/schemas";

type SeatPickerProps = {
  seats: readonly Seat[];
};

const typeLabel: Record<SeatType, string> = {
  SILVER: "Silver",
  GOLD: "Gold",
  PLATINUM: "Platinum",
};
const typeLetter: Record<SeatType, string> = { SILVER: "S", GOLD: "G", PLATINUM: "P" };

// Every state has a text label and a symbol as well as a colour, so none relies on colour alone.
const stateStyle = {
  available: {
    label: "Available",
    symbol: "",
    color: "border-zinc-400 hover:bg-zinc-100 dark:border-zinc-500 dark:hover:bg-zinc-800",
  },
  selected: {
    label: "Selected",
    symbol: "✓",
    color: "border-indigo-700 bg-indigo-700 text-white dark:border-indigo-500 dark:bg-indigo-600",
  },
  held: {
    label: "Held",
    symbol: "◔",
    color: "border-amber-600 bg-amber-100 text-amber-900 dark:bg-amber-900 dark:text-amber-100",
  },
  booked: {
    label: "Booked",
    symbol: "✕",
    color:
      "border-zinc-400 bg-zinc-300 text-zinc-700 dark:border-zinc-600 dark:bg-zinc-700 dark:text-zinc-300",
  },
  blocked: {
    label: "Blocked",
    symbol: "⊘",
    color: "border-zinc-400 border-dashed text-zinc-600 dark:text-zinc-400",
  },
} as const;
type SeatState = keyof typeof stateStyle;

const unavailableState: Record<Exclude<SeatStatus, "AVAILABLE">, SeatState> = {
  HELD: "held",
  BOOKED: "booked",
  BLOCKED: "blocked",
};

// Picking only: nothing is held or booked from here. Selection lives in this component's state.
export default function SeatPicker({ seats }: SeatPickerProps) {
  const router = useRouter();
  const [chosen, setChosen] = useState<readonly string[]>([]);

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
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <ul aria-label="Seat legend" className="flex flex-wrap gap-x-4 gap-y-2 text-sm">
        {(Object.keys(stateStyle) as SeatState[]).map((state) => (
          <li key={state} className="flex items-center gap-2">
            <span
              aria-hidden
              className={`inline-flex h-6 w-6 items-center justify-center rounded border text-xs ${stateStyle[state].color}`}
            >
              {stateStyle[state].symbol}
            </span>
            {stateStyle[state].label}
          </li>
        ))}
        <li className="text-zinc-700 dark:text-zinc-300">
          Seat type:{" "}
          {(Object.keys(typeLabel) as SeatType[])
            .map((t) => `${typeLetter[t]} = ${typeLabel[t]}`)
            .join(", ")}
        </li>
      </ul>

      <div role="group" aria-label="Seats" className="flex flex-col gap-2 overflow-x-auto">
        {groupSeatsByRow(seats).map(({ row, seats: rowSeats }) => (
          <div key={row} role="group" aria-label={`Row ${row}`} className="flex items-center gap-2">
            <span aria-hidden className="w-6 shrink-0 text-center font-semibold">
              {row}
            </span>
            {rowSeats.map((seat) => {
              const isSelected = selected.includes(seat.seatId);
              const state: SeatState = isSelected
                ? "selected"
                : seat.status === "AVAILABLE"
                  ? "available"
                  : unavailableState[seat.status];
              const bookable = seat.status === "AVAILABLE";
              return (
                <button
                  key={seat.seatId}
                  type="button"
                  disabled={!bookable}
                  aria-pressed={bookable ? isSelected : undefined}
                  aria-label={`Seat ${seat.seatId}, ${typeLabel[seat.type]}, ${stateStyle[state].label.toLowerCase()}`}
                  onClick={() => toggle(seat.seatId)}
                  className={`flex h-11 w-11 shrink-0 flex-col items-center justify-center rounded border text-xs leading-tight focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600 disabled:cursor-not-allowed dark:focus-visible:outline-indigo-400 ${stateStyle[state].color}`}
                >
                  <span aria-hidden className="text-sm font-medium">
                    {seat.number}
                  </span>
                  <span aria-hidden>
                    {typeLetter[seat.type]}
                    {stateStyle[state].symbol}
                  </span>
                </button>
              );
            })}
          </div>
        ))}
      </div>

      <div aria-live="polite" className="flex flex-col gap-1">
        <p>
          {selected.length === 0
            ? "No seats selected."
            : `Selected ${selected.length} of ${MAX_SEATS}: ${selected.join(", ")}`}
        </p>
        {limitReached && (
          <p role="status" className="font-medium">
            You can select up to {MAX_SEATS} seats. Deselect one to pick another.
          </p>
        )}
        {dropped > 0 && (
          <p role="status" className="font-medium">
            {dropped === 1 ? "A seat you selected is" : `${dropped} seats you selected are`} no
            longer available.
          </p>
        )}
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <Button variant="secondary" onClick={() => router.refresh()}>
          Refresh availability
        </Button>
        <Button disabled aria-describedby="booking-note">
          Continue to booking
        </Button>
        <span id="booking-note" className="text-sm text-zinc-700 dark:text-zinc-300">
          Booking is not available yet.
        </span>
      </div>
    </div>
  );
}
