package com.mhub.shipping.adapter.cj;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cj.api")
public record CjApiProperties(String baseUrl) {}
