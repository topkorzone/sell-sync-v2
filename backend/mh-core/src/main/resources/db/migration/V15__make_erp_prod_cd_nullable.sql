-- erp_prod_cd를 nullable로 변경하여 미매핑 상태의 상품 등록 허용
ALTER TABLE product_mapping ALTER COLUMN erp_prod_cd DROP NOT NULL;

COMMENT ON COLUMN product_mapping.erp_prod_cd IS 'ERP 품목 코드 (null이면 미매핑 상태)';
