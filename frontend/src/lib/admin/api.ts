import "server-only";
import { GatewayNotFoundError } from "@/lib/gateway/errors";
import { gatewayGet, gatewaySend, gatewaySendNoContent } from "@/lib/gateway/rest";
import {
  type AdminMovie,
  type AdminScreen,
  type AdminShow,
  type AdminTheater,
  adminMovieSchema,
  adminScreenSchema,
  adminShowSchema,
  adminTheaterSchema,
  pageOf,
} from "./schemas";

// Catalog writes. The gateway forwards the admin's token and catalog-service checks the admin
// role on every POST, PUT and DELETE, so these calls fail with 403 for anyone else.

export const ADMIN_PAGE_SIZE = 20;
// Screens of one theater and theaters in the "new show" picker are loaded in one page.
const PICKER_PAGE_SIZE = 100;

const orNull = async <T>(call: () => Promise<T>): Promise<T | null> => {
  try {
    return await call();
  } catch (error) {
    if (error instanceof GatewayNotFoundError) return null;
    throw error;
  }
};

export type MovieInput = {
  title: string;
  description: string | null;
  durationMinutes: number;
  language: string;
  rating: string | null;
  releaseDate: string | null;
  status: "ACTIVE" | "ARCHIVED";
};

export function listAdminMovies(title: string, page: number) {
  const query = new URLSearchParams({
    page: String(page),
    size: String(ADMIN_PAGE_SIZE),
    sort: "title,asc",
  });
  if (title) query.set("title", title);
  return gatewayGet("admin", `/api/v1/movies?${query}`, pageOf(adminMovieSchema));
}

export const getAdminMovie = (id: string): Promise<AdminMovie | null> =>
  orNull(() => gatewayGet("admin", `/api/v1/movies/${id}`, adminMovieSchema));

export const createMovie = (input: MovieInput): Promise<AdminMovie> =>
  gatewaySend("admin", "/api/v1/movies", { method: "POST", body: input }, adminMovieSchema);

export const updateMovie = (id: string, input: MovieInput): Promise<AdminMovie> =>
  gatewaySend("admin", `/api/v1/movies/${id}`, { method: "PUT", body: input }, adminMovieSchema);

export function listTheaters(page: number, size = ADMIN_PAGE_SIZE) {
  const query = new URLSearchParams({ page: String(page), size: String(size), sort: "name,asc" });
  return gatewayGet("admin", `/api/v1/theaters?${query}`, pageOf(adminTheaterSchema));
}

export const listAllTheaters = async (): Promise<AdminTheater[]> =>
  (await listTheaters(0, PICKER_PAGE_SIZE)).content;

export const getTheater = (id: string): Promise<AdminTheater | null> =>
  orNull(() => gatewayGet("admin", `/api/v1/theaters/${id}`, adminTheaterSchema));

export const createTheater = (input: {
  name: string;
  city: string;
  address: string;
}): Promise<AdminTheater> =>
  gatewaySend("admin", "/api/v1/theaters", { method: "POST", body: input }, adminTheaterSchema);

export const deleteTheater = (id: string): Promise<void> =>
  gatewaySendNoContent("admin", `/api/v1/theaters/${id}`, { method: "DELETE" });

export async function listScreens(theaterId: string): Promise<AdminScreen[]> {
  const query = new URLSearchParams({
    page: "0",
    size: String(PICKER_PAGE_SIZE),
    sort: "name,asc",
  });
  const page = await gatewayGet(
    "admin",
    `/api/v1/theaters/${theaterId}/screens?${query}`,
    pageOf(adminScreenSchema),
  );
  return page.content;
}

export const createScreen = (
  theaterId: string,
  input: { name: string; seatRows: number; seatsPerRow: number },
): Promise<AdminScreen> =>
  gatewaySend(
    "admin",
    `/api/v1/theaters/${theaterId}/screens`,
    { method: "POST", body: input },
    adminScreenSchema,
  );

export const createShow = (input: {
  movieId: string;
  screenId: string;
  startsAt: string;
  endsAt: string;
  basePrice: number;
}): Promise<AdminShow> =>
  gatewaySend("admin", "/api/v1/shows", { method: "POST", body: input }, adminShowSchema);

export const cancelShow = (id: string): Promise<AdminShow> =>
  gatewaySend("admin", `/api/v1/shows/${id}/cancel`, { method: "POST" }, adminShowSchema);
