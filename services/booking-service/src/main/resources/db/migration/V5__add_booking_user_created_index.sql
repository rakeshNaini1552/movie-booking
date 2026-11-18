-- Serves GET /api/v1/bookings: one user's bookings, newest first.
CREATE INDEX idx_booking_user_id_created_at ON booking (user_id, created_at DESC);
