-- coupang_seller_product 테이블에 수수료율 컬럼 추가
-- 등록상품 동기화 시 카테고리 기반으로 수수료율을 매핑하여 저장

ALTER TABLE coupang_seller_product
ADD COLUMN commission_rate DECIMAL(5,2);

-- 컬럼 설명
COMMENT ON COLUMN coupang_seller_product.commission_rate IS '카테고리 기반 수수료율 (%)';
