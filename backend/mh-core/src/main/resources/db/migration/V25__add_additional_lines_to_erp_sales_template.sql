-- 전표 템플릿에 추가 항목 라인 필드 추가
ALTER TABLE erp_sales_template
ADD COLUMN IF NOT EXISTS additional_lines jsonb DEFAULT '[]'::jsonb;

COMMENT ON COLUMN erp_sales_template.additional_lines IS '추가 항목 라인 템플릿 (전표 생성 시 자동 포함)';
