package com.mhub.marketplace.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class MarketplaceConfig {
    @Bean
    public WebClient.Builder marketplaceWebClientBuilder() {
        return WebClient.builder().codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024));
    }
}
