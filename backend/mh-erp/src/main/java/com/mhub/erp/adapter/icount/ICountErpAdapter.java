package com.mhub.erp.adapter.icount;

import com.mhub.core.domain.entity.TenantErpConfig;
import com.mhub.core.domain.enums.ErpType;
import com.mhub.erp.adapter.ErpAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Map;

@Slf4j @Component
public class ICountErpAdapter implements ErpAdapter {
    @Override public ErpType getErpType() { return ErpType.ICOUNT; }
    @Override public DocumentResult createSalesDocument(TenantErpConfig config, SalesDocumentRequest req) { log.info("iCount sales doc for {}", req.date()); return new DocumentResult(false, null, "iCount API not yet implemented", Map.of()); }
    @Override public DocumentResult createJournalEntry(TenantErpConfig config, JournalEntryRequest req) { log.info("iCount journal for {}", req.date()); return new DocumentResult(false, null, "iCount API not yet implemented", Map.of()); }
    @Override public DocumentStatus getDocumentStatus(TenantErpConfig config, String docId) { log.info("iCount status for {}", docId); return new DocumentStatus(docId, "UNKNOWN", Map.of()); }
}
