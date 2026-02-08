package com.mhub.core.domain.entity;

import com.mhub.core.domain.enums.ErpDocumentStatus;
import com.mhub.core.domain.enums.MarketplaceType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "erp_sales_document")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ErpSalesDocument extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "erp_config_id", nullable = false)
    private UUID erpConfigId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ErpDocumentStatus status = ErpDocumentStatus.PENDING;

    @Column(name = "document_date", nullable = false)
    private LocalDate documentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "marketplace_type", nullable = false)
    private MarketplaceType marketplaceType;

    @Column(name = "customer_code")
    private String customerCode;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Type(JsonType.class)
    @Column(name = "document_lines", columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> documentLines;

    @Column(name = "erp_document_id")
    private String erpDocumentId;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 전표 전송 성공 처리
     */
    public void markAsSent(String documentId) {
        this.status = ErpDocumentStatus.SENT;
        this.erpDocumentId = documentId;
        this.sentAt = Instant.now();
        this.errorMessage = null;
    }

    /**
     * 전표 전송 실패 처리
     */
    public void markAsFailed(String errorMessage) {
        this.status = ErpDocumentStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    /**
     * 전표 취소 처리
     */
    public void cancel() {
        this.status = ErpDocumentStatus.CANCELLED;
    }

    /**
     * 재전송 가능 여부
     */
    public boolean canRetry() {
        return status == ErpDocumentStatus.PENDING || status == ErpDocumentStatus.FAILED;
    }

    /**
     * 삭제(취소) 가능 여부
     */
    public boolean canCancel() {
        return status == ErpDocumentStatus.PENDING || status == ErpDocumentStatus.FAILED;
    }
}
