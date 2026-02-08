package com.mhub.erp.adapter.ecount;

import com.mhub.core.domain.entity.ErpFieldMapping;
import com.mhub.core.domain.entity.ErpSalesTemplate;
import com.mhub.core.domain.entity.Order;
import com.mhub.core.domain.entity.OrderItem;
import com.mhub.core.domain.entity.OrderSettlement;
import com.mhub.core.domain.enums.ErpFieldPosition;
import com.mhub.core.domain.enums.ErpLineType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ECountSalesDocumentBuilder {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ErpFieldResolver fieldResolver;

    /**
     * 주문(Order) + 정산(OrderSettlement) + 템플릿(ErpSalesTemplate) → ECount SaveSale API 요청 body 변환
     * 1주문 = 1전표 (UPLOAD_SER_NO로 묶임)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> buildSaveSaleRequest(Order order, List<OrderSettlement> settlements, ErpSalesTemplate template) {
        String ioDate = order.getOrderedAt() != null
                ? order.getOrderedAt().format(DATE_FMT)
                : DATE_FMT.format(java.time.LocalDate.now());

        String uploadSerNo = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 헤더: defaultHeader + marketplaceHeaders[marketplaceType] 병합
        Map<String, Object> header = new LinkedHashMap<>();
        if (template.getDefaultHeader() != null) {
            header.putAll(template.getDefaultHeader());
        }
        String mktKey = order.getMarketplaceType().name();
        Map<String, Object> mktHeaders = template.getMarketplaceHeaders();
        if (mktHeaders != null && mktHeaders.containsKey(mktKey)) {
            Object mktVal = mktHeaders.get(mktKey);
            if (mktVal instanceof Map) {
                header.putAll((Map<String, Object>) mktVal);
            }
        }

        List<Map<String, Object>> lines = new ArrayList<>();
        int lineNo = 1;

        // 1) 상품판매 라인: OrderItem별 개별 라인
        Map<String, Object> prodTemplate = template.getLineProductSale();
        for (OrderItem item : order.getItems()) {
            Map<String, Object> line = buildBaseLine(header, ioDate, uploadSerNo, lineNo++);

            // PROD_CD: OrderItem.erpProdCd (ProductMapping에서 매핑된 코드)
            String prodCd = item.getErpProdCd();
            if (prodCd == null || prodCd.isBlank()) {
                prodCd = getStringField(prodTemplate, "prodCd", "");
            }
            line.put("PROD_CD", prodCd);

            // PROD_DES
            String prodDes = item.getProductName();
            if (item.getOptionName() != null && !item.getOptionName().isBlank()) {
                prodDes = prodDes + " / " + item.getOptionName();
            }
            line.put("PROD_DES", prodDes);

            // QTY
            line.put("QTY", String.valueOf(item.getQuantity()));

            // 금액 계산
            BigDecimal amount = item.getTotalPrice();
            applyVatCalculation(line, amount, getStringField(prodTemplate, "vatCalculation", "SUPPLY_DIV_11"));

            // 적요
            String remarks = getStringField(prodTemplate, "remarks", "");
            if (!remarks.isBlank()) line.put("REMARKS", remarks);

            applyExtraFields(line, prodTemplate);
            lines.add(line);
        }

        // 2) 배송비 라인
        BigDecimal deliveryFee = order.getDeliveryFee();
        Map<String, Object> delFeeTemplate = template.getLineDeliveryFee();
        if (shouldAddLine(deliveryFee, delFeeTemplate)) {
            Map<String, Object> line = buildBaseLine(header, ioDate, uploadSerNo, lineNo++);
            line.put("PROD_CD", getStringField(delFeeTemplate, "prodCd", ""));
            line.put("PROD_DES", getStringField(delFeeTemplate, "prodDes", "배송비"));
            line.put("QTY", "1");
            applyVatCalculation(line, deliveryFee, getStringField(delFeeTemplate, "vatCalculation", "SUPPLY_DIV_11"));
            if (getBoolField(delFeeTemplate, "negateAmount")) negateAmounts(line);
            String remarks = getStringField(delFeeTemplate, "remarks", "");
            if (!remarks.isBlank()) line.put("REMARKS", remarks);
            applyExtraFields(line, delFeeTemplate);
            lines.add(line);
        }

        // 3) 판매수수료 라인 (마켓별 품목코드 지원)
        BigDecimal commissionAmount = calculateCommissionAmount(order, settlements);
        Map<String, Object> commTemplate = template.getLineSalesCommission();
        if (shouldAddLine(commissionAmount, commTemplate)) {
            Map<String, Object> line = buildBaseLine(header, ioDate, uploadSerNo, lineNo++);
            // 마켓별 품목코드가 있으면 우선 사용
            String commProdCd = getMarketplaceProdCd(commTemplate, mktKey, "prodCd");
            String commProdDes = getMarketplaceProdCd(commTemplate, mktKey, "prodDes");
            line.put("PROD_CD", commProdCd.isBlank() ? getStringField(commTemplate, "prodCd", "") : commProdCd);
            line.put("PROD_DES", commProdDes.isBlank() ? getStringField(commTemplate, "prodDes", "판매수수료") : commProdDes);
            line.put("QTY", "1");
            applyVatCalculation(line, commissionAmount, getStringField(commTemplate, "vatCalculation", "SUPPLY_DIV_11"));
            if (getBoolField(commTemplate, "negateAmount")) negateAmounts(line);
            String remarks = getStringField(commTemplate, "remarks", "");
            if (!remarks.isBlank()) line.put("REMARKS", remarks);
            applyExtraFields(line, commTemplate);
            lines.add(line);
        }

        // 4) 배송수수료 라인 (마켓별 품목코드 지원)
        BigDecimal deliveryCommission = calculateDeliveryCommission(order, settlements);
        Map<String, Object> delCommTemplate = template.getLineDeliveryCommission();
        if (shouldAddLine(deliveryCommission, delCommTemplate)) {
            Map<String, Object> line = buildBaseLine(header, ioDate, uploadSerNo, lineNo++);
            // 마켓별 품목코드가 있으면 우선 사용
            String delCommProdCd = getMarketplaceProdCd(delCommTemplate, mktKey, "prodCd");
            String delCommProdDes = getMarketplaceProdCd(delCommTemplate, mktKey, "prodDes");
            line.put("PROD_CD", delCommProdCd.isBlank() ? getStringField(delCommTemplate, "prodCd", "") : delCommProdCd);
            line.put("PROD_DES", delCommProdDes.isBlank() ? getStringField(delCommTemplate, "prodDes", "배송수수료") : delCommProdDes);
            line.put("QTY", "1");
            applyVatCalculation(line, deliveryCommission, getStringField(delCommTemplate, "vatCalculation", "SUPPLY_DIV_11"));
            if (getBoolField(delCommTemplate, "negateAmount")) negateAmounts(line);
            String remarks = getStringField(delCommTemplate, "remarks", "");
            if (!remarks.isBlank()) line.put("REMARKS", remarks);
            applyExtraFields(line, delCommTemplate);
            lines.add(line);
        }

        // ECount API 형식: SaleList 내 각 요소는 { "BulkDatas": {...} } 형태
        List<Map<String, Object>> saleList = new ArrayList<>();
        for (Map<String, Object> line : lines) {
            Map<String, Object> wrapper = new LinkedHashMap<>();
            wrapper.put("BulkDatas", line);
            saleList.add(wrapper);
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("SaleList", saleList);
        return requestBody;
    }

    /**
     * 필드 매핑 기반 전표 생성 (신규 방식)
     * 테넌트별로 설정된 필드 매핑을 사용하여 동적으로 전표 생성
     *
     * @param order 주문 정보
     * @param settlements 정산 정보
     * @param fieldMappings 필드 매핑 목록
     * @return ECount SaveSale API 요청 body
     */
    public Map<String, Object> buildSaveSaleRequestWithMappings(
            Order order,
            List<OrderSettlement> settlements,
            List<ErpFieldMapping> fieldMappings) {

        String ioDate = order.getOrderedAt() != null
                ? order.getOrderedAt().format(DATE_FMT)
                : DATE_FMT.format(java.time.LocalDate.now());

        String uploadSerNo = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 헤더 필드 분리
        List<ErpFieldMapping> headerMappings = fieldMappings.stream()
                .filter(m -> m.getFieldPosition() == ErpFieldPosition.HEADER)
                .toList();

        // 라인 필드 분리
        Map<ErpLineType, List<ErpFieldMapping>> lineMappingsByType = new EnumMap<>(ErpLineType.class);
        for (ErpLineType type : ErpLineType.values()) {
            lineMappingsByType.put(type, fieldMappings.stream()
                    .filter(m -> m.getFieldPosition() == ErpFieldPosition.LINE)
                    .filter(m -> m.getLineType() == ErpLineType.ALL || m.getLineType() == type)
                    .toList());
        }

        List<Map<String, Object>> lines = new ArrayList<>();
        int lineNo = 1;

        // 1) 상품판매 라인
        for (OrderItem item : order.getItems()) {
            Map<String, Object> line = buildBaseLineWithMappings(headerMappings, order, item, ioDate, uploadSerNo, lineNo++);

            // 기본 상품 필드
            String prodCd = item.getErpProdCd();
            if (prodCd != null && !prodCd.isBlank()) {
                line.put("PROD_CD", prodCd);
            }

            String prodDes = item.getProductName();
            if (item.getOptionName() != null && !item.getOptionName().isBlank()) {
                prodDes = prodDes + " / " + item.getOptionName();
            }
            line.put("PROD_DES", prodDes);
            line.put("QTY", String.valueOf(item.getQuantity()));

            // 금액 계산
            applyVatCalculation(line, item.getTotalPrice(), "SUPPLY_DIV_11");

            // 라인 필드 매핑 적용
            applyLineMappings(line, lineMappingsByType.get(ErpLineType.PRODUCT_SALE), order, item);

            lines.add(line);
        }

        // 2) 배송비 라인
        BigDecimal deliveryFee = order.getDeliveryFee();
        if (deliveryFee != null && deliveryFee.compareTo(BigDecimal.ZERO) > 0) {
            Map<String, Object> line = buildBaseLineWithMappings(headerMappings, order, null, ioDate, uploadSerNo, lineNo++);
            line.put("PROD_DES", "배송비");
            line.put("QTY", "1");
            applyVatCalculation(line, deliveryFee, "SUPPLY_DIV_11");
            applyLineMappings(line, lineMappingsByType.get(ErpLineType.DELIVERY_FEE), order, null);
            lines.add(line);
        }

        // 3) 판매수수료 라인
        BigDecimal commissionAmount = calculateCommissionAmount(order, settlements);
        if (commissionAmount.compareTo(BigDecimal.ZERO) > 0) {
            Map<String, Object> line = buildBaseLineWithMappings(headerMappings, order, null, ioDate, uploadSerNo, lineNo++);
            line.put("PROD_DES", "판매수수료");
            line.put("QTY", "1");
            applyVatCalculation(line, commissionAmount, "SUPPLY_DIV_11");
            negateAmounts(line); // 수수료는 마이너스
            applyLineMappings(line, lineMappingsByType.get(ErpLineType.SALES_COMMISSION), order, null);
            lines.add(line);
        }

        // 4) 배송수수료 라인
        BigDecimal deliveryCommission = calculateDeliveryCommission(order, settlements);
        if (deliveryCommission.compareTo(BigDecimal.ZERO) > 0) {
            Map<String, Object> line = buildBaseLineWithMappings(headerMappings, order, null, ioDate, uploadSerNo, lineNo++);
            line.put("PROD_DES", "배송수수료");
            line.put("QTY", "1");
            applyVatCalculation(line, deliveryCommission, "SUPPLY_DIV_11");
            negateAmounts(line); // 수수료는 마이너스
            applyLineMappings(line, lineMappingsByType.get(ErpLineType.DELIVERY_COMMISSION), order, null);
            lines.add(line);
        }

        // ECount API 형식
        List<Map<String, Object>> saleList = new ArrayList<>();
        for (Map<String, Object> line : lines) {
            Map<String, Object> wrapper = new LinkedHashMap<>();
            wrapper.put("BulkDatas", line);
            saleList.add(wrapper);
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("SaleList", saleList);
        return requestBody;
    }

    /**
     * 헤더 매핑을 적용한 기본 라인 생성
     */
    private Map<String, Object> buildBaseLineWithMappings(
            List<ErpFieldMapping> headerMappings,
            Order order,
            OrderItem item,
            String ioDate,
            String uploadSerNo,
            int lineNo) {

        Map<String, Object> line = new LinkedHashMap<>();
        line.put("IO_DATE", ioDate);
        line.put("UPLOAD_SER_NO", uploadSerNo);
        line.put("LINE_NO", String.valueOf(lineNo));

        // 헤더 필드 매핑 적용
        for (ErpFieldMapping mapping : headerMappings) {
            String value = fieldResolver.resolveValue(mapping, order, item);
            if (value != null && !value.isBlank()) {
                line.put(mapping.getFieldName(), value);
            }
        }

        return line;
    }

    /**
     * 라인 필드 매핑 적용
     */
    private void applyLineMappings(Map<String, Object> line, List<ErpFieldMapping> lineMappings, Order order, OrderItem item) {
        if (lineMappings == null) return;

        for (ErpFieldMapping mapping : lineMappings) {
            String value = fieldResolver.resolveValue(mapping, order, item);
            if (value != null && !value.isBlank()) {
                line.put(mapping.getFieldName(), value);
            }
        }
    }

    private Map<String, Object> buildBaseLine(Map<String, Object> header, String ioDate, String uploadSerNo, int lineNo) {
        Map<String, Object> line = new LinkedHashMap<>(header);
        line.put("IO_DATE", ioDate);
        line.put("UPLOAD_SER_NO", uploadSerNo);
        line.put("LINE_NO", String.valueOf(lineNo));
        return line;
    }

    private void applyVatCalculation(Map<String, Object> line, BigDecimal totalAmount, String method) {
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;

        if ("NO_VAT".equals(method)) {
            line.put("SUPPLY_AMT", totalAmount.setScale(0, RoundingMode.HALF_UP).toPlainString());
            line.put("VAT_AMT", "0");
            line.put("PRICE", totalAmount.setScale(0, RoundingMode.HALF_UP).toPlainString());
        } else {
            // SUPPLY_DIV_11 (기본): VAT = round(총액/11), 공급가 = 총액 - VAT
            BigDecimal vatAmt = totalAmount.divide(BigDecimal.valueOf(11), 0, RoundingMode.HALF_UP);
            BigDecimal supplyAmt = totalAmount.subtract(vatAmt);
            line.put("SUPPLY_AMT", supplyAmt.setScale(0, RoundingMode.HALF_UP).toPlainString());
            line.put("VAT_AMT", vatAmt.setScale(0, RoundingMode.HALF_UP).toPlainString());
            line.put("PRICE", totalAmount.setScale(0, RoundingMode.HALF_UP).toPlainString());
        }
    }

    private void negateAmounts(Map<String, Object> line) {
        negateField(line, "SUPPLY_AMT");
        negateField(line, "VAT_AMT");
        negateField(line, "PRICE");
    }

    private void negateField(Map<String, Object> line, String field) {
        Object val = line.get(field);
        if (val != null) {
            try {
                BigDecimal bd = new BigDecimal(val.toString());
                line.put(field, bd.negate().toPlainString());
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private boolean shouldAddLine(BigDecimal amount, Map<String, Object> lineTemplate) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return !getBoolField(lineTemplate, "skipIfZero");
        }
        return true;
    }

    private BigDecimal calculateCommissionAmount(Order order, List<OrderSettlement> settlements) {
        // 정산 데이터 우선 사용
        if (settlements != null && !settlements.isEmpty()) {
            return settlements.stream()
                    .map(OrderSettlement::getCommissionAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        // fallback: OrderItem.commissionRate로 계산
        return order.getItems().stream()
                .filter(i -> i.getCommissionRate() != null)
                .map(i -> i.getTotalPrice()
                        .multiply(i.getCommissionRate())
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateDeliveryCommission(Order order, List<OrderSettlement> settlements) {
        // 배송비가 없으면 배송수수료도 없음
        if (order.getDeliveryFee() == null || order.getDeliveryFee().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // 1. 정산 데이터 우선 사용 (실제값)
        if (settlements != null && !settlements.isEmpty()) {
            BigDecimal total = settlements.stream()
                    .map(OrderSettlement::getDeliveryFeeCommission)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (total.compareTo(BigDecimal.ZERO) > 0) {
                return total;
            }
        }

        // 2. Order에 저장된 추정값 사용
        if (order.getEstimatedDeliveryCommission() != null
                && order.getEstimatedDeliveryCommission().compareTo(BigDecimal.ZERO) > 0) {
            return order.getEstimatedDeliveryCommission();
        }

        // 3. 기존 데이터 호환용 fallback 계산
        return calculateFallbackDeliveryCommission(order);
    }

    /**
     * 기존 데이터 호환용 fallback 계산
     * - 쿠팡: 배송비 × 3.3%
     * - 네이버: 67원 고정
     */
    private BigDecimal calculateFallbackDeliveryCommission(Order order) {
        BigDecimal deliveryFee = order.getDeliveryFee();
        if (deliveryFee == null || deliveryFee.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        switch (order.getMarketplaceType()) {
            case COUPANG:
                return deliveryFee
                        .multiply(BigDecimal.valueOf(0.033))
                        .setScale(0, RoundingMode.HALF_UP);
            case NAVER:
                return BigDecimal.valueOf(67);
            default:
                return BigDecimal.ZERO;
        }
    }

    @SuppressWarnings("unchecked")
    private void applyExtraFields(Map<String, Object> line, Map<String, Object> lineTemplate) {
        Object extra = lineTemplate.get("extraFields");
        if (extra instanceof Map) {
            ((Map<String, Object>) extra).forEach((k, v) -> {
                if (v != null && !v.toString().isBlank()) {
                    line.put(k, v.toString());
                }
            });
        }
    }

    private String getStringField(Map<String, Object> map, String key, String defaultValue) {
        if (map == null) return defaultValue;
        Object val = map.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    /**
     * 마켓별 품목코드 추출 (marketplaceProdCds 필드에서)
     * 예: { "marketplaceProdCds": { "COUPANG": { "prodCd": "001", "prodDes": "쿠팡수수료" } } }
     */
    @SuppressWarnings("unchecked")
    private String getMarketplaceProdCd(Map<String, Object> lineTemplate, String marketplace, String field) {
        if (lineTemplate == null) return "";
        Object marketplaceProdCds = lineTemplate.get("marketplaceProdCds");
        if (!(marketplaceProdCds instanceof Map)) return "";

        Map<String, Object> mktMap = (Map<String, Object>) marketplaceProdCds;
        Object mktEntry = mktMap.get(marketplace);
        if (!(mktEntry instanceof Map)) return "";

        Map<String, Object> entry = (Map<String, Object>) mktEntry;
        Object val = entry.get(field);
        return val != null ? val.toString() : "";
    }

    private boolean getBoolField(Map<String, Object> map, String key) {
        if (map == null) return false;
        Object val = map.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        if (val != null) return "true".equalsIgnoreCase(val.toString());
        return false;
    }
}
