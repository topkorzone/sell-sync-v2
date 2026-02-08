package com.mhub.erp.adapter;

import com.mhub.core.domain.entity.TenantErpConfig;
import com.mhub.core.domain.enums.ErpType;
import java.util.List;
import java.util.Map;

public interface ErpAdapter {
    ErpType getErpType();
    boolean testConnection(TenantErpConfig config);
    DocumentResult createSalesDocument(TenantErpConfig config, SalesDocumentRequest request);
    DocumentResult createJournalEntry(TenantErpConfig config, JournalEntryRequest request);
    DocumentStatus getDocumentStatus(TenantErpConfig config, String documentId);
    ItemFetchResult fetchItems(TenantErpConfig config);

    record SalesDocumentRequest(String date, String customerName, String productName, int quantity, java.math.BigDecimal unitPrice, java.math.BigDecimal totalAmount, Map<String, Object> extraFields) {}
    record JournalEntryRequest(String date, String description, java.math.BigDecimal debitAmount, java.math.BigDecimal creditAmount, String debitAccount, String creditAccount, Map<String, Object> extraFields) {}
    record DocumentResult(boolean success, String documentId, String errorMessage, Map<String, Object> responseData) {}
    record DocumentStatus(String documentId, String status, Map<String, Object> details) {}
    record ItemFetchResult(boolean success, List<Map<String, Object>> items, String errorMessage, int totalCount) {}
    record InventoryFetchResult(boolean success, List<Map<String, Object>> items, String errorMessage) {}

    default InventoryFetchResult fetchInventoryBalance(TenantErpConfig config, String baseDate, List<String> prodCds) {
        return new InventoryFetchResult(false, List.of(), "지원하지 않는 ERP 유형입니다");
    }
}
