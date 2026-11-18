import { beforeEach, describe, expect, it, vi } from "vitest";

const { gatewayQuery } = vi.hoisted(() => ({ gatewayQuery: vi.fn() }));
vi.mock("server-only", () => ({}));
vi.mock("./client", () => ({ gatewayQuery }));

import { GatewayError } from "@/lib/gateway/errors";
import { getMovie, listMovies } from "./movies";

const ID = "3f1c2b0e-8a45-4d6e-9b7a-1c2d3e4f5a6b";
const movie = (n: number) => ({
  id: `00000000-0000-0000-0000-00000000000${n}`,
  title: `Movie ${n}`,
  durationMinutes: 100,
  language: "en",
  rating: null,
});

describe("listMovies", () => {
  beforeEach(() => vi.clearAllMocks());

  it("asks for one extra row and reports hasNext when it comes back", async () => {
    gatewayQuery.mockResolvedValue({ movies: [movie(1), movie(2), movie(3)] });

    const result = await listMovies({ titleContains: "inc", page: 2, size: 2 });

    expect(result.movies.map((m) => m.title)).toEqual(["Movie 1", "Movie 2"]);
    expect(result.hasNext).toBe(true);
    expect(gatewayQuery.mock.calls[0][1]).toEqual({ titleContains: "inc", page: 2, size: 3 });
  });

  it("reports no next page when the extra row is missing, and omits an empty search", async () => {
    gatewayQuery.mockResolvedValue({ movies: [movie(1)] });

    const result = await listMovies({ titleContains: "", page: 0, size: 2 });

    expect(result).toEqual({ movies: [movie(1)], hasNext: false });
    expect(gatewayQuery.mock.calls[0][1].titleContains).toBeUndefined();
  });

  it("rejects page and size values the catalog would reject", async () => {
    await expect(listMovies({ page: -1, size: 10 })).rejects.toThrow(RangeError);
    await expect(listMovies({ page: 0, size: 0 })).rejects.toThrow(RangeError);
    await expect(listMovies({ page: 0, size: 100 })).rejects.toThrow(RangeError);
    expect(gatewayQuery).not.toHaveBeenCalled();
  });

  it("selects only the fields the list shows", async () => {
    gatewayQuery.mockResolvedValue({ movies: [] });
    await listMovies({ page: 0, size: 5 });
    const query: string = gatewayQuery.mock.calls[0][0];
    expect(query).not.toMatch(/shows|description|createdAt|updatedAt/);
  });
});

describe("getMovie", () => {
  beforeEach(() => vi.clearAllMocks());

  it("returns the movie", async () => {
    gatewayQuery.mockResolvedValue({ movie: { id: ID } });
    await expect(getMovie(ID)).resolves.toEqual({ id: ID });
    expect(gatewayQuery.mock.calls[0][1]).toEqual({ id: ID });
  });

  it("returns null when the catalog answers NOT_FOUND", async () => {
    gatewayQuery.mockRejectedValue(new GatewayError("graphql", { classifications: ["NOT_FOUND"] }));
    await expect(getMovie(ID)).resolves.toBeNull();
  });

  it("returns null for a null movie", async () => {
    gatewayQuery.mockResolvedValue({ movie: null });
    await expect(getMovie(ID)).resolves.toBeNull();
  });

  it("returns null for an id that is not an id, without calling the gateway", async () => {
    await expect(getMovie("not-an-id")).resolves.toBeNull();
    expect(gatewayQuery).not.toHaveBeenCalled();
  });

  it("rethrows other failures", async () => {
    gatewayQuery.mockRejectedValue(new GatewayError("timeout"));
    await expect(getMovie(ID)).rejects.toMatchObject({ kind: "timeout" });
  });
});

describe("listNowShowing", () => {
  it("keeps only movies with an upcoming, not cancelled show, soonest first", async () => {
    const { gatewayQuery } = await import("./client");
    const { listNowShowing } = await import("./movies");
    const movie = (n: number, shows: { startsAt: string; status: string }[]) => ({
      id: `00000000-0000-0000-0000-00000000000${n}`,
      title: `Movie ${n}`,
      durationMinutes: 100,
      language: "en",
      rating: null,
      shows,
    });
    vi.mocked(gatewayQuery).mockResolvedValue({
      movies: [
        movie(1, []),
        movie(2, [{ startsAt: "2026-12-05T10:00:00Z", status: "SCHEDULED" }]),
        movie(3, [{ startsAt: "2026-12-02T10:00:00Z", status: "CANCELLED" }]),
        movie(4, [{ startsAt: "2026-11-01T10:00:00Z", status: "SCHEDULED" }]),
        movie(5, [{ startsAt: "2026-12-03T10:00:00Z", status: "SCHEDULED" }]),
      ],
    });
    const result = await listNowShowing(4, new Date("2026-12-01T00:00:00Z"));
    expect(result.map((m) => m.title)).toEqual(["Movie 5", "Movie 2"]);
    expect(result[0]).not.toHaveProperty("shows");
  });
});
