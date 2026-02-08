package com.mhub.erp.adapter.ecount;

import com.mhub.common.exception.BusinessException;
import com.mhub.common.exception.ErrorCodes;
import com.mhub.core.domain.entity.TenantErpConfig;
import com.mhub.core.domain.enums.ErpType;
import com.mhub.erp.adapter.ErpAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ECountAdapter implements ErpAdapter {

    private static final String ZONE_API_URL = "https://oapi.ecount.com/OAPI/V2/Zone";

    @Qualifier("erpWebClient")
    private final WebClient webClient;

    @Override
    public ErpType getErpType() {
        return ErpType.ECOUNT;
    }

    @Override
    public boolean testConnection(TenantErpConfig config) {
        try {
            String zone = getZone(config.getCompanyCode());
            String sessionId = login(zone, config.getCompanyCode(), config.getUserId(), config.getApiKey());
            log.info("ECount connection test successful for company {}, zone={}, sessionId obtained",
                    config.getCompanyCode(), zone);
            return true;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("ECount connection test failed for company {}", config.getCompanyCode(), e);
            throw new BusinessException(ErrorCodes.ERP_CONNECTION_FAILED,
                    "ECount 연결 테스트 실패: " + e.getMessage());
        }
    }

    private String getZone(String companyCode) {
        log.debug("Getting zone for company {}", companyCode);

        Map<String, Object> requestBody = Map.of("COM_CODE", companyCode);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri(ZONE_API_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                throw new BusinessException(ErrorCodes.ERP_CONNECTION_FAILED, "Zone API 응답이 없습니다");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("Data");
            if (data == null) {
                String status = String.valueOf(response.get("Status"));
                String message = String.valueOf(response.get("Error"));
                throw new BusinessException(ErrorCodes.ERP_CONNECTION_FAILED,
                        "Zone 조회 실패: [" + status + "] " + message);
            }

            String zone = String.valueOf(data.get("ZONE"));
            log.debug("Zone obtained for company {}: {}", companyCode, zone);
            return zone;
        } catch (WebClientResponseException e) {
            log.error("Zone API error: {}", e.getResponseBodyAsString());
            throw new BusinessException(ErrorCodes.ERP_CONNECTION_FAILED,
                    "Zone API 호출 실패: " + e.getMessage());
        }
    }

    private String login(String zone, String companyCode, String userId, String apiKey) {
        String loginUrl = String.format("https://oapi%s.ecount.com/OAPI/V2/OAPILogin", zone);
        log.debug("Logging in to ECount at {}", loginUrl);

        Map<String, Object> requestBody = Map.of(
                "COM_CODE", companyCode,
                "USER_ID", userId,
                "API_CERT_KEY", apiKey,
                "LAN_TYPE", "ko-KR",
                "ZONE", zone
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri(loginUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                throw new BusinessException(ErrorCodes.ERP_CONNECTION_FAILED, "Login API 응답이 없습니다");
            }

            log.debug("Login API response: {}", response);

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("Data");
            if (data == null) {
                throw new BusinessException(ErrorCodes.ERP_CONNECTION_FAILED,
                        "ECount 로그인 실패: Data 없음");
            }

            log.debug("Login API Data: {}", data);

            // ECount 응답 구조: Data.Code가 "00"이면 성공, 아니면 실패
            String code = String.valueOf(data.get("Code"));

            if (!"00".equals(code)) {
                String message = String.valueOf(data.get("Message"));
                throw new BusinessException(ErrorCodes.ERP_CONNECTION_FAILED,
                        "ECount 로그인 실패: " + message);
            }

            // 로그인 성공 시 SESSION_ID는 Data.Datas.SESSION_ID에 있음
            @SuppressWarnings("unchecked")
            Map<String, Object> datas = (Map<String, Object>) data.get("Datas");
            if (datas == null) {
                throw new BusinessException(ErrorCodes.ERP_CONNECTION_FAILED,
                        "ECount 로그인 실패: Datas 없음");
            }

            String sessionId = String.valueOf(datas.get("SESSION_ID"));
            if (sessionId == null || sessionId.isEmpty() || "null".equals(sessionId)) {
                throw new BusinessException(ErrorCodes.ERP_CONNECTION_FAILED,
                        "ECount 로그인 실패: SESSION_ID를 받지 못했습니다");
            }

            log.debug("Login successful, SESSION_ID obtained");
            return sessionId;
        } catch (BusinessException e) {
            throw e;
        } catch (WebClientResponseException e) {
            log.error("Login API error: {}", e.getResponseBodyAsString());
            throw new BusinessException(ErrorCodes.ERP_CONNECTION_FAILED,
                    "ECount 로그인 실패: " + e.getMessage());
        } catch (Exception e) {
            log.error("Login failed", e);
            throw new BusinessException(ErrorCodes.ERP_CONNECTION_FAILED,
                    "ECount 로그인 실패: " + e.getMessage());
        }
    }

    @Override
    public DocumentResult createSalesDocument(TenantErpConfig config, SalesDocumentRequest request) {
        log.info("ECount createSalesDocument for date {}", request.date());
        return new DocumentResult(false, null, "단건 전표 등록은 createSaveSale()을 사용하세요", Map.of());
    }

    /**
     * ECount SaveSale API를 호출하여 판매전표를 등록합니다.
     * @param config ERP 설정 (인증 정보 포함)
     * @param saveSaleBody ECountSalesDocumentBuilder가 생성한 요청 body
     * @return DocumentResult (성공 시 전표번호 포함)
     */
    @SuppressWarnings("unchecked")
    public DocumentResult createSaveSale(TenantErpConfig config, Map<String, Object> saveSaleBody) {
        try {
            String zone = getZone(config.getCompanyCode());
            String sessionId = login(zone, config.getCompanyCode(), config.getUserId(), config.getApiKey());

            String apiUrl = String.format(
                    "https://oapi%s.ecount.com/OAPI/V2/Sale/SaveSale?SESSION_ID=%s",
                    zone, sessionId);

            log.debug("Calling SaveSale API at {}", apiUrl);
            log.info("SaveSale request body: {}", saveSaleBody);

            Map<String, Object> response = webClient.post()
                    .uri(apiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(saveSaleBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                return new DocumentResult(false, null, "SaveSale API 응답이 없습니다", Map.of());
            }

            log.debug("SaveSale API response: {}", response);

            String status = String.valueOf(response.get("Status"));
            if (!"200".equals(status)) {
                Object error = response.get("Error");
                String errorMsg = error != null ? error.toString() : "HTTP " + status;
                return new DocumentResult(false, null, "전표 등록 실패: " + errorMsg, response);
            }

            Map<String, Object> data = (Map<String, Object>) response.get("Data");
            if (data == null) {
                return new DocumentResult(false, null, "전표 등록 실패: Data 없음", response);
            }

            Object failCntObj = data.get("FailCnt");
            int failCnt = failCntObj != null ? Integer.parseInt(failCntObj.toString()) : 0;

            if (failCnt > 0) {
                Object resultDetails = data.get("ResultDetails");
                String detailMsg = resultDetails != null ? resultDetails.toString() : "상세 내용 없음";
                return new DocumentResult(false, null, "전표 등록 일부 실패: " + detailMsg, response);
            }

            // 성공 - 전표번호 추출
            String slipNo = null;
            Object slipNos = data.get("SlipNos");
            if (slipNos instanceof List && !((List<?>) slipNos).isEmpty()) {
                slipNo = ((List<?>) slipNos).get(0).toString();
            }

            int successCnt = data.get("SuccessCnt") != null ? Integer.parseInt(data.get("SuccessCnt").toString()) : 0;
            log.info("SaveSale success: {} lines, slipNo={}", successCnt, slipNo);
            return new DocumentResult(true, slipNo, null, response);

        } catch (BusinessException e) {
            throw e;
        } catch (WebClientResponseException e) {
            log.error("SaveSale API error: {}", e.getResponseBodyAsString());
            return new DocumentResult(false, null, "SaveSale API 호출 실패: " + e.getMessage(), Map.of());
        } catch (Exception e) {
            log.error("SaveSale failed", e);
            return new DocumentResult(false, null, "전표 등록 실패: " + e.getMessage(), Map.of());
        }
    }

    @Override
    public DocumentResult createJournalEntry(TenantErpConfig config, JournalEntryRequest request) {
        log.info("ECount createJournalEntry for date {}", request.date());
        return new DocumentResult(false, null, "ECount 분개 등록 기능은 2단계에서 구현됩니다", Map.of());
    }

    @Override
    public DocumentStatus getDocumentStatus(TenantErpConfig config, String documentId) {
        log.info("ECount getDocumentStatus for {}", documentId);
        return new DocumentStatus(documentId, "UNKNOWN", Map.of());
    }

    @Override
    public ItemFetchResult fetchItems(TenantErpConfig config) {
        try {
            String zone = getZone(config.getCompanyCode());
            String sessionId = login(zone, config.getCompanyCode(), config.getUserId(), config.getApiKey());
            return fetchItemsWithSession(zone, sessionId);
        } catch (BusinessException e) {
            log.error("ECount fetchItems failed: {}", e.getMessage());
            return new ItemFetchResult(false, List.of(), e.getMessage(), 0);
        } catch (Exception e) {
            log.error("ECount fetchItems failed", e);
            return new ItemFetchResult(false, List.of(), "품목 조회 실패: " + e.getMessage(), 0);
        }
    }

    @Override
    public InventoryFetchResult fetchInventoryBalance(TenantErpConfig config, String baseDate, List<String> prodCds) {
        try {
            String zone = getZone(config.getCompanyCode());
            String sessionId = login(zone, config.getCompanyCode(), config.getUserId(), config.getApiKey());
            return fetchInventoryWithSession(zone, sessionId, baseDate, prodCds);
        } catch (BusinessException e) {
            log.error("ECount fetchInventoryBalance failed: {}", e.getMessage());
            return new InventoryFetchResult(false, List.of(), e.getMessage());
        } catch (Exception e) {
            log.error("ECount fetchInventoryBalance failed", e);
            return new InventoryFetchResult(false, List.of(), "재고 조회 실패: " + e.getMessage());
        }
    }

    private InventoryFetchResult fetchInventoryWithSession(String zone, String sessionId, String baseDate, List<String> prodCds) {
        String apiUrl = String.format(
                "https://oapi%s.ecount.com/OAPI/V2/InventoryBalance/GetListInventoryBalanceStatusByLocation?SESSION_ID=%s",
                zone, sessionId);

        log.debug("Fetching inventory balance from ECount at {}, baseDate={}, prodCds={}", apiUrl, baseDate, prodCds);

        // 요청 body 구성
        Map<String, Object> requestBody = new java.util.HashMap<>();
        requestBody.put("BASE_DATE", baseDate);
        if (prodCds != null && !prodCds.isEmpty()) {
            requestBody.put("PROD_CD", String.join(",", prodCds));
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri(apiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                return new InventoryFetchResult(false, List.of(), "재고 조회 API 응답이 없습니다");
            }

            log.debug("ECount inventory API response: Status={}, Error={}", response.get("Status"), response.get("Error"));

            // 에러 체크
            Object error = response.get("Error");
            if (error != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> errorMap = (Map<String, Object>) error;
                String errorMessage = String.valueOf(errorMap.get("Message"));
                return new InventoryFetchResult(false, List.of(), "재고 조회 실패: " + errorMessage);
            }

            String status = String.valueOf(response.get("Status"));
            if (!"200".equals(status)) {
                return new InventoryFetchResult(false, List.of(), "재고 조회 실패: HTTP " + status);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("Data");
            if (data == null) {
                return new InventoryFetchResult(false, List.of(), "재고 조회 실패: Data 없음");
            }

            // 재고 데이터는 Data.Result에 있음
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("Result");
            if (items == null) {
                items = List.of();
            }

            log.info("ECount inventory fetched successfully: {} items", items.size());
            return new InventoryFetchResult(true, items, null);
        } catch (WebClientResponseException e) {
            log.error("ECount inventory API error: {}", e.getResponseBodyAsString());
            return new InventoryFetchResult(false, List.of(), "재고 조회 API 호출 실패: " + e.getMessage());
        }
    }

    private ItemFetchResult fetchItemsWithSession(String zone, String sessionId) {
        String apiUrl = String.format(
                "https://oapi%s.ecount.com/OAPI/V2/InventoryBasic/GetBasicProductsList?SESSION_ID=%s",
                zone, sessionId);

        log.debug("Fetching items from ECount at {}", apiUrl);

        // 전체 품목 조회 - PROD_TYPE을 비우면 전체 검색
        Map<String, Object> requestBody = new java.util.HashMap<>();
        requestBody.put("PROD_CD", "");
        requestBody.put("PROD_TYPE", "");

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri(apiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                return new ItemFetchResult(false, List.of(), "품목 조회 API 응답이 없습니다", 0);
            }

            log.debug("ECount items API response: Status={}, Error={}", response.get("Status"), response.get("Error"));

            // 에러 체크: Error 필드가 있으면 실패
            Object error = response.get("Error");
            if (error != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> errorMap = (Map<String, Object>) error;
                String errorMessage = String.valueOf(errorMap.get("Message"));
                return new ItemFetchResult(false, List.of(), "품목 조회 실패: " + errorMessage, 0);
            }

            // Status가 200이 아니면 실패
            String status = String.valueOf(response.get("Status"));
            if (!"200".equals(status)) {
                return new ItemFetchResult(false, List.of(), "품목 조회 실패: HTTP " + status, 0);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("Data");
            if (data == null) {
                return new ItemFetchResult(false, List.of(), "품목 조회 실패: Data 없음", 0);
            }

            // 품목 데이터는 Data.Result에 있음
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("Result");
            if (items == null) {
                items = List.of();
            }

            log.info("ECount items fetched successfully: {} items", items.size());
            return new ItemFetchResult(true, items, null, items.size());
        } catch (WebClientResponseException e) {
            log.error("ECount items API error: {}", e.getResponseBodyAsString());
            return new ItemFetchResult(false, List.of(), "품목 조회 API 호출 실패: " + e.getMessage(), 0);
        }
    }
}
