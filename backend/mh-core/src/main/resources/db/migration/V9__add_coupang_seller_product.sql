-- 쿠팡 등록상품 테이블: 쿠팡 마켓플레이스에 등록된 상품 정보 저장
CREATE TABLE coupang_seller_product (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id               UUID NOT NULL,
    seller_product_id       BIGINT NOT NULL,
    seller_product_name     VARCHAR(500),
    display_category_code   BIGINT,
    category_id             BIGINT,
    product_id              BIGINT,
    vendor_id               VARCHAR(50),
    sale_started_at         TIMESTAMP,
    sale_ended_at           TIMESTAMP,
    brand                   VARCHAR(255),
    status_name             VARCHAR(50),
    synced_at               TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 유니크 인덱스: 테넌트별 sellerProductId는 유니크
CREATE UNIQUE INDEX idx_coupang_seller_product_unique
    ON coupang_seller_product(tenant_id, seller_product_id);

-- 조회 최적화를 위한 인덱스
CREATE INDEX idx_coupang_seller_product_tenant ON coupang_seller_product(tenant_id);
CREATE INDEX idx_coupang_seller_product_name ON coupang_seller_product(tenant_id, seller_product_name);
CREATE INDEX idx_coupang_seller_product_status ON coupang_seller_product(tenant_id, status_name);
CREATE INDEX idx_coupang_seller_product_brand ON coupang_seller_product(tenant_id, brand);

-- RLS 정책 활성화
ALTER TABLE coupang_seller_product ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_coupang_seller_product ON coupang_seller_product
    USING (tenant_id::text = current_setting('app.current_tenant_id', true));

-- 코멘트
COMMENT ON TABLE coupang_seller_product IS '쿠팡 등록상품 마스터 테이블';
COMMENT ON COLUMN coupang_seller_product.tenant_id IS '테넌트 ID';
COMMENT ON COLUMN coupang_seller_product.seller_product_id IS '쿠팡 셀러 상품 ID';
COMMENT ON COLUMN coupang_seller_product.seller_product_name IS '상품명';
COMMENT ON COLUMN coupang_seller_product.display_category_code IS '전시 카테고리 코드';
COMMENT ON COLUMN coupang_seller_product.category_id IS '카테고리 ID';
COMMENT ON COLUMN coupang_seller_product.product_id IS '쿠팡 상품 ID';
COMMENT ON COLUMN coupang_seller_product.vendor_id IS '벤더 ID';
COMMENT ON COLUMN coupang_seller_product.sale_started_at IS '판매 시작일';
COMMENT ON COLUMN coupang_seller_product.sale_ended_at IS '판매 종료일';
COMMENT ON COLUMN coupang_seller_product.brand IS '브랜드명';
COMMENT ON COLUMN coupang_seller_product.status_name IS '상품 상태';
COMMENT ON COLUMN coupang_seller_product.synced_at IS '마지막 동기화 시간';
