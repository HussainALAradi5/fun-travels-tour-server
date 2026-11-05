-- Replaces the old startup SchemaMaintenance runner and adds indexes.

-- Columns whose fields were removed from the entities.
ALTER TABLE IF EXISTS tours DROP COLUMN IF EXISTS price;
ALTER TABLE IF EXISTS transportations DROP COLUMN IF EXISTS remaining_seats;

-- A seat can be booked again after a cancellation, so tickets.seat_id is no longer unique.
DO $$
DECLARE constraint_row record;
BEGIN
    FOR constraint_row IN
        SELECT tc.constraint_name
        FROM information_schema.table_constraints tc
        JOIN information_schema.constraint_column_usage ccu
          ON tc.constraint_name = ccu.constraint_name AND tc.table_schema = ccu.table_schema
        WHERE tc.table_name = 'tickets' AND tc.constraint_type = 'UNIQUE' AND ccu.column_name = 'seat_id'
    LOOP
        EXECUTE format('ALTER TABLE tickets DROP CONSTRAINT %I', constraint_row.constraint_name);
    END LOOP;
END $$;

-- PostgreSQL does not index foreign keys on its own. Audit columns (created_by_id, updated_by_id) are left out:
-- nothing filters on them.
CREATE INDEX IF NOT EXISTS idx_tickets_seat_id ON tickets (seat_id);
CREATE INDEX IF NOT EXISTS idx_seats_transportation_id ON seats (transportation_id);
CREATE INDEX IF NOT EXISTS idx_transactions_account_id ON transactions (account_id);
CREATE INDEX IF NOT EXISTS idx_ticket_meals_selection_ticket_id ON ticket_meals_selection (ticket_id);
CREATE INDEX IF NOT EXISTS idx_ports_city_id ON ports (city_id);
CREATE INDEX IF NOT EXISTS idx_transportations_agency_id ON transportations (agency_id);
CREATE INDEX IF NOT EXISTS idx_agency_branch_city_id ON agency_branch (city_id);
CREATE INDEX IF NOT EXISTS idx_tickets_reservation_id ON tickets (reservation_id);
CREATE INDEX IF NOT EXISTS idx_tours_branch_id ON tours (branch_id);
CREATE INDEX IF NOT EXISTS idx_cities_country_id ON cities (country_id);
CREATE INDEX IF NOT EXISTS idx_tour_available_meals_meal_plan_id ON tour_available_meals (meal_plan_id);
CREATE INDEX IF NOT EXISTS idx_users_agency_id ON users (agency_id);
CREATE INDEX IF NOT EXISTS idx_agency_branch_country_id ON agency_branch (country_id);
CREATE INDEX IF NOT EXISTS idx_tours_transportation_id ON tours (transportation_id);
CREATE INDEX IF NOT EXISTS idx_ticket_meals_selection_meal_plan_id ON ticket_meals_selection (meal_plan_id);
CREATE INDEX IF NOT EXISTS idx_tours_end_country_id ON tours (end_country_id);
CREATE INDEX IF NOT EXISTS idx_transactions_reservation_id ON transactions (reservation_id);
CREATE INDEX IF NOT EXISTS idx_tours_agency_id ON tours (agency_id);
CREATE INDEX IF NOT EXISTS idx_agency_branch_manager_id ON agency_branch (manager_id);
CREATE INDEX IF NOT EXISTS idx_tickets_customer_id ON tickets (customer_id);
CREATE INDEX IF NOT EXISTS idx_media_owner_id ON media (owner_id);
CREATE INDEX IF NOT EXISTS idx_transportations_agency_branch_id ON transportations (agency_branch_id);
CREATE INDEX IF NOT EXISTS idx_user_requests_solved_by_id ON user_requests (solved_by_id);
CREATE INDEX IF NOT EXISTS idx_tours_end_city_id ON tours (end_city_id);
CREATE INDEX IF NOT EXISTS idx_tour_reservations_tour_id ON tour_reservations (tour_id);
CREATE INDEX IF NOT EXISTS idx_agency_branch_agency_id ON agency_branch (agency_id);
CREATE INDEX IF NOT EXISTS idx_users_agency_branch_id ON users (agency_branch_id);
CREATE INDEX IF NOT EXISTS idx_agencies_city_id ON agencies (city_id);
CREATE INDEX IF NOT EXISTS idx_user_requests_assigned_to_id ON user_requests (assigned_to_id);
CREATE INDEX IF NOT EXISTS idx_payments_reservation_id ON payments (reservation_id);
CREATE INDEX IF NOT EXISTS idx_tours_start_city_id ON tours (start_city_id);
CREATE INDEX IF NOT EXISTS idx_tour_available_meals_tour_id ON tour_available_meals (tour_id);
CREATE INDEX IF NOT EXISTS idx_generic_event_logs_actor_id ON generic_event_logs (actor_id);
CREATE INDEX IF NOT EXISTS idx_transactions_payment_id ON transactions (payment_id);
CREATE INDEX IF NOT EXISTS idx_tickets_tour_id ON tickets (tour_id);
CREATE INDEX IF NOT EXISTS idx_agencies_country_id ON agencies (country_id);
CREATE INDEX IF NOT EXISTS idx_tour_destinations_country_id ON tour_destinations (country_id);
CREATE INDEX IF NOT EXISTS idx_tours_start_country_id ON tours (start_country_id);
CREATE INDEX IF NOT EXISTS idx_ports_country_id ON ports (country_id);
CREATE INDEX IF NOT EXISTS idx_tour_reservations_user_id ON tour_reservations (user_id);
CREATE INDEX IF NOT EXISTS idx_user_requests_user_id ON user_requests (user_id);
CREATE INDEX IF NOT EXISTS idx_meal_plans_agency_id ON meal_plans (agency_id);
CREATE INDEX IF NOT EXISTS idx_agencies_owner_id ON agencies (owner_id);
CREATE INDEX IF NOT EXISTS idx_tour_destinations_tour_id ON tour_destinations (tour_id);
CREATE INDEX IF NOT EXISTS idx_generic_comments_author_id ON generic_comments (author_id);
CREATE INDEX IF NOT EXISTS idx_tours_status_start_date ON tours (status, start_date);
CREATE INDEX IF NOT EXISTS idx_tickets_ticket_status ON tickets (ticket_status);
CREATE INDEX IF NOT EXISTS idx_tour_reservations_status_hold_expires_at ON tour_reservations (status, hold_expires_at);
CREATE INDEX IF NOT EXISTS idx_notifications_user_id_is_read ON notifications (user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_media_category_created_at ON media (category, created_at);
CREATE INDEX IF NOT EXISTS idx_user_requests_status ON user_requests (status);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments (status);
CREATE INDEX IF NOT EXISTS idx_transportations_unit_status ON transportations (unit_status);
CREATE INDEX IF NOT EXISTS idx_users_user_type ON users (user_type);
