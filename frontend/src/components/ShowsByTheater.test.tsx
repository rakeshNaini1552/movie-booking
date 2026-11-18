import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { Show } from "@/lib/catalog/schemas";
import ShowsByTheater from "./ShowsByTheater";

const show = (n: number, theaterId: string, theaterName: string, startsAt: string): Show => ({
  id: `00000000-0000-0000-0000-00000000000${n}`,
  startsAt,
  basePrice: 12.5,
  status: "SCHEDULED",
  screen: { name: `Screen ${n}`, theater: { id: theaterId, name: theaterName, city: "Austin" } },
});

describe("ShowsByTheater", () => {
  it("lists each theater with its shows: time, screen and price", () => {
    render(
      <ShowsByTheater
        movieId="m1"
        shows={[
          show(1, "t1", "Alpha Cinema", "2026-12-01T18:30:00Z"),
          show(2, "t2", "Beta Cinema", "2026-12-01T20:00:00Z"),
        ]}
      />,
    );

    const alpha = screen.getByRole("region", { name: /Alpha Cinema/ });
    expect(within(alpha).getByText("Tue, Dec 1, 18:30 UTC")).toBeInTheDocument();
    expect(within(alpha).getByText(/Screen 1/)).toBeInTheDocument();
    expect(within(alpha).getByText(/12\.50/)).toBeInTheDocument();
    expect(screen.getByRole("region", { name: /Beta Cinema/ })).toBeInTheDocument();
    expect(
      within(alpha).getByRole("link", { name: /Select seats for .* at Alpha Cinema/ }),
    ).toHaveAttribute("href", "/movies/m1/shows/00000000-0000-0000-0000-000000000001/seats");
  });

  it("says so when the movie has no shows", () => {
    render(<ShowsByTheater movieId="m1" shows={[]} />);
    expect(screen.getByText("No shows are scheduled for this movie yet.")).toBeInTheDocument();
  });
});
