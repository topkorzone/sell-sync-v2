-- 글로벌 필드 매핑 추가: 모든 라인에 공통 적용되는 ECount 필드 매핑
ALTER TABLE erp_sales_template
ADD COLUMN IF NOT EXISTS global_field_mappings JSONB DEFAULT '[]';

COMMENT ON COLUMN erp_sales_template.global_field_mappings IS '글로벌 필드 매핑 - 모든 라인에 적용되는 ECount 추가 필드 설정';
