package com.mhub.shipping.adapter.cj.dto;

import lombok.Builder;

/**
 * CJ대한통운 주소정제 API (ReqAddrRfnSm) 응답 결과 DTO
 *
 * 응답 필드:
 * - CLSFCD: 도착지 코드 (분류코드)
 * - SUBCLSFCD: 도착지 서브 코드
 * - CLSFADDR: 주소 약칭
 * - CLLDLVBRANNM: 배송집배점 명
 * - CLLDLVEMPNM: 배송SM명
 * - CLLDLVEMPNICKNM: SM분류코드 (배송사원 별칭)
 * - RSPSDIV: 권역 구분
 * - P2PCD: P2P코드
 */
@Builder
public record CjAddrRefineResponse(
        boolean success,
        String classificationCode,      // CLSFCD - 도착지 코드
        String subClassificationCode,   // SUBCLSFCD - 도착지 서브 코드
        String addressAlias,            // CLSFADDR - 주소 약칭
        String deliveryBranchName,      // CLLDLVBRANNM - 배송집배점 명
        String deliveryEmployeeName,    // CLLDLVEMPNM - 배송SM명
        String deliveryEmployeeNickname,// CLLDLVEMPNICKNM - SM분류코드
        String regionDivision,          // RSPSDIV - 권역 구분
        String p2pCode,                 // P2PCD - P2P코드
        String errorMessage
) {
    public static CjAddrRefineResponse failure(String errorMessage) {
        return CjAddrRefineResponse.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
