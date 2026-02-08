package com.mhub.shipping.adapter.cj;

import com.mhub.shipping.adapter.cj.dto.CjAddrRefineRequest;
import com.mhub.shipping.adapter.cj.dto.CjAddrRefineResponse;
import com.mhub.shipping.adapter.cj.dto.CjApiResponse;
import com.mhub.shipping.adapter.cj.dto.CjRegBookRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class CjApiClient {

    private final WebClient webClient;
    private final ConcurrentHashMap<String, CachedToken> tokenCache = new ConcurrentHashMap<>();

    public CjApiClient(CjApiProperties props) {
        this.webClient = WebClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public String requestToken(String custId, String bizRegNum) {
        CachedToken cached = tokenCache.get(custId);
        if (cached != null && !cached.isExpiringSoon()) {
            return cached.token;
        }

        log.info("CJ token request for custId={}", custId);
        Map<String, Object> body = Map.of("DATA", Map.of(
                "CUST_ID", custId,
                "BIZ_REG_NUM", bizRegNum
        ));

        CjApiResponse response = webClient.post()
                .uri("/ReqOneDayToken")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(CjApiResponse.class)
                .block();

        if (response == null || !response.isSuccess()) {
            String detail = response != null ? response.RESULT_DETAIL() : "No response";
            throw new RuntimeException("CJ token request failed: " + detail);
        }

        String token = (String) response.DATA().get("TOKEN_NUM");
        tokenCache.put(custId, new CachedToken(token, Instant.now().plusSeconds(24 * 3600)));
        return token;
    }

    public String requestTrackingNumber(String token, String custId) {
        log.info("CJ tracking number request for custId={}", custId);
        Map<String, Object> body = Map.of("DATA", Map.of(
                "TOKEN_NUM", token,
                "CLNTNUM", custId
        ));

        CjApiResponse response = webClient.post()
                .uri("/ReqInvcNo")
                .header("CJ-Gateway-APIKey", token)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(CjApiResponse.class)
                .block();

        if (response == null || !response.isSuccess()) {
            String detail = response != null ? response.RESULT_DETAIL() : "No response";
            throw new RuntimeException("CJ tracking number request failed: " + detail);
        }

        return (String) response.DATA().get("INVC_NO");
    }

    public CjApiResponse registerBooking(String token, CjRegBookRequest request) {
        log.info("CJ RegBook request");
        CjApiResponse response = webClient.post()
                .uri("/RegBook")
                .header("CJ-Gateway-APIKey", token)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CjApiResponse.class)
                .block();

        if (response == null) {
            throw new RuntimeException("CJ RegBook request failed: No response");
        }
        return response;
    }

    /**
     * CJ대한통운 주소정제 API (ReqAddrRfnSm) 호출
     * 분류코드, 주소약칭, 배달점소 정보를 반환
     *
     * @param token 토큰번호
     * @param custId 고객 ID (CLNTNUM)
     * @param address 주소
     * @return 주소정제 결과 (분류코드, 주소약칭, 배달점소 등)
     */
    public CjAddrRefineResponse refineAddress(String token, String custId, String address) {
        log.info("CJ ReqAddrRfnSm request: custId={}", custId);
        try {
            CjAddrRefineRequest request = CjAddrRefineRequest.of(token, custId, address);
            CjApiResponse response = webClient.post()
                    .uri("/ReqAddrRfnSm")
                    .header("CJ-Gateway-APIKey", token)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(CjApiResponse.class)
                    .block();

            if (response == null || !response.isSuccess()) {
                String detail = response != null ? response.RESULT_DETAIL() : "No response";
                log.warn("CJ ReqAddrRfnSm failed: {}", detail);
                return CjAddrRefineResponse.failure(detail);
            }

            Map<String, Object> data = response.DATA();
            log.info("CJ ReqAddrRfnSm response DATA: {}", data);
            return CjAddrRefineResponse.builder()
                    .success(true)
                    .classificationCode(getString(data, "CLSFCD"))           // 도착지 코드
                    .subClassificationCode(getString(data, "SUBCLSFCD"))     // 도착지 서브 코드
                    .addressAlias(getString(data, "CLSFADDR"))               // 주소 약칭
                    .deliveryBranchName(getString(data, "CLLDLVBRANNM"))     // 배송집배점 명
                    .deliveryEmployeeName(getString(data, "CLLDLVEMPNM"))    // 배송SM명
                    .deliveryEmployeeNickname(getString(data, "CLLDLVEMPNICKNM")) // SM분류코드
                    .regionDivision(getString(data, "RSPSDIV"))              // 권역 구분
                    .p2pCode(getString(data, "P2PCD"))                       // P2P코드
                    .build();
        } catch (Exception e) {
            log.error("CJ ReqAddrRfnSm request error", e);
            return CjAddrRefineResponse.failure(e.getMessage());
        }
    }

    private String getString(Map<String, Object> data, String key) {
        Object val = data.get(key);
        return val != null ? val.toString() : null;
    }

    private record CachedToken(String token, Instant expiresAt) {
        boolean isExpiringSoon() {
            return Instant.now().isAfter(expiresAt.minusSeconds(30 * 60));
        }
    }
}
