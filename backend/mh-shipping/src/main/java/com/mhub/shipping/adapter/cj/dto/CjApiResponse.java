package com.mhub.shipping.adapter.cj.dto;

import java.util.Map;

public record CjApiResponse(
        String RESULT_CD,
        String RESULT_DETAIL,
        Map<String, Object> DATA
) {
    public boolean isSuccess() {
        return "S".equals(RESULT_CD);
    }
}
