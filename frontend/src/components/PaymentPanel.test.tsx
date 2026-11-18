import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

const { refresh, onPay } = vi.hoisted(() => ({ refresh: vi.fn(), onPay: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => ({ refresh }) }));

import PaymentPanel from "./PaymentPanel";

const BOOKING = "3f1c2b0e-8a45-4d6e-9b7a-1c2d3e4f5a6b";
const panel = <PaymentPanel bookingId={BOOKING} total="500.00" onPay={onPay} />;

describe("PaymentPanel", () => {
  beforeEach(() => vi.clearAllMocks());

  it("pays with the chosen method and reloads the booking", async () => {
    const user = userEvent.setup();
    onPay.mockResolvedValue({ ok: true });
    render(panel);
    await user.click(screen.getByRole("button", { name: "Pay 500.00" }));
    expect(onPay).toHaveBeenCalledWith({
      bookingId: BOOKING,
      method: "approve",
      idempotencyKey: expect.stringMatching(/^[0-9a-f-]{36}$/),
    });
    expect(refresh).toHaveBeenCalled();
  });

  it("shows a declined card and uses a new idempotency key for the next attempt", async () => {
    const user = userEvent.setup();
    onPay.mockResolvedValue({ error: "The card was declined. Try another card." });
    render(panel);
    await user.click(screen.getByLabelText(/Test card that declines/));
    await user.click(screen.getByRole("button", { name: "Pay 500.00" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("declined");
    expect(onPay.mock.calls[0][0].method).toBe("decline");

    await user.click(screen.getByRole("button", { name: "Pay 500.00" }));
    expect(onPay.mock.calls[1][0].idempotencyKey).not.toBe(onPay.mock.calls[0][0].idempotencyKey);
  });
});
