// A stand-in for the real gateway and the services behind it, used only by the end-to-end tests.
// It keeps everything in memory and speaks the same HTTP contract the frontend uses:
// GraphQL catalog reads, the seat map, bookings, payments, tickets and the admin REST calls.
// The tests never talk to Keycloak either: the "token" is just a name ("e2e-user", "e2e-admin").
//
// Run by Playwright (see playwright.config.ts). POST /__reset puts the data back to its start state.
import { createServer } from "node:http";
import { randomUUID } from "node:crypto";

const PORT = Number(process.env.MOCK_GATEWAY_PORT ?? 8099);
const SEAT_PRICE = 250;
const ADMIN_TOKEN = "e2e-admin";

const uuid = (n) => `00000000-0000-4000-8000-${String(n).padStart(12, "0")}`;
const now = () => new Date().toISOString();

const TITLES = [
  ["The Lighthouse Keeper", 118, "English", "PG-13"],
  ["Midnight in Marrakesh", 104, "French", "PG"],
  ["Paper Satellites", 131, "English", "PG-13"],
  ["The Last Monsoon", 142, "Hindi", "U/A"],
  ["Quiet Thunder", 97, "English", "R"],
  ["Salt and Ember", 109, "Spanish", "PG-13"],
  ["A Field of Clocks", 125, "English", "PG"],
  ["Northbound", 101, "Korean", "PG-13"],
  ["The Glass Orchard", 113, "English", "PG"],
  ["Seven Borrowed Days", 122, "Telugu", "U"],
  ["Low Tide Radio", 94, "English", "PG-13"],
  ["The Cartographer's Daughter", 137, "Italian", "PG"],
  ["Static Bloom", 99, "Japanese", "R"],
  ["Harbor Lights", 116, "English", "PG"],
];

let state;

function reset() {
  const theaters = [
    { id: uuid(101), name: "Aurora Cinemas", city: "Dallas", address: "12 Elm Street" },
    { id: uuid(102), name: "Grand Palace", city: "Frisco", address: "400 Main Street" },
  ];
  const screens = [
    { id: uuid(201), theaterId: uuid(101), name: "Screen 1", seatRows: 6, seatsPerRow: 10 },
    { id: uuid(202), theaterId: uuid(102), name: "IMAX", seatRows: 6, seatsPerRow: 10 },
  ];
  const movies = TITLES.map(([title, durationMinutes, language, rating], index) => ({
    id: uuid(index + 1),
    title,
    description: `${title} follows two strangers whose plans fall apart over one long night, and what they decide to do about it.`,
    durationMinutes,
    language,
    rating,
    releaseDate: "2026-10-02",
    status: "ACTIVE",
    createdAt: now(),
    updatedAt: now(),
  }));
  const shows = [];
  movies.slice(0, 6).forEach((movie, index) => {
    for (const [slot, screenId] of [
      [0, uuid(201)],
      [1, uuid(202)],
    ]) {
      const startsAt = new Date(Date.UTC(2026, 11, 1 + index, 15 + slot * 4, 30));
      shows.push({
        id: uuid(300 + index * 2 + slot),
        movieId: movie.id,
        screenId,
        startsAt: startsAt.toISOString(),
        endsAt: new Date(startsAt.getTime() + movie.durationMinutes * 60000).toISOString(),
        basePrice: 250,
        status: "SCHEDULED",
        createdAt: now(),
        updatedAt: now(),
      });
    }
  });
  state = { theaters, screens, movies, shows, bookings: [], tickets: [], taken: new Map() };
  // A few seats are already gone, so every seat state is visible in the first show.
  state.taken.set(
    shows[0].id,
    new Map([
      ["C4", "BOOKED"],
      ["C5", "BOOKED"],
      ["D7", "HELD"],
      ["F1", "BLOCKED"],
    ]),
  );
}
reset();

const seatType = (row) => (row < 2 ? "SILVER" : row < 4 ? "GOLD" : "PLATINUM");

function seatMap(show) {
  const screen = state.screens.find((s) => s.id === show.screenId);
  const taken = state.taken.get(show.id) ?? new Map();
  const seats = [];
  for (let row = 0; row < screen.seatRows; row++) {
    const letter = String.fromCharCode(65 + row);
    for (let number = 1; number <= screen.seatsPerRow; number++) {
      const seatId = `${letter}${number}`;
      seats.push({
        seatId,
        row: letter,
        number,
        type: seatType(row),
        status: taken.get(seatId) ?? "AVAILABLE",
      });
    }
  }
  return seats;
}

const page = (items, url) => {
  const number = Number(url.searchParams.get("page") ?? 0);
  const size = Number(url.searchParams.get("size") ?? 20);
  return {
    content: items.slice(number * size, number * size + size),
    page: number,
    size,
    totalElements: items.length,
    totalPages: Math.ceil(items.length / size),
  };
};

const graphMovie = (movie) => ({
  ...movie,
  shows: state.shows
    .filter((show) => show.movieId === movie.id)
    .map((show) => {
      const screen = state.screens.find((s) => s.id === show.screenId);
      return {
        ...show,
        screen: { ...screen, theater: state.theaters.find((t) => t.id === screen.theaterId) },
      };
    }),
});

