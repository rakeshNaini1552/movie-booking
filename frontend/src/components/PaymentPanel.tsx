"use client";

import { useRouter } from "next/navigation";
import { useRef, useState, useTransition } from "react";
import Button from "@/components/Button";
import { PAYMENT_METHODS, type PaymentMethodId } from "@/lib/bookings/payment-methods";

type ActionResult = { error: string } | { ok: true };

type PaymentPanelProps = {
  bookingId: string;
  // Already formatted, for example "500.00".
  total: string;
  // Server action, passed in by the page so this component imports no server code.
  onPay: (input: {
    bookingId: string;
    method: PaymentMethodId;
    idempotencyKey: string;
  }) => Promise<ActionResult>;
};

export default function PaymentPanel({ bookingId, total, onPay }: PaymentPanelProps) {
  const router = useRouter();
  const [method, setMethod] = useState<PaymentMethodId>(PAYMENT_METHODS[0].id);
  const [error, setError] = useState<string | null>(null);
  const [paying, startPaying] = useTransition();
  // One key per payment attempt: a double click sends the same key and is charged once. After an
  // answer arrives (approved or declined) the next click is a new attempt and gets a new key.
  const idempotencyKey = useRef<string | null>(null);

  function pay() {
    idempotencyKey.current ??= crypto.randomUUID();
    const key = idempotencyKey.current;
    setError(null);
    startPaying(async () => {
      const result = await onPay({ bookingId, method, idempotencyKey: key });
      idempotencyKey.current = null;
      if ("error" in result) setError(result.error);
      // Reload the booking from the server: it is now confirmed, or no longer payable.
      router.refresh();
    });
  }

  return (
    <section
      aria-labelledby="payment-heading"
      className="border-line bg-panel rounded-3xl border p-6"
    >
      <h2 id="payment-heading" className="font-display text-xl font-bold">
        Payment
      </h2>
      <p className="mt-1 text-sm text-zinc-400">This is a test payment. No real card is charged.</p>
      <fieldset className="mt-5 flex flex-col gap-3" disabled={paying}>
        <legend className="sr-only">Payment method</legend>
        {PAYMENT_METHODS.map((option) => (
          <label
            key={option.id}
            className="border-line bg-raised has-checked:border-brand-soft has-checked:bg-brand/10 flex cursor-pointer items-start gap-3 rounded-2xl border p-4 transition-colors"
          >
            <input
              type="radio"
              name="payment-method"
              value={option.id}
              checked={method === option.id}
              onChange={() => setMethod(option.id)}
              className="accent-brand mt-1 h-4 w-4"
            />
            <span>
              <span className="block font-medium">{option.label}</span>
              <span className="block text-sm text-zinc-400">{option.hint}</span>
            </span>
          </label>
        ))}
      </fieldset>
      {error && (
        <p role="alert" className="mt-4 text-sm text-red-300">
          {error}
        </p>
      )}
      <Button size="lg" className="mt-6 w-full" onClick={pay} disabled={paying}>
        {paying ? "Processing payment..." : `Pay ${total}`}
      </Button>
    </section>
  );
}
