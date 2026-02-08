-- ============================================================
-- ERP SALES TEMPLATE (판매전표 자동등록 템플릿)
-- ============================================================
CREATE TABLE erp_sales_template (
    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id                   UUID NOT NULL,
    erp_config_id               UUID NOT NULL REFERENCES tenant_erp_config(id) ON DELETE CASCADE,
    marketplace_headers         JSONB NOT NULL DEFAULT '{}',
    default_header              JSONB NOT NULL DEFAULT '{}',
    line_product_sale           JSONB NOT NULL DEFAULT '{}',
    line_delivery_fee           JSONB NOT NULL DEFAULT '{}',
    line_sales_commission       JSONB NOT NULL DEFAULT '{}',
    line_delivery_commission    JSONB NOT NULL DEFAULT '{}',
    active                      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id, erp_config_id)
);

CREATE INDEX idx_erp_sales_template_tenant ON erp_sales_template(tenant_id);
CREATE INDEX idx_erp_sales_template_config ON erp_sales_template(erp_config_id);

-- RLS 정책
ALTER TABLE erp_sales_template ENABLE ROW LEVEL SECURITY;

CREATE POLICY erp_sales_template_tenant_isolation ON erp_sales_template
    USING (tenant_id = current_setting('app.current_tenant_id', true)::uuid);

-- ERP 전송 상태를 주문에 추가 (erp_document_id)
ALTER TABLE orders ADD COLUMN IF NOT EXISTS erp_document_id VARCHAR(255);
