-- Owner of the ticket (the booking's user), taken from BookingConfirmed.userId.
-- Nullable: tickets issued before this migration have no recorded owner and are not returned by the read endpoints.
ALTER TABLE ticket ADD COLUMN user_id uuid;
