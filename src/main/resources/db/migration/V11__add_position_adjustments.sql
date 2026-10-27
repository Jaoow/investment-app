ALTER TABLE portfolio ADD COLUMN position_revision BIGINT NOT NULL DEFAULT 0;
ALTER TABLE asset_movement ADD COLUMN adjustment_quantity NUMERIC(38, 8);
ALTER TABLE asset_movement ADD COLUMN adjustment_total_cost NUMERIC(38, 8);
ALTER TABLE asset_movement ADD COLUMN previous_quantity NUMERIC(38, 8);
ALTER TABLE asset_movement ADD COLUMN previous_total_cost NUMERIC(64, 16);
ALTER TABLE asset_movement ADD COLUMN adjustment_reason VARCHAR(500);
ALTER TABLE asset_movement ADD COLUMN adjustment_author VARCHAR(255);
ALTER TABLE asset_movement ADD COLUMN adjustment_recorded_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE asset_movement ADD COLUMN adjustment_request_id UUID;
ALTER TABLE asset_movement ADD COLUMN adjustment_expected_revision BIGINT;
ALTER TABLE asset_movement ADD COLUMN adjustment_result_revision BIGINT;
ALTER TABLE asset_movement ADD CONSTRAINT uk_position_adjustment_request
    UNIQUE (portfolio_id, adjustment_request_id);
ALTER TABLE asset_movement ADD CONSTRAINT ck_position_adjustment
    CHECK (type <> 'POSITION_ADJUSTMENT' OR (
        portfolio_id IS NOT NULL AND ticker_symbol IS NOT NULL AND date IS NOT NULL
        AND quantity IS NULL AND price IS NULL AND operation_cost IS NULL AND external_reference IS NULL
        AND adjustment_quantity IS NOT NULL AND adjustment_quantity >= 0
        AND adjustment_total_cost IS NOT NULL AND adjustment_total_cost >= 0
        AND (adjustment_quantity > 0 OR adjustment_total_cost = 0)
        AND previous_quantity IS NOT NULL AND previous_total_cost IS NOT NULL
        AND adjustment_reason IS NOT NULL AND adjustment_author IS NOT NULL
        AND adjustment_recorded_at IS NOT NULL AND adjustment_request_id IS NOT NULL
        AND adjustment_expected_revision IS NOT NULL AND adjustment_result_revision IS NOT NULL
    ));
