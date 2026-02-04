-- V10: 쿠팡 카테고리 테이블 추가
CREATE TABLE coupang_category (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    display_category_code BIGINT NOT NULL UNIQUE,
    display_category_name VARCHAR(255) NOT NULL,
    parent_category_code BIGINT,
    depth_level INT NOT NULL DEFAULT 1,
    root_category_code BIGINT,
    root_category_name VARCHAR(255),
    synced_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_coupang_category_parent ON coupang_category(parent_category_code);
CREATE INDEX idx_coupang_category_root ON coupang_category(root_category_code);
CREATE INDEX idx_coupang_category_depth ON coupang_category(depth_level);

COMMENT ON TABLE coupang_category IS '쿠팡 카테고리 테이블';
COMMENT ON COLUMN coupang_category.display_category_code IS '전시 카테고리 코드';
COMMENT ON COLUMN coupang_category.display_category_name IS '카테고리명';
COMMENT ON COLUMN coupang_category.parent_category_code IS '상위 카테고리 코드';
COMMENT ON COLUMN coupang_category.depth_level IS '카테고리 depth (1: 대분류, 2: 중분류, ...)';
COMMENT ON COLUMN coupang_category.root_category_code IS '최상위 대분류 카테고리 코드 (수수료 매핑용)';
COMMENT ON COLUMN coupang_category.root_category_name IS '최상위 대분류 카테고리명';

-- 수수료 테이블에 display_category_code 컬럼 추가 (대분류 코드 매핑용)
ALTER TABLE coupang_commission_rate ADD COLUMN display_category_code BIGINT;

COMMENT ON COLUMN coupang_commission_rate.display_category_code IS '전시 카테고리 코드 (대분류)';
