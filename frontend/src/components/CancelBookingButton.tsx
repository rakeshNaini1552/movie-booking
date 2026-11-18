"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import Button from "@/components/Button";

type ActionResult = { error: string } | { ok: true };

type CancelBookingButtonProps = {
  bookingId: string;
  onCancel: (bookingId: string) => Promise<ActionResult>;
};

// Cancelling cannot be undone, so it takes two clicks: the first one only asks.
export default function CancelBookingButton({ bookingId, onCancel }: CancelBookingButtonProps) {
  const router = useRouter();
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [cancelling, startCancelling] = useTransition();

  function cancel() {
    setError(null);
    startCancelling(async () => {
      const result = await onCancel(bookingId);
      if ("error" in result) setError(result.error);
      setConfirming(false);
      router.refresh();
    });
  }

  return (
    <div className="flex flex-col items-start gap-3">
      {confirming ? (
        <div role="group" aria-label="Confirm cancellation" className="flex flex-col gap-3">
          <p className="text-sm text-zinc-300">
            Cancel this booking? Your seats are released and any payment is refunded.
          </p>
          <div className="flex flex-wrap gap-3">
            <Button variant="danger" onClick={cancel} disabled={cancelling}>
              {cancelling ? "Cancelling..." : "Yes, cancel booking"}
            </Button>
            <Button variant="ghost" onClick={() => setConfirming(false)} disabled={cancelling}>
              Keep booking
            </Button>
          </div>
        </div>
      ) : (
        <Button variant="secondary" onClick={() => setConfirming(true)}>
          Cancel booking
        </Button>
      )}
      {error && (
        <p role="alert" className="text-sm text-red-300">
          {error}
        </p>
      )}
    </div>
  );
}
