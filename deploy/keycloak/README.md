# Keycloak realm exports

- `movie-booking-realm.json`: the `movie-booking` realm (local).
- `movie-booking-stage-realm.json`: the `movie-booking-stage` realm (stage).

Both are **partial exports**: realm settings, roles and clients only. They contain no users (apart from the
service-account user), no passwords and no real client secrets. Test users are not included; create them by hand.

## Import

`docker-compose.yml` mounts this folder read-only into `/opt/keycloak/data/import` and starts Keycloak with
`--import-realm`. Import only runs when a realm does not exist yet; an existing realm is never overwritten. To pick up
a changed file you have to remove that realm in Keycloak first (or start from a fresh data volume).

## Secret placeholders

The `booking-service` client's `secret` is a placeholder that Keycloak substitutes from its environment on import:

| File | Placeholder |
|---|---|
| `movie-booking-realm.json` | `${BOOKING_SERVICE_CLIENT_SECRET}` |
| `movie-booking-stage-realm.json` | `${BOOKING_SERVICE_CLIENT_SECRET_STAGE}` |

The values come from `deploy/.env` (names are listed in `deploy/.env.example`). They are never committed. The
booking-service app reads the same value through `BOOKING_SERVICE_CLIENT_SECRET`.

## Re-exporting

Realm settings > Action > Partial export, with groups and roles and clients included. Keycloak masks the client secret
as `**********` in the export, so re-apply the placeholder edit on the `booking-service` client afterwards.

## Dev-only settings to review before any real deployment

- `booking-service` client `redirectUris` is `/*`.
- `movie-booking-app` has `directAccessGrantsEnabled` turned on.
