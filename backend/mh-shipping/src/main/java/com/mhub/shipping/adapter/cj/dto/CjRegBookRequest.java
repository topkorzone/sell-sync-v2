package com.mhub.shipping.adapter.cj.dto;

import java.util.List;
import java.util.Map;

public record CjRegBookRequest(Map<String, Object> DATA) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final java.util.LinkedHashMap<String, Object> data = new java.util.LinkedHashMap<>();

        public Builder put(String key, Object value) {
            data.put(key, value);
            return this;
        }

        public Builder items(List<Map<String, Object>> items) {
            data.put("ARRAY", items);
            return this;
        }

        public CjRegBookRequest build() {
            return new CjRegBookRequest(Map.copyOf(data));
        }
    }
}
