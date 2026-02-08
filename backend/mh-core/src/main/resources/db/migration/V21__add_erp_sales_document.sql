-- ERP 판매전표 테이블
-- 출고 완료 시 자동 생성 → 관리 화면에서 검토 → ERP 전송
CREATE TABLE erp_sales_document (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    order_id UUID NOT NULL,
    erp_config_id UUID NOT NULL REFERENCES tenant_erp_config(id),

    -- 전표 상태: PENDING(미전송), SENT(전송완료), FAILED(전송실패), CANCELLED(취소)
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    -- 전표 내용
    document_date DATE NOT NULL,
    marketplace_type VARCHAR(30) NOT NULL,
    customer_code VARCHAR(50),
    customer_name VARCHAR(100),
    total_amount DECIMAL(15,2) NOT NULL,
    document_lines JSONB NOT NULL,

    -- ERP 전송 결과
    erp_document_id VARCHAR(100),
    sent_at TIMESTAMP,
    error_message TEXT,

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 인덱스
CREATE INDEX idx_erp_sales_document_tenant_status ON erp_sales_document(tenant_id, status);
CREATE INDEX idx_erp_sales_document_order ON erp_sales_document(order_id);
CREATE INDEX idx_erp_sales_document_created ON erp_sales_document(tenant_id, created_at DESC);

-- PENDING/SENT/FAILED 상태의 전표는 주문당 1개만 (CANCELLED 제외)
CREATE UNIQUE INDEX idx_erp_sales_document_order_unique
    ON erp_sales_document(order_id)
    WHERE status != 'CANCELLED';

-- RLS 정책
ALTER TABLE erp_sales_document ENABLE ROW LEVEL SECURITY;

CREATE POLICY erp_sales_document_tenant_isolation ON erp_sales_document
    FOR ALL
    USING (tenant_id::text = current_setting('app.current_tenant_id', true));
