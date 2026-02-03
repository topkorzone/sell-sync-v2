-- 상품 매핑 테이블: 마켓플레이스 상품과 ERP 품목 간 매핑 관리
CREATE TABLE product_mapping (
    id                       UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id                UUID NOT NULL,
    marketplace_type         VARCHAR(30) NOT NULL,
    marketplace_product_id   VARCHAR(255) NOT NULL,
    marketplace_sku          VARCHAR(255),
    marketplace_product_name VARCHAR(500),
    marketplace_option_name  VARCHAR(500),
    erp_item_id              UUID REFERENCES erp_item(id) ON DELETE SET NULL,
    erp_prod_cd              VARCHAR(100) NOT NULL,
    auto_created             BOOLEAN NOT NULL DEFAULT FALSE,
    use_count                INTEGER NOT NULL DEFAULT 0,
    last_used_at             TIMESTAMP,
    created_at               TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 유니크 인덱스 (SKU가 NULL인 경우도 처리)
CREATE UNIQUE INDEX idx_product_mapping_unique
    ON product_mapping(tenant_id, marketplace_type, marketplace_product_id, COALESCE(marketplace_sku, ''));

-- 조회 최적화를 위한 인덱스
CREATE INDEX idx_product_mapping_lookup ON product_mapping(tenant_id, marketplace_type, marketplace_product_id);
CREATE INDEX idx_product_mapping_tenant ON product_mapping(tenant_id);
CREATE INDEX idx_product_mapping_erp_prod_cd ON product_mapping(tenant_id, erp_prod_cd);
CREATE INDEX idx_product_mapping_use_count ON product_mapping(tenant_id, use_count DESC);

-- RLS 정책 활성화
ALTER TABLE product_mapping ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_product_mapping ON product_mapping
    USING (tenant_id::text = current_setting('app.current_tenant_id', true));

-- 코멘트
COMMENT ON TABLE product_mapping IS '마켓플레이스 상품 - ERP 품목 매핑 마스터 테이블';
COMMENT ON COLUMN product_mapping.tenant_id IS '테넌트 ID';
COMMENT ON COLUMN product_mapping.marketplace_type IS '마켓플레이스 유형 (COUPANG, NAVER 등)';
COMMENT ON COLUMN product_mapping.marketplace_product_id IS '마켓플레이스 상품 ID';
COMMENT ON COLUMN product_mapping.marketplace_sku IS '마켓플레이스 SKU/옵션코드 (옵션별 매핑용)';
COMMENT ON COLUMN product_mapping.marketplace_product_name IS '마켓플레이스 상품명 (참조용)';
COMMENT ON COLUMN product_mapping.marketplace_option_name IS '마켓플레이스 옵션명 (참조용)';
COMMENT ON COLUMN product_mapping.erp_item_id IS 'ERP 품목 ID (erp_item 테이블 참조)';
COMMENT ON COLUMN product_mapping.erp_prod_cd IS 'ERP 품목 코드';
COMMENT ON COLUMN product_mapping.auto_created IS '자동 생성 여부 (주문 매핑 시 자동 저장된 경우 true)';
COMMENT ON COLUMN product_mapping.use_count IS '매핑 사용 횟수';
COMMENT ON COLUMN product_mapping.last_used_at IS '마지막 사용 시간';
