-- 자동 전표생성 및 자동 ERP 전송 설정 추가
ALTER TABLE tenant_erp_config
    ADD COLUMN IF NOT EXISTS auto_generate_document BOOLEAN DEFAULT false,
    ADD COLUMN IF NOT EXISTS auto_send_to_erp BOOLEAN DEFAULT false;

COMMENT ON COLUMN tenant_erp_config.auto_generate_document IS '자동 전표생성 활성화 여부';
COMMENT ON COLUMN tenant_erp_config.auto_send_to_erp IS '자동 ERP전송 활성화 여부';
