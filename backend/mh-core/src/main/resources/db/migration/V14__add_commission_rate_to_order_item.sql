-- OrderItem 테이블에 수수료율 및 정산예정금액 컬럼 추가
ALTER TABLE order_item ADD COLUMN IF NOT EXISTS commission_rate DECIMAL(5,2);
ALTER TABLE order_item ADD COLUMN IF NOT EXISTS expected_settlement_amount DECIMAL(15,2);

COMMENT ON COLUMN order_item.commission_rate IS '수수료율 (%)';
COMMENT ON COLUMN order_item.expected_settlement_amount IS '상품별 정산예정금액';
