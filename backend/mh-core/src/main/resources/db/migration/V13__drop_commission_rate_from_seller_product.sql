-- V13: coupang_seller_product 테이블에서 commission_rate 컬럼 삭제
-- 수수료율은 주문 수집 시점에 카테고리 기반으로 실시간 조회하도록 롤백

ALTER TABLE coupang_seller_product
DROP COLUMN IF EXISTS commission_rate;
