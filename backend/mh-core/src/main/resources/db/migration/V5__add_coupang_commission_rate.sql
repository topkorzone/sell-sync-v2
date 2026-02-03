-- V5: 쿠팡 수수료율 테이블 추가
CREATE TABLE coupang_commission_rate (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category_id VARCHAR(50) NOT NULL UNIQUE,
    category_name VARCHAR(255) NOT NULL,
    commission_rate NUMERIC(5, 2) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

COMMENT ON TABLE coupang_commission_rate IS '쿠팡 카테고리별 수수료율 테이블';
COMMENT ON COLUMN coupang_commission_rate.category_id IS '카테고리 ID (DEFAULT는 기본 수수료율)';
COMMENT ON COLUMN coupang_commission_rate.category_name IS '카테고리명';
COMMENT ON COLUMN coupang_commission_rate.commission_rate IS '수수료율 (%)';
COMMENT ON COLUMN coupang_commission_rate.effective_from IS '적용 시작일';
COMMENT ON COLUMN coupang_commission_rate.effective_to IS '적용 종료일 (NULL이면 현재 적용중)';

-- 기본 수수료율 데이터 (10.8%)
INSERT INTO coupang_commission_rate (category_id, category_name, commission_rate, effective_from)
VALUES ('DEFAULT', '기본 수수료율', 10.80, '2026-01-01');
