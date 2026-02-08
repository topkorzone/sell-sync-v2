package com.mhub.shipping.adapter.cj.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CJ대한통운 주소정제 API (ReqAddrRfnSm) 요청 DTO
 *
 * 요청 필드:
 * - TOKEN_NUM: 토큰번호 (NOT NULL)
 * - CLNTNUM: 고객 ID (NOT NULL)
 * - CLNTMGMCUSTCD: 협력사 코드 (협력사코드가 없을 경우 고객ID로 대체)
 * - USER_ID: 중개업체ID (중개업체 사용 시 필수)
 * - ADDRESS: 주소 (NOT NULL)
 */
public record CjAddrRefineRequest(Map<String, Object> DATA) {

    public static CjAddrRefineRequest of(String token, String custId, String address) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("TOKEN_NUM", token);
        data.put("CLNTNUM", custId);
        data.put("CLNTMGMCUSTCD", custId); // 협력사코드가 없으면 고객ID로 대체
        data.put("ADDRESS", address);
        return new CjAddrRefineRequest(data);
    }

    public static CjAddrRefineRequest of(String token, String custId, String clntMgmCustCd, String address) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("TOKEN_NUM", token);
        data.put("CLNTNUM", custId);
        data.put("CLNTMGMCUSTCD", clntMgmCustCd != null ? clntMgmCustCd : custId);
        data.put("ADDRESS", address);
        return new CjAddrRefineRequest(data);
    }
}
