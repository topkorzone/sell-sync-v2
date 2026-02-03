-- ERP 품목 마스터 데이터 테이블
CREATE TABLE erp_item (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL,
    erp_config_id       UUID NOT NULL REFERENCES tenant_erp_config(id) ON DELETE CASCADE,
    prod_cd             VARCHAR(100) NOT NULL,
    prod_des            VARCHAR(500) NOT NULL,
    size_des            VARCHAR(255),
    unit                VARCHAR(50),
    prod_type           VARCHAR(10),
    in_price            NUMERIC(15,2),
    out_price           NUMERIC(15,2),
    bar_code            VARCHAR(100),
    class_cd            VARCHAR(50),
    class_cd2           VARCHAR(50),
    class_cd3           VARCHAR(50),
    set_flag            BOOLEAN DEFAULT FALSE,
    bal_flag            BOOLEAN DEFAULT TRUE,
    last_synced_at      TIMESTAMP NOT NULL,
    raw_data            JSONB,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id, erp_config_id, prod_cd)
);

-- 인덱스 생성
CREATE INDEX idx_erp_item_tenant ON erp_item(tenant_id);
CREATE INDEX idx_erp_item_prod_cd ON erp_item(tenant_id, prod_cd);
CREATE INDEX idx_erp_item_bar_code ON erp_item(tenant_id, bar_code) WHERE bar_code IS NOT NULL;

-- Row Level Security 설정
ALTER TABLE erp_item ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_erp_item ON erp_item
    USING (tenant_id::text = current_setting('app.current_tenant_id', true));
