import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

const { refresh } = vi.hoisted(() => ({ refresh: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => ({ refresh }) }));

import { MAX_SEATS } from "@/lib/seats/limits";
import type { Seat } from "@/lib/seats/schemas";
import SeatPicker from "./SeatPicker";

const seat = (row: string, number: number, over: Partial<Seat> = {}): Seat => ({
  seatId: `${row}${number}`,
  row,
  number,
  type: "SILVER",
  status: "AVAILABLE",
  ...over,
});

const seats: Seat[] = [
  seat("A", 2),
  seat("A", 1, { type: "GOLD" }),
  seat("A", 3, { status: "BOOKED" }),
  seat("A", 4, { status: "HELD" }),
  seat("A", 5, { status: "BLOCKED", type: "PLATINUM" }),
  seat("B", 1),
];

describe("SeatPicker", () => {
  beforeEach(() => vi.clearAllMocks());

  it("lays out labelled rows with seats in number order", () => {
    render(<SeatPicker seats={seats} />);
    const rowA = screen.getByRole("group", { name: "Row A" });
    const names = within(rowA)
      .getAllByRole("button")
      .map((b) => b.getAttribute("aria-label"));
    expect(names[0]).toBe("Seat A1, Gold, available");
    expect(names[1]).toBe("Seat A2, Silver, available");
    expect(screen.getByRole("group", { name: "Row B" })).toBeInTheDocument();
  });

  it("gives every state an aria-label with seat, type and status, and disables unavailable seats", () => {
    render(<SeatPicker seats={seats} />);
    expect(screen.getByRole("button", { name: "Seat A3, Silver, booked" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Seat A4, Silver, held" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Seat A5, Platinum, blocked" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Seat A1, Gold, available" })).toBeEnabled();
  });

  it("shows a legend with every state and the seat types", () => {
    render(<SeatPicker seats={seats} />);
    const legend = screen.getByRole("list", { name: "Seat legend" });
    for (const text of [
      "Available",
      "Selected",
      "Held",
      "Booked",
      "Blocked",
      "S = Silver, G = Gold, P = Platinum",
    ]) {
      expect(within(legend).getByText(new RegExp(text))).toBeInTheDocument();
    }
  });

  it("toggles an available seat and summarises the selection", async () => {
    const user = userEvent.setup();
    render(<SeatPicker seats={seats} />);
    expect(screen.getByText("No seats selected.")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Seat A1, Gold, available" }));
    await user.click(screen.getByRole("button", { name: "Seat B1, Silver, available" }));
    expect(screen.getByText("Selected 2 of 6: A1, B1")).toBeInTheDocument();
    const a1 = screen.getByRole("button", { name: "Seat A1, Gold, selected" });
    expect(a1).toHaveAttribute("aria-pressed", "true");

    await user.click(a1);
    expect(screen.getByText("Selected 1 of 6: B1")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Seat A1, Gold, available" })).toHaveAttribute(
      "aria-pressed",
      "false",
    );
  });

  it("can be used from the keyboard", async () => {
    const user = userEvent.setup();
    render(<SeatPicker seats={seats} />);
    screen.getByRole("button", { name: "Seat A1, Gold, available" }).focus();
    await user.keyboard("{Enter}");
    expect(screen.getByText("Selected 1 of 6: A1")).toBeInTheDocument();
    await user.keyboard(" ");
    expect(screen.getByText("No seats selected.")).toBeInTheDocument();
  });

  it("does not select a disabled seat", async () => {
    const user = userEvent.setup();
    render(<SeatPicker seats={seats} />);
    await user.click(screen.getByRole("button", { name: "Seat A3, Silver, booked" }));
    expect(screen.getByText("No seats selected.")).toBeInTheDocument();
  });

  it(`stops at ${MAX_SEATS} seats and says so, and frees a slot when one is deselected`, async () => {
    const user = userEvent.setup();
    const many = Array.from({ length: MAX_SEATS + 2 }, (_, i) => seat("C", i + 1));
    render(<SeatPicker seats={many} />);
    expect(screen.queryByText(/You can select up to/)).not.toBeInTheDocument();

    for (let n = 1; n <= MAX_SEATS + 1; n++) {
      await user.click(screen.getByRole("button", { name: new RegExp(`^Seat C${n}, `) }));
    }
    expect(
      screen.getByText(`Selected ${MAX_SEATS} of ${MAX_SEATS}: C1, C2, C3, C4, C5, C6`),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /^Seat C7, .*available/ })).toHaveAttribute(
      "aria-pressed",
      "false",
    );
    expect(
      screen.getByText(`You can select up to ${MAX_SEATS} seats. Deselect one to pick another.`),
    ).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /^Seat C1, .*selected/ }));
    await user.click(screen.getByRole("button", { name: /^Seat C7, / }));
    expect(screen.getByText(/C2, C3, C4, C5, C6, C7/)).toBeInTheDocument();
  });

  it("has a disabled Continue button for now", () => {
    render(<SeatPicker seats={seats} />);
    expect(screen.getByRole("button", { name: "Continue to booking" })).toBeDisabled();
  });

  it("refreshes the route when Refresh is clicked, without polling", async () => {
    const user = userEvent.setup();
    render(<SeatPicker seats={seats} />);
    await user.click(screen.getByRole("button", { name: "Refresh availability" }));
    expect(refresh).toHaveBeenCalledTimes(1);
  });

  it("drops a selected seat that is taken after a refresh, and tells the user", async () => {
    const user = userEvent.setup();
    const { rerender } = render(<SeatPicker seats={seats} />);
    await user.click(screen.getByRole("button", { name: "Seat A1, Gold, available" }));
    await user.click(screen.getByRole("button", { name: "Seat B1, Silver, available" }));

    rerender(
      <SeatPicker seats={seats.map((s) => (s.seatId === "A1" ? { ...s, status: "BOOKED" } : s))} />,
    );

    expect(screen.getByText("Selected 1 of 6: B1")).toBeInTheDocument();
    expect(screen.getByText(/A seat you selected is no longer available/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Seat A1, Gold, booked" })).toBeDisabled();
  });
});
