-- Each agency's own discount and cancellation rules (AgencyPolicy), starting from the platform defaults:
-- kids chairs 50% off; full refund more than 14 days before the tour; 50% from 7 days; nothing after that.
ALTER TABLE agencies ADD COLUMN IF NOT EXISTS kids_discount_percent integer NOT NULL DEFAULT 50;
ALTER TABLE agencies ADD COLUMN IF NOT EXISTS full_refund_days integer NOT NULL DEFAULT 14;
ALTER TABLE agencies ADD COLUMN IF NOT EXISTS partial_refund_days integer NOT NULL DEFAULT 7;
ALTER TABLE agencies ADD COLUMN IF NOT EXISTS partial_refund_percent integer NOT NULL DEFAULT 50;

ALTER TABLE agencies ADD CONSTRAINT agencies_kids_discount_percent_check CHECK (kids_discount_percent BETWEEN 0 AND 100);
ALTER TABLE agencies ADD CONSTRAINT agencies_partial_refund_percent_check CHECK (partial_refund_percent BETWEEN 0 AND 100);
ALTER TABLE agencies ADD CONSTRAINT agencies_refund_days_check
    CHECK (partial_refund_days >= 0 AND full_refund_days >= partial_refund_days);
