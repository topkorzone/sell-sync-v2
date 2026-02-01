package com.mhub.scheduler.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import java.net.URI;

@Configuration
public class SqsConfig {
    @Value("${mhub.aws.sqs.endpoint:}") private String sqsEndpoint;
    @Value("${mhub.aws.region:ap-northeast-2}") private String awsRegion;
    @Value("${mhub.aws.access-key:test}") private String accessKey;
    @Value("${mhub.aws.secret-key:test}") private String secretKey;

    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        var builder = SqsAsyncClient.builder().region(Region.of(awsRegion)).credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
        if (sqsEndpoint != null && !sqsEndpoint.isBlank()) builder.endpointOverride(URI.create(sqsEndpoint));
        return builder.build();
    }
}
