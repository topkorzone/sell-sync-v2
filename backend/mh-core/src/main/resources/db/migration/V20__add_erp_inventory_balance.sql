-- ERP 창고별 재고현황 테이블
CREATE TABLE erp_inventory_balance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    erp_config_id UUID NOT NULL REFERENCES tenant_erp_config(id) ON DELETE CASCADE,
    prod_cd VARCHAR(100) NOT NULL,
    wh_cd VARCHAR(50) NOT NULL,
    wh_des VARCHAR(255),
    bal_qty NUMERIC(15,2) NOT NULL DEFAULT 0,
    last_synced_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(tenant_id, erp_config_id, prod_cd, wh_cd)
);

-- 인덱스
CREATE INDEX idx_erp_inventory_tenant ON erp_inventory_balance(tenant_id);
CREATE INDEX idx_erp_inventory_prod_cd ON erp_inventory_balance(prod_cd);
CREATE INDEX idx_erp_inventory_config_prod ON erp_inventory_balance(erp_config_id, prod_cd);

-- RLS 정책
ALTER TABLE erp_inventory_balance ENABLE ROW LEVEL SECURITY;

CREATE POLICY erp_inventory_balance_tenant_isolation ON erp_inventory_balance
    USING (tenant_id = current_setting('app.current_tenant_id', true)::UUID);

-- 코멘트
COMMENT ON TABLE erp_inventory_balance IS 'ERP 창고별 재고현황';
COMMENT ON COLUMN erp_inventory_balance.prod_cd IS '품목코드';
COMMENT ON COLUMN erp_inventory_balance.wh_cd IS '창고코드';
COMMENT ON COLUMN erp_inventory_balance.wh_des IS '창고명';
COMMENT ON COLUMN erp_inventory_balance.bal_qty IS '재고수량';
