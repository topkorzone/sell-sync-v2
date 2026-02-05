-- ============================================================
-- ORDER SETTLEMENT (건별 정산 데이터)
-- ============================================================
CREATE TABLE order_settlement (
    id                              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id                       UUID NOT NULL,
    marketplace_type                VARCHAR(30) NOT NULL,
    marketplace_order_id            VARCHAR(255) NOT NULL,
    marketplace_product_order_id    VARCHAR(255),
    order_id                        UUID,
    settle_type                     VARCHAR(50),
    settle_basis_date               DATE,
    settle_expect_date              DATE,
    settle_complete_date            DATE,
    pay_date                        DATE,
    product_id                      VARCHAR(255),
    product_name                    VARCHAR(500),
    vendor_item_id                  VARCHAR(255),
    sale_amount                     NUMERIC(15,2),
    commission_amount               NUMERIC(15,2),
    delivery_fee_amount             NUMERIC(15,2),
    delivery_fee_commission         NUMERIC(15,2),
    settlement_amount               NUMERIC(15,2),
    discount_amount                 NUMERIC(15,2),
    seller_discount_amount          NUMERIC(15,2),
    raw_data                        JSONB,
    created_at                      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 동일 건 중복 방지 UNIQUE 인덱스
CREATE UNIQUE INDEX idx_order_settlement_unique
    ON order_settlement(tenant_id, marketplace_type, marketplace_order_id,
                        COALESCE(marketplace_product_order_id, ''),
                        COALESCE(settle_basis_date, '1970-01-01'));

-- 조회 인덱스
CREATE INDEX idx_order_settlement_tenant_mkt ON order_settlement(tenant_id, marketplace_type);
CREATE INDEX idx_order_settlement_order ON order_settlement(order_id) WHERE order_id IS NOT NULL;
CREATE INDEX idx_order_settlement_basis_date ON order_settlement(tenant_id, settle_basis_date);

-- RLS 정책
ALTER TABLE order_settlement ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_order_settlement ON order_settlement
    USING (tenant_id::text = current_setting('app.current_tenant_id', true));

-- ============================================================
-- orders 테이블에 settlement_collected 컬럼 추가
-- ============================================================
ALTER TABLE orders ADD COLUMN settlement_collected BOOLEAN NOT NULL DEFAULT FALSE;
