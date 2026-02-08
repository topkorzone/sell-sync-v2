-- ERP 필드 매핑 테이블
-- 테넌트별로 ECount API 필드를 동적으로 설정

CREATE TABLE erp_field_mapping (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    erp_config_id UUID NOT NULL,

    -- ECount 필드 정보
    field_name VARCHAR(50) NOT NULL,           -- ECount 변수명 (WH_CD, CUST, TTL_CTT 등)
    field_position VARCHAR(20) NOT NULL,       -- HEADER(상단) or LINE(하단)
    line_type VARCHAR(30),                     -- LINE일 경우: PRODUCT_SALE, DELIVERY_FEE, SALES_COMMISSION, DELIVERY_COMMISSION, ALL

    -- 값 설정
    value_type VARCHAR(20) NOT NULL,           -- FIXED, MARKETPLACE, ORDER_FIELD
    fixed_value VARCHAR(500),                  -- value_type=FIXED일 때 고정값
    marketplace_values JSONB,                  -- value_type=MARKETPLACE일 때 마켓별 값 {"COUPANG":"C01","NAVER":"C02"}
    order_field_template VARCHAR(500),         -- value_type=ORDER_FIELD일 때 템플릿 "{marketplaceName}-{orderId}"

    -- 메타
    display_order INT DEFAULT 0,
    description VARCHAR(200),
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_erp_field_mapping_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT fk_erp_field_mapping_erp_config FOREIGN KEY (erp_config_id) REFERENCES tenant_erp_config(id)
);

-- 인덱스
CREATE INDEX idx_erp_field_mapping_tenant ON erp_field_mapping(tenant_id);
CREATE INDEX idx_erp_field_mapping_config ON erp_field_mapping(erp_config_id);
CREATE INDEX idx_erp_field_mapping_active ON erp_field_mapping(tenant_id, erp_config_id, active);

-- RLS 정책
ALTER TABLE erp_field_mapping ENABLE ROW LEVEL SECURITY;

CREATE POLICY erp_field_mapping_tenant_isolation ON erp_field_mapping
    USING (tenant_id = current_setting('app.current_tenant_id', true)::UUID);

COMMENT ON TABLE erp_field_mapping IS 'ERP 필드 매핑 설정 - 테넌트별 ECount API 필드 동적 설정';
COMMENT ON COLUMN erp_field_mapping.field_name IS 'ECount API 변수명 (WH_CD, CUST, TTL_CTT, U_MEMO1 등)';
COMMENT ON COLUMN erp_field_mapping.field_position IS 'HEADER(전표 상단) 또는 LINE(전표 하단/라인)';
COMMENT ON COLUMN erp_field_mapping.line_type IS 'LINE 위치일 때 적용 대상: PRODUCT_SALE, DELIVERY_FEE, SALES_COMMISSION, DELIVERY_COMMISSION, ALL';
COMMENT ON COLUMN erp_field_mapping.value_type IS 'FIXED(고정값), MARKETPLACE(마켓별), ORDER_FIELD(주문정보 템플릿)';
COMMENT ON COLUMN erp_field_mapping.order_field_template IS '주문정보 템플릿. 사용가능: {orderId}, {productOrderId}, {marketplaceName}, {buyerName}, {receiverName}, {orderDate}';
