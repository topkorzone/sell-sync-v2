package com.mhub.shipping.adapter.cj;

import com.mhub.core.domain.entity.TenantCourierConfig;
import com.mhub.core.domain.enums.CourierType;
import com.mhub.shipping.adapter.CourierAdapter;
import com.mhub.shipping.adapter.cj.dto.CjAddrRefineResponse;
import com.mhub.shipping.adapter.cj.dto.CjApiResponse;
import com.mhub.shipping.adapter.cj.dto.CjRegBookRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CjCourierAdapter implements CourierAdapter {

    private final CjApiClient cjApiClient;

    @Override
    public CourierType getCourierType() {
        return CourierType.CJ;
    }

    @Override
    public ReservationResult reservePickup(TenantCourierConfig config, PickupRequest req) {
        CjReservationResult extResult = reservePickupWithClassification(config, req);
        return new ReservationResult(extResult.success(), extResult.trackingNumber(), extResult.errorMessage());
    }

    /**
     * 주소정제 정보를 포함한 확장 예약 결과 반환
     */
    public CjReservationResult reservePickupWithClassification(TenantCourierConfig config, PickupRequest req) {
        try {
            String custId = config.getContractCode();
            String bizRegNum = getExtraString(config, "bizRegNum");

            // 1. Token
            String token = cjApiClient.requestToken(custId, bizRegNum);

            // 2. Tracking number (if not pre-allocated)
            String trackingNumber = req.trackingNumber();
            if (trackingNumber == null || trackingNumber.isBlank()) {
                trackingNumber = cjApiClient.requestTrackingNumber(token, custId);
            }

            // 3. Address Refine (분류코드 조회)
            // 주소정제 API는 고객ID와 전체 주소를 파라미터로 받음
            String fullAddress = (req.receiverAddressBase() + " " +
                    (req.receiverAddressDetail() != null ? req.receiverAddressDetail() : "")).trim();
            CjAddrRefineResponse addrRefine = cjApiClient.refineAddress(
                    token, custId, fullAddress);

            // 4. RegBook
            CjRegBookRequest regBookReq = buildRegBookRequest(config, req, token, trackingNumber);
            log.info("CJ RegBook request parameters: {}", regBookReq.DATA());
            CjApiResponse response = cjApiClient.registerBooking(token, regBookReq);

            if (response.isSuccess()) {
                log.info("CJ RegBook success: trackingNumber={}, classificationCode={}",
                        trackingNumber, addrRefine.classificationCode());
            } else {
                log.warn("CJ RegBook failed: {}", response.RESULT_DETAIL());
            }
            // 접수 성공/실패와 관계없이 주소정제 데이터는 항상 반환
            return new CjReservationResult(
                    response.isSuccess(),
                    trackingNumber,
                    response.isSuccess() ? null : response.RESULT_DETAIL(),
                    addrRefine.classificationCode(),
                    addrRefine.subClassificationCode(),
                    addrRefine.addressAlias(),
                    addrRefine.deliveryBranchName(),
                    addrRefine.deliveryEmployeeNickname()
            );
        } catch (Exception e) {
            log.error("CJ pickup reservation failed", e);
            return new CjReservationResult(false, null, e.getMessage(),
                    null, null, null, null, null);
        }
    }

    /**
     * CJ 전용 예약 결과 (주소정제 정보 포함)
     */
    public record CjReservationResult(
            boolean success,
            String trackingNumber,
            String errorMessage,
            String classificationCode,
            String subClassificationCode,
            String addressAlias,
            String deliveryBranchName,
            String deliveryEmployeeNickname
    ) {}

    @Override
    public void cancelReservation(TenantCourierConfig config, String trackingNumber) {
        log.info("CJ cancel reservation: {}", trackingNumber);
    }

    private CjRegBookRequest buildRegBookRequest(TenantCourierConfig config, PickupRequest req,
                                                  String token, String trackingNumber) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String custId = config.getContractCode();
        String mpckKey = today + "_" + custId + "_" + req.marketplaceOrderId();

        String[] senderPhone = splitPhone(config.getSenderPhone());
        String[] receiverPhone = splitPhone(req.receiverPhone());
        String[] buyerPhone = splitPhone(req.buyerPhone());

        String calDvCd = getExtraString(config, "calDvCd", "01");
        String frtDvCd = getExtraString(config, "frtDvCd", "02");
        String cntrItemCd = getExtraString(config, "cntrItemCd", "01");
        // Per-request boxTypeCd overrides config default
        String boxTypeCd = req.extraOptions() != null && req.extraOptions().containsKey("boxTypeCd")
                ? req.extraOptions().get("boxTypeCd")
                : getExtraString(config, "boxTypeCd", "02");
        String custMgmtDlcmCd = custId;
        String senderDetailAddr = getExtraString(config, "senderDetailAddress", " ");

        // printCount: 0이면 최초출력(N), 1이상이면 재출력(Y)
        int printCount = req.extraOptions() != null && req.extraOptions().containsKey("printCount")
                ? Integer.parseInt(req.extraOptions().get("printCount"))
                : 0;
        String reprintYn = printCount == 0 ? "N" : "Y";
        String ouppur = String.valueOf(printCount + 1);

        CjRegBookRequest.Builder builder = CjRegBookRequest.builder()
                .put("TOKEN_NUM", token)
                .put("INVC_NO", trackingNumber)
                .put("CUST_ID", custId)
                .put("MPCK_KEY", mpckKey)
                .put("CUST_USE_NO", req.marketplaceOrderId())
                .put("RCPT_DV", "01")
                .put("WORK_DV_CD", "01")
                .put("REQ_DV_CD", "01")
                .put("RCPT_YMD", today)
                .put("SENDR_NM", config.getSenderName())
                .put("SENDR_TEL_NO1", senderPhone[0])
                .put("SENDR_TEL_NO2", senderPhone[1])
                .put("SENDR_TEL_NO3", senderPhone[2])
                .put("SENDR_CELL_NO1", senderPhone[0])
                .put("SENDR_CELL_NO2", senderPhone[1])
                .put("SENDR_CELL_NO3", senderPhone[2])
                .put("SENDR_ZIP_NO", config.getSenderZipcode())
                .put("SENDR_ADDR", config.getSenderAddress())
                .put("SENDR_DETAIL_ADDR", senderDetailAddr)
                .put("RCVR_NM", req.receiverName())
                .put("RCVR_TEL_NO1", receiverPhone[0])
                .put("RCVR_TEL_NO2", receiverPhone[1])
                .put("RCVR_TEL_NO3", receiverPhone[2])
                .put("RCVR_CELL_NO1", receiverPhone[0])
                .put("RCVR_CELL_NO2", receiverPhone[1])
                .put("RCVR_CELL_NO3", receiverPhone[2])
                .put("RCVR_ZIP_NO", req.receiverZipcode())
                .put("RCVR_ADDR", req.receiverAddressBase() != null ? req.receiverAddressBase() : "")
                .put("RCVR_DETAIL_ADDR", req.receiverAddressDetail() != null ? req.receiverAddressDetail() : " ")
                .put("REMARK_1", req.memo() != null ? req.memo() : "")
                .put("ORDRR_NM", req.buyerName() != null ? req.buyerName() : req.receiverName())
                .put("ORDRR_TEL_NO1", buyerPhone[0])
                .put("ORDRR_TEL_NO2", buyerPhone[1])
                .put("ORDRR_TEL_NO3", buyerPhone[2])
                .put("ORDRR_CELL_NO1", buyerPhone[0])
                .put("ORDRR_CELL_NO2", buyerPhone[1])
                .put("ORDRR_CELL_NO3", buyerPhone[2])
                .put("CAL_DV_CD", calDvCd)
                .put("FRT_DV_CD", frtDvCd)
                .put("CNTR_ITEM_CD", cntrItemCd)
                .put("BOX_TYPE_CD", boxTypeCd)
                .put("BOX_QTY", "1")
                .put("PRT_ST", "01")
                .put("COD_YN", "N")
                .put("DLV_DV", "01")
                .put("CUST_MGMT_DLCM_CD", custMgmtDlcmCd)
                .put("REPRINTYN", reprintYn)
                .put("OUPPUR", ouppur);

        // Items
        List<Map<String, Object>> items = new ArrayList<>();
        if (req.items() != null) {
            for (int i = 0; i < req.items().size(); i++) {
                ItemInfo item = req.items().get(i);
                Map<String, Object> itemMap = new LinkedHashMap<>();
                itemMap.put("GDS_NM", item.productName());
                itemMap.put("GDS_QTY", String.valueOf(item.quantity()));
                itemMap.put("GDS_AMT", String.valueOf(item.unitPrice()));
                itemMap.put("MPCK_SEQ", String.valueOf(i + 1));
                items.add(itemMap);
            }
        }
        builder.items(items);

        return builder.build();
    }

    static String[] splitPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return new String[]{"", "", ""};
        }
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (cleaned.length() == 11) {
            return new String[]{cleaned.substring(0, 3), cleaned.substring(3, 7), cleaned.substring(7)};
        } else if (cleaned.length() == 10) {
            return new String[]{cleaned.substring(0, 3), cleaned.substring(3, 6), cleaned.substring(6)};
        } else if (phone.contains("-")) {
            String[] parts = phone.split("-");
            if (parts.length == 3) return parts;
        }
        return new String[]{cleaned, "", ""};
    }

    private String getExtraString(TenantCourierConfig config, String key) {
        return getExtraString(config, key, "");
    }

    private String getExtraString(TenantCourierConfig config, String key, String defaultValue) {
        if (config.getExtraConfig() == null) return defaultValue;
        Object val = config.getExtraConfig().get(key);
        return val != null ? val.toString() : defaultValue;
    }
}
