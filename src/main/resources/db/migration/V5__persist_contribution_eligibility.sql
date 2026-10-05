ALTER TABLE portfolio_asset_preference
    ALTER COLUMN price_ceiling DROP NOT NULL;

ALTER TABLE portfolio_asset_preference
    ADD COLUMN contribution_enabled BOOLEAN NOT NULL DEFAULT TRUE;