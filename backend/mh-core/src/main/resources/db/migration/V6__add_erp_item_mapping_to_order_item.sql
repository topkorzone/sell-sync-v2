-- OrderItem 테이블에 ERP 품목 매핑 컬럼 추가
ALTER TABLE order_item ADD COLUMN erp_item_id UUID;
ALTER TABLE order_item ADD COLUMN erp_prod_cd VARCHAR(100);

-- erp_item 테이블과의 외래키 관계 추가
ALTER TABLE order_item ADD CONSTRAINT fk_order_item_erp_item
    FOREIGN KEY (erp_item_id) REFERENCES erp_item(id) ON DELETE SET NULL;

-- 인덱스 추가
CREATE INDEX idx_order_item_erp_item_id ON order_item(erp_item_id);
CREATE INDEX idx_order_item_erp_prod_cd ON order_item(erp_prod_cd);

COMMENT ON COLUMN order_item.erp_item_id IS 'ERP 품목 ID (erp_item 테이블 참조)';
COMMENT ON COLUMN order_item.erp_prod_cd IS 'ERP 품목 코드 (빠른 조회용 비정규화)';
