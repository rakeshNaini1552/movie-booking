#!/usr/bin/env bash
# Creates or updates the Kubernetes Secrets the services read via envFrom (safe to re-run).
# Usage: create-secrets.sh [namespace] [override-env-file]
#   namespace          defaults to movie-booking
#   override-env-file  optional; its values replace the ones from deploy/.env (e.g. deploy/.env.stage)
# Values come from deploy/.env (kept out of git); nothing is printed or stored in this file.
set -euo pipefail

NAMESPACE="${1:-movie-booking}"
OVERRIDE_FILE="${2:-}"
ENV_FILE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/.env"

[[ -f "$ENV_FILE" ]] || { echo "Missing $ENV_FILE" >&2; exit 1; }
set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
if [[ -n "$OVERRIDE_FILE" ]]; then
  [[ -f "$OVERRIDE_FILE" ]] || { echo "Missing $OVERRIDE_FILE" >&2; exit 1; }
  # shellcheck disable=SC1090
  source "$OVERRIDE_FILE"
fi
set +a

# create_secret <secret-name> <ENV_VAR>...
# Values are piped through stdin as an env-file so they never appear in argv or output.
create_secret() {
  local name=$1; shift
  local var
  for var in "$@"; do
    [[ -n "${!var:-}" ]] || { echo "Missing $var in $ENV_FILE or the override file (needed by $name)" >&2; exit 1; }
  done
  for var in "$@"; do
    printf '%s=%s\n' "$var" "${!var}"
  done | kubectl create secret generic "$name" --namespace "$NAMESPACE" --from-env-file=/dev/stdin \
      --dry-run=client -o yaml \
    | kubectl apply -f - >/dev/null
  echo "applied secret/$name (keys: $*)"
}

create_secret payment-service-db PAYMENT_DB_USER PAYMENT_DB_PASSWORD
create_secret seat-service-db    SEAT_DB_USER SEAT_DB_PASSWORD
create_secret booking-service-db BOOKING_DB_USER BOOKING_DB_PASSWORD BOOKING_SERVICE_CLIENT_SECRET
create_secret ticket-service-db  TICKET_DB_USER TICKET_DB_PASSWORD TICKET_QR_SECRET
create_secret catalog-service-db CATALOG_DB_USER CATALOG_DB_PASSWORD
