package com.mhub.core.erp.dto;

import com.mhub.core.domain.entity.ErpSalesDocument;
import com.mhub.core.domain.enums.ErpDocumentStatus;
import com.mhub.core.domain.enums.MarketplaceType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ErpSalesDocumentResponse(
        UUID id,
        UUID orderId,
        String marketplaceOrderId,
        ErpDocumentStatus status,
        LocalDate documentDate,
        MarketplaceType marketplaceType,
        String customerCode,
        String customerName,
        BigDecimal totalAmount,
        List<Map<String, Object>> documentLines,
        String erpDocumentId,
        Instant sentAt,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
    public static ErpSalesDocumentResponse from(ErpSalesDocument doc, String marketplaceOrderId) {
        return new ErpSalesDocumentResponse(
                doc.getId(),
                doc.getOrderId(),
                marketplaceOrderId,
                doc.getStatus(),
                doc.getDocumentDate(),
                doc.getMarketplaceType(),
                doc.getCustomerCode(),
                doc.getCustomerName(),
                doc.getTotalAmount(),
                doc.getDocumentLines(),
                doc.getErpDocumentId(),
                doc.getSentAt(),
                doc.getErrorMessage(),
                doc.getCreatedAt() != null ? doc.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null,
                doc.getUpdatedAt() != null ? doc.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant() : null
        );
    }
}
