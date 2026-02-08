-- 상품 매핑에 ERP 창고코드 추가
ALTER TABLE product_mapping ADD COLUMN IF NOT EXISTS erp_wh_cd VARCHAR(10);

-- 주문 아이템에 ERP 창고코드 추가
ALTER TABLE order_item ADD COLUMN IF NOT EXISTS erp_wh_cd VARCHAR(10);

COMMENT ON COLUMN product_mapping.erp_wh_cd IS 'ERP 출하창고 코드';
COMMENT ON COLUMN order_item.erp_wh_cd IS 'ERP 출하창고 코드 (매핑에서 복사)';
