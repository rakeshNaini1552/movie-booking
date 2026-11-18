import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

const { refresh, onCancel } = vi.hoisted(() => ({ refresh: vi.fn(), onCancel: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => ({ refresh }) }));

import CancelBookingButton from "./CancelBookingButton";

describe("CancelBookingButton", () => {
  beforeEach(() => vi.clearAllMocks());

  it("asks first, and does nothing when the customer keeps the booking", async () => {
    const user = userEvent.setup();
    render(<CancelBookingButton bookingId="b1" onCancel={onCancel} />);
    await user.click(screen.getByRole("button", { name: "Cancel booking" }));
    await user.click(screen.getByRole("button", { name: "Keep booking" }));
    expect(onCancel).not.toHaveBeenCalled();
    expect(screen.getByRole("button", { name: "Cancel booking" })).toBeInTheDocument();
  });

  it("cancels after confirmation and shows why when it cannot", async () => {
    const user = userEvent.setup();
    onCancel.mockResolvedValue({ error: "This booking can no longer be cancelled." });
    render(<CancelBookingButton bookingId="b1" onCancel={onCancel} />);
    await user.click(screen.getByRole("button", { name: "Cancel booking" }));
    await user.click(screen.getByRole("button", { name: "Yes, cancel booking" }));
    expect(onCancel).toHaveBeenCalledWith("b1");
    expect(await screen.findByRole("alert")).toHaveTextContent("no longer be cancelled");
    expect(refresh).toHaveBeenCalled();
  });
});