function graphql({ variables = {} }) {
  if ("id" in variables) {
    const movie = state.movies.find((m) => m.id === variables.id);
    if (!movie) {
      return {
        data: { movie: null },
        errors: [{ message: "Movie not found", extensions: { classification: "NOT_FOUND" } }],
      };
    }
    return { data: { movie: graphMovie(movie) } };
  }
  const aliased = Object.keys(variables).filter((key) => /^id\d+$/.test(key));
  if (aliased.length > 0) {
    const data = {};
    for (const key of aliased) {
      const movie = state.movies.find((m) => m.id === variables[key]);
      data[`m${key.slice(2)}`] = movie ? graphMovie(movie) : null;
    }
    return { data };
  }
  const { titleContains, page: number = 0, size = 20 } = variables;
  const matches = state.movies.filter(
    (movie) =>
      movie.status === "ACTIVE" &&
      (!titleContains || movie.title.toLowerCase().includes(titleContains.toLowerCase())),
  );
  return { data: { movies: matches.slice(number * size, number * size + size).map(graphMovie) } };
}

const bookingView = (b) => ({
  bookingId: b.bookingId,
  showId: b.showId,
  status: b.status,
  totalAmount: b.totalAmount,
  seatIds: b.seatIds,
});

// Returns [status, body]. `user` is the bearer token, which doubles as the user's name here.
function route(method, url, body, user, headers) {
  const path = url.pathname;
  const isAdmin = user === ADMIN_TOKEN;
  let match;

  if (method === "POST" && path === "/graphql") return [200, graphql(body)];

  if (method === "GET" && (match = path.match(/^\/api\/v1\/shows\/([^/]+)\/seats$/))) {
    const show = state.shows.find((s) => s.id === match[1]);
    return show ? [200, seatMap(show)] : [404, { message: "No seats" }];
  }

  // ---- bookings ----
  if (method === "POST" && path === "/api/v1/bookings") {
    const key = headers["idempotency-key"];
    const existing = state.bookings.find((b) => b.idempotencyKey === key && b.userId === user);
    if (existing) return [201, { bookingId: existing.bookingId }];
    const show = state.shows.find((s) => s.id === body.showId);
    if (!show) return [400, { message: "Unknown show" }];
    const taken = state.taken.get(show.id) ?? new Map();
    if (body.seatIds.some((seat) => taken.has(seat)))
      return [409, { message: "Seats unavailable" }];
    for (const seat of body.seatIds) taken.set(seat, "HELD");
    state.taken.set(show.id, taken);
    const booking = {
      bookingId: randomUUID(),
      userId: user,
      showId: show.id,
      status: "PENDING",
      totalAmount: SEAT_PRICE * body.seatIds.length,
      seatIds: body.seatIds,
      createdAt: now(),
      idempotencyKey: key,
    };
    state.bookings.unshift(booking);
    return [201, { bookingId: booking.bookingId }];
  }
  if (method === "GET" && path === "/api/v1/bookings") {
    const mine = state.bookings.filter((b) => b.userId === user);
    const number = Number(url.searchParams.get("page") ?? 0);
    const size = Number(url.searchParams.get("size") ?? 20);
    return [
      200,
      {
        items: mine
          .slice(number * size, number * size + size)
          .map((b) => ({ ...bookingView(b), createdAt: b.createdAt })),
        page: number,
        size,
        totalElements: mine.length,
        totalPages: Math.ceil(mine.length / size),
      },
    ];
  }
  if ((match = path.match(/^\/api\/v1\/bookings\/([^/]+)(\/payments|\/cancel)?$/))) {
    const booking = state.bookings.find((b) => b.bookingId === match[1] && b.userId === user);
    if (!booking) return [404, { message: "Booking not found" }];
    const taken = state.taken.get(booking.showId);
    if (method === "GET" && !match[2]) return [200, bookingView(booking)];
    if (method === "POST" && match[2] === "/payments") {
      if (booking.status !== "PENDING") return [409, { message: "Booking is not payable" }];
      if (body.paymentToken === "tok_decline") {
        return [200, { bookingId: booking.bookingId, status: "PENDING", paymentStatus: "FAILED" }];
      }
      if (body.paymentToken !== "tok_success") return [400, { message: "Unknown payment token" }];
      booking.status = "CONFIRMED";
      for (const seat of booking.seatIds) taken.set(seat, "BOOKED");
      const ticketId = randomUUID();
      state.tickets.push({
        ticketId,
        bookingId: booking.bookingId,
        userId: user,
        status: "ISSUED",
        qrPayload: `${ticketId}.mock-signature`,
        issuedAt: now(),
        usedAt: null,
      });
      return [200, { bookingId: booking.bookingId, status: "CONFIRMED", paymentStatus: "SUCCESS" }];
    }
    if (method === "POST" && match[2] === "/cancel") {
      if (["FAILED", "EXPIRED"].includes(booking.status))
        return [409, { message: "Not cancellable" }];
      booking.status = "CANCELLED";
      for (const seat of booking.seatIds) taken.delete(seat);
      const ticket = state.tickets.find((t) => t.bookingId === booking.bookingId);
      if (ticket) ticket.status = "VOID";
      return [200, { bookingId: booking.bookingId, status: "CANCELLED" }];
    }
  }

  if (method === "GET" && (match = path.match(/^\/api\/v1\/tickets\/by-booking\/([^/]+)$/))) {
    const ticket = state.tickets.find((t) => t.bookingId === match[1] && t.userId === user);
    if (!ticket) return [404, { message: "Ticket not found" }];
    const { ticketId, bookingId, status, qrPayload, issuedAt, usedAt } = ticket;
    return [200, { ticketId, bookingId, status, qrPayload, issuedAt, usedAt }];
  }

  // ---- catalog REST (reads for everyone, writes for the admin) ----
  const write = method !== "GET";
  if (write && path.startsWith("/api/v1/") && !isAdmin) return [403, { detail: "Forbidden" }];

  if (path === "/api/v1/movies") {
    if (method === "GET") {
      const title = url.searchParams.get("title")?.toLowerCase();
      const items = state.movies
        .filter((m) => !title || m.title.toLowerCase().includes(title))
        .toSorted((a, b) => a.title.localeCompare(b.title));
      return [200, page(items, url)];
    }
    if (method === "POST") {
      const movie = {
        ...body,
        status: body.status ?? "ACTIVE",
        id: randomUUID(),
        createdAt: now(),
        updatedAt: now(),
      };
      state.movies.push(movie);
      return [201, movie];
    }
  }
  if ((match = path.match(/^\/api\/v1\/movies\/([^/]+)$/))) {
    const movie = state.movies.find((m) => m.id === match[1]);
    if (!movie) return [404, { detail: "Movie not found" }];
    if (method === "GET") return [200, movie];
    if (method === "PUT") return [200, Object.assign(movie, body, { updatedAt: now() })];
  }
  if (path === "/api/v1/theaters") {
    if (method === "GET") return [200, page(state.theaters, url)];
    if (method === "POST") {
      const theater = { ...body, id: randomUUID() };
      state.theaters.push(theater);
      return [201, theater];
    }
  }
  if ((match = path.match(/^\/api\/v1\/theaters\/([^/]+)$/))) {
    const theater = state.theaters.find((t) => t.id === match[1]);
    if (!theater) return [404, { detail: "Theater not found" }];
    if (method === "GET") return [200, theater];
    if (method === "DELETE") {
      if (state.screens.some((s) => s.theaterId === theater.id))
        return [409, { detail: "Theater has screens" }];
      state.theaters = state.theaters.filter((t) => t !== theater);
      return [204, null];
    }
  }
  if ((match = path.match(/^\/api\/v1\/theaters\/([^/]+)\/screens$/))) {
    if (method === "GET")
      return [
        200,
        page(
          state.screens.filter((s) => s.theaterId === match[1]),
          url,
        ),
      ];
    if (method === "POST") {
      const screen = { ...body, id: randomUUID(), theaterId: match[1] };
      state.screens.push(screen);
      return [201, screen];
    }
  }
  if (method === "POST" && path === "/api/v1/shows") {
    const show = {
      ...body,
      id: randomUUID(),
      status: "SCHEDULED",
      createdAt: now(),
      updatedAt: now(),
    };
    state.shows.push(show);
    return [201, show];
  }
  if ((match = path.match(/^\/api\/v1\/shows\/([^/]+)(\/cancel)?$/))) {
    const show = state.shows.find((s) => s.id === match[1]);
    if (!show) return [404, { detail: "Show not found" }];
    if (method === "GET" && !match[2]) return [200, show];
    if (method === "POST" && match[2]) return [200, Object.assign(show, { status: "CANCELLED" })];
  }

  return [404, { message: "No such route in the mock gateway" }];
}

createServer((request, response) => {
  const chunks = [];
  request.on("data", (chunk) => chunks.push(chunk));
  request.on("end", () => {
    const url = new URL(request.url, `http://${request.headers.host}`);
    const send = (status, body) => {
      response.writeHead(status, { "content-type": "application/json" });
      response.end(body === null ? undefined : JSON.stringify(body));
    };
    if (request.method === "POST" && url.pathname === "/__reset") {
      reset();
      return send(200, { ok: true });
    }
    if (url.pathname === "/__health") return send(200, { ok: true });
    const user = request.headers.authorization?.replace(/^Bearer /, "");
    if (!user) return send(401, { message: "Unauthorized" });
    let body = {};
    try {
      if (chunks.length > 0) body = JSON.parse(Buffer.concat(chunks).toString("utf8"));
    } catch {
      return send(400, { message: "Malformed JSON" });
    }
    const [status, payload] = route(request.method, url, body, user, request.headers);
    send(status, payload);
  });
}).listen(PORT, "127.0.0.1", () => console.log(`mock gateway on http://127.0.0.1:${PORT}`));
