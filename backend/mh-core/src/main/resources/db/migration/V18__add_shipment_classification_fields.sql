-- V18: Add CJ classification fields to shipment table for standard waybill printing

ALTER TABLE shipment
  ADD COLUMN classification_code VARCHAR(10),
  ADD COLUMN sub_classification_code VARCHAR(10),
  ADD COLUMN address_alias VARCHAR(100),
  ADD COLUMN delivery_branch_name VARCHAR(100),
  ADD COLUMN delivery_employee_nickname VARCHAR(50),
  ADD COLUMN receipt_date DATE,
  ADD COLUMN delivery_message VARCHAR(500);

COMMENT ON COLUMN shipment.classification_code IS 'CJ 분류코드 대분류 (4자리)';
COMMENT ON COLUMN shipment.sub_classification_code IS 'CJ 서브분류코드';
COMMENT ON COLUMN shipment.address_alias IS 'CJ 주소약칭';
COMMENT ON COLUMN shipment.delivery_branch_name IS 'CJ 배달점소';
COMMENT ON COLUMN shipment.delivery_employee_nickname IS 'CJ 배송사원 별칭';
COMMENT ON COLUMN shipment.receipt_date IS '접수일자';
COMMENT ON COLUMN shipment.delivery_message IS '배송메시지';

CREATE INDEX idx_shipment_classification ON shipment(classification_code);
