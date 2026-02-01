package com.mhub.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.mhub")
@EntityScan(basePackages = "com.mhub")
public class MarketplaceHubApplication {
    public static void main(String[] args) { SpringApplication.run(MarketplaceHubApplication.class, args); }
}
