package com.mhub.shipping.adapter.cj;

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

    private record CachedToken(String token, Instant expiresAt) {
        boolean isExpiringSoon() {
            return Instant.now().isAfter(expiresAt.minusSeconds(30 * 60));
        }
    }
}
