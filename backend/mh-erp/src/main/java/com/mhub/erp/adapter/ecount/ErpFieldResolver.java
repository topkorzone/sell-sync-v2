package com.mhub.erp.adapter.ecount;

import com.mhub.core.domain.entity.ErpFieldMapping;
import com.mhub.core.domain.entity.Order;
import com.mhub.core.domain.entity.OrderItem;
import com.mhub.core.domain.enums.ErpFieldValueType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ERP 필드 값 해석기
 * 필드 매핑 설정에 따라 주문 정보에서 값을 추출
 */
@Slf4j
@Component
public class ErpFieldResolver {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)}");

    /**
     * 필드 매핑에 따라 값 해석
     *
     * @param mapping 필드 매핑 설정
     * @param order 주문 정보
     * @param item 주문 상품 (라인 필드일 경우)
     * @return 해석된 값
     */
    public String resolveValue(ErpFieldMapping mapping, Order order, OrderItem item) {
        if (mapping == null) {
            return null;
        }

        ErpFieldValueType valueType = mapping.getValueType();

        switch (valueType) {
            case FIXED:
                return mapping.getFixedValue();

            case MARKETPLACE:
                return resolveMarketplaceValue(mapping, order);

            case ORDER_FIELD:
                return resolveOrderFieldValue(mapping, order, item);

            default:
                log.warn("Unknown value type: {}", valueType);
                return null;
        }
    }

    /**
     * 마켓별 값 해석
     */
    private String resolveMarketplaceValue(ErpFieldMapping mapping, Order order) {
        Map<String, String> marketplaceValues = mapping.getMarketplaceValues();
        if (marketplaceValues == null || marketplaceValues.isEmpty()) {
            return mapping.getFixedValue(); // fallback
        }

        String marketplaceName = order.getMarketplaceType().name();
        String value = marketplaceValues.get(marketplaceName);

        if (value == null) {
            // 기본값으로 fallback
            value = marketplaceValues.get("DEFAULT");
        }

        return value;
    }

    /**
     * 주문 정보 템플릿 해석
     * 사용 가능한 플레이스홀더:
     * - {orderId}: 마켓플레이스 주문번호
     * - {productOrderId}: 상품주문번호
     * - {marketplaceName}: 마켓명 (COUPANG, NAVER 등)
     * - {marketplaceDisplayName}: 마켓 표시명 (쿠팡, 네이버 등)
     * - {buyerName}: 주문자명
     * - {receiverName}: 수령인명
     * - {receiverPhone}: 수령인 연락처
     * - {receiverAddress}: 배송지 주소
     * - {orderDate}: 주문일자 (yyyyMMdd)
     * - {productName}: 상품명 (라인 필드일 경우)
     * - {optionName}: 옵션명 (라인 필드일 경우)
     * - {quantity}: 수량 (라인 필드일 경우)
     */
    private String resolveOrderFieldValue(ErpFieldMapping mapping, Order order, OrderItem item) {
        String template = mapping.getOrderFieldTemplate();
        if (template == null || template.isBlank()) {
            return null;
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);

        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String replacement = getPlaceholderValue(placeholder, order, item);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement != null ? replacement : ""));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 플레이스홀더 값 추출
     */
    private String getPlaceholderValue(String placeholder, Order order, OrderItem item) {
        switch (placeholder) {
            case "orderId":
                return order.getMarketplaceOrderId();

            case "productOrderId":
                return order.getMarketplaceProductOrderId();

            case "marketplaceName":
                return order.getMarketplaceType().name();

            case "marketplaceDisplayName":
                return getMarketplaceDisplayName(order.getMarketplaceType().name());

            case "buyerName":
                return order.getBuyerName();

            case "receiverName":
                return order.getReceiverName();

            case "receiverPhone":
                return order.getReceiverPhone();

            case "receiverAddress":
                return order.getReceiverAddress();

            case "orderDate":
                return order.getOrderedAt() != null ? order.getOrderedAt().format(DATE_FMT) : "";

            case "productName":
                return item != null ? item.getProductName() : "";

            case "optionName":
                return item != null ? item.getOptionName() : "";

            case "quantity":
                return item != null ? String.valueOf(item.getQuantity()) : "";

            case "totalAmount":
                return order.getTotalAmount() != null ? order.getTotalAmount().toPlainString() : "0";

            case "deliveryFee":
                return order.getDeliveryFee() != null ? order.getDeliveryFee().toPlainString() : "0";

            default:
                log.debug("Unknown placeholder: {}", placeholder);
                return "";
        }
    }

    /**
     * 마켓플레이스 표시명
     */
    private String getMarketplaceDisplayName(String marketplaceName) {
        switch (marketplaceName) {
            case "COUPANG":
                return "쿠팡";
            case "NAVER":
                return "네이버";
            case "GMARKET":
                return "G마켓";
            case "ELEVEN_STREET":
                return "11번가";
            default:
                return marketplaceName;
        }
    }
}
