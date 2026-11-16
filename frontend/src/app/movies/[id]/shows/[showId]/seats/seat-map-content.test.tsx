import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const { getMovie, getSeatMap, notFound, redirect } = vi.hoisted(() => ({
  getMovie: vi.fn(),
  getSeatMap: vi.fn(),
  notFound: vi.fn(() => {
    throw new Error("NEXT_NOT_FOUND");
  }),
  redirect: vi.fn((url: string) => {
    throw new Error(`NEXT_REDIRECT ${url}`);
  }),
}));
vi.mock("@/lib/catalog/movies", () => ({ getMovie }));
vi.mock("@/lib/seats/seat-map", () => ({ getSeatMap }));
vi.mock("next/navigation", () => ({ notFound, redirect, useRouter: () => ({ refresh: vi.fn() }) }));

import { GatewayError, NotSignedInError } from "@/lib/gateway/errors";
import SeatMapContent from "./seat-map-content";

const MOVIE = "3f1c2b0e-8a45-4d6e-9b7a-1c2d3e4f5a6b";
const SHOW = "00000000-0000-0000-0000-000000000001";
const movie = {
  id: MOVIE,
  title: "Inception",
  shows: [
    {
      id: SHOW,
      startsAt: "2026-12-01T18:30:00Z",
      basePrice: 12.5,
      status: "SCHEDULED",
      screen: { name: "Screen 1", theater: { id: "t1", name: "Alpha Cinema", city: "Austin" } },
    },
    {
      id: "00000000-0000-0000-0000-000000000002",
      startsAt: "2026-12-02T18:30:00Z",
      basePrice: 9,
      status: "CANCELLED",
      screen: { name: "Screen 2", theater: { id: "t1", name: "Alpha Cinema", city: "Austin" } },
    },
  ],
};
const seats = [{ seatId: "A1", row: "A", number: 1, type: "GOLD", status: "AVAILABLE" }];

const run = (id = MOVIE, showId = SHOW) =>
  SeatMapContent({ params: Promise.resolve({ id, showId }) });

describe("SeatMapContent", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getMovie.mockResolvedValue(movie);
    getSeatMap.mockResolvedValue(seats);
  });

  it("shows the movie, theater, screen, start time (UTC) and price above the seat grid", async () => {
    render(await run());
    expect(
      screen.getByRole("heading", { level: 1, name: "Select seats: Inception" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Alpha Cinema, Austin")).toBeInTheDocument();
    expect(screen.getByText("Screen 1")).toBeInTheDocument();
    expect(screen.getByText("Tue, Dec 1, 18:30 UTC")).toBeInTheDocument();
    expect(screen.getByText("12.50")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Seat A1, Gold, available" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Back to Inception" })).toHaveAttribute(
      "href",
      `/movies/${MOVIE}`,
    );
    expect(getSeatMap).toHaveBeenCalledWith(SHOW);
  });

  it("shows a clear message with a link back when the show has no seat map", async () => {
    getSeatMap.mockResolvedValue(null);
    render(await run());
    expect(screen.getByText("Seats are not available for this show yet.")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Back to the show list" })).toHaveAttribute(
      "href",
      `/movies/${MOVIE}`,
    );
    expect(screen.queryByRole("button", { name: /Seat/ })).not.toBeInTheDocument();
  });

  it("returns 404 when the show is not one of this movie's shows", async () => {
    await expect(run(MOVIE, "00000000-0000-0000-0000-0000000000ff")).rejects.toThrow(
      "NEXT_NOT_FOUND",
    );
  });

  it("returns 404 for a cancelled show", async () => {
    await expect(run(MOVIE, "00000000-0000-0000-0000-000000000002")).rejects.toThrow(
      "NEXT_NOT_FOUND",
    );
  });

  it("returns 404 when the movie does not exist", async () => {
    getMovie.mockResolvedValue(null);
    await expect(run()).rejects.toThrow("NEXT_NOT_FOUND");
  });

  it("returns 404 for malformed ids without calling the gateway", async () => {
    await expect(run("nope", SHOW)).rejects.toThrow("NEXT_NOT_FOUND");
    await expect(run(MOVIE, "1; drop")).rejects.toThrow("NEXT_NOT_FOUND");
    expect(getMovie).not.toHaveBeenCalled();
    expect(getSeatMap).not.toHaveBeenCalled();
  });

  it("redirects a signed-out user to sign-in, returning to the seat page", async () => {
    getSeatMap.mockRejectedValue(new NotSignedInError());
    await expect(run()).rejects.toThrow(
      `NEXT_REDIRECT /signin?callbackUrl=${encodeURIComponent(`/movies/${MOVIE}/shows/${SHOW}/seats`)}`,
    );
  });

  it("lets a gateway failure reach the error boundary", async () => {
    getSeatMap.mockRejectedValue(new GatewayError("timeout"));
    await expect(run()).rejects.toBeInstanceOf(GatewayError);
    expect(redirect).not.toHaveBeenCalled();
  });
});
