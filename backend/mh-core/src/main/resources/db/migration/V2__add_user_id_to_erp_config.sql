-- ============================================================
-- ADD user_id COLUMN TO tenant_erp_config
-- Required for ECount ERP authentication
-- ============================================================
ALTER TABLE tenant_erp_config
    ADD COLUMN IF NOT EXISTS user_id VARCHAR(100);

-- Add comment for documentation
COMMENT ON COLUMN tenant_erp_config.user_id IS 'ERP user ID (required for ECount authentication)';
