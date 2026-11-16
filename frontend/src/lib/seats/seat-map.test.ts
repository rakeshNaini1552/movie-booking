import { beforeEach, describe, expect, it, vi } from "vitest";

const { gatewayGet } = vi.hoisted(() => ({ gatewayGet: vi.fn() }));
vi.mock("server-only", () => ({}));
vi.mock("@/lib/gateway/rest", () => ({ gatewayGet }));

import { GatewayError, GatewayNotFoundError } from "@/lib/gateway/errors";
import { seatMapSchema } from "./schemas";
import { getSeatMap } from "./seat-map";

const SHOW = "3f1c2b0e-8a45-4d6e-9b7a-1c2d3e4f5a6b";

describe("getSeatMap", () => {
  beforeEach(() => vi.clearAllMocks());

  it("requests the show's seat map and returns it", async () => {
    const seats = [{ seatId: "A1", row: "A", number: 1, type: "GOLD", status: "AVAILABLE" }];
    gatewayGet.mockResolvedValue(seats);
    await expect(getSeatMap(SHOW)).resolves.toEqual(seats);
    expect(gatewayGet).toHaveBeenCalledWith("seats", `/api/v1/shows/${SHOW}/seats`, seatMapSchema);
  });

  it("returns null when the show has no seats (404)", async () => {
    gatewayGet.mockRejectedValue(new GatewayNotFoundError());
    await expect(getSeatMap(SHOW)).resolves.toBeNull();
  });

  it("returns null for a show id that is not an id, without calling the gateway", async () => {
    await expect(getSeatMap("../bookings")).resolves.toBeNull();
    expect(gatewayGet).not.toHaveBeenCalled();
  });

  it("rethrows other failures", async () => {
    gatewayGet.mockRejectedValue(new GatewayError("timeout"));
    await expect(getSeatMap(SHOW)).rejects.toMatchObject({ kind: "timeout" });
  });
});

describe("seatMapSchema", () => {
  const seat = { seatId: "A1", row: "A", number: 1, type: "GOLD", status: "AVAILABLE" };
  it("accepts the seat service shape", () => {
    expect(seatMapSchema.safeParse([seat]).success).toBe(true);
  });
  it.each([{ type: "VIP" }, { status: "FREE" }, { number: "1" }, { number: 0 }])(
    "rejects %j",
    (change) => {
      expect(seatMapSchema.safeParse([{ ...seat, ...change }]).success).toBe(false);
    },
  );
});
