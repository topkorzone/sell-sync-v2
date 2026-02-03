-- V4: 주문에 정산예정금 컬럼 추가
ALTER TABLE orders ADD COLUMN expected_settlement_amount NUMERIC(15, 2);

COMMENT ON COLUMN orders.expected_settlement_amount IS '정산예정금 (마켓플레이스 수수료 차감 후 예상 정산 금액)';
