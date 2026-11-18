import { describe, expect, it, vi } from "vitest";

const order: string[] = [];

vi.mock("next/server", () => ({
  connection: vi.fn(async () => {
    order.push("connection");
  }),
}));
vi.mock("@/auth", () => ({
  auth: vi.fn(async () => {
    order.push("auth");
    return null;
  }),
}));
vi.mock("@/lib/env", () => ({ env: { CATALOG_ADMIN_ROLE: "catalog-admin" } }));
vi.mock("@/lib/auth/actions", () => ({ signInAction: vi.fn(), signOutAction: vi.fn() }));

import AuthStatus from "./AuthStatus";

describe("AuthStatus", () => {
  it("opts into request time before reading the session", async () => {
    await AuthStatus();
    expect(order).toEqual(["connection", "auth"]);
  });
});
