package com.lecook.backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.checksums.*;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import java.net.URI;
import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "AWS_ENDPOINT_URL_S3")
public class ObjectStorageConfig {
    @Bean(destroyMethod = "close")
    S3Client recipeImagesClient(Environment environment) {
        return S3Client.builder()
            .endpointOverride(URI.create(environment.getRequiredProperty("AWS_ENDPOINT_URL_S3")))
            .region(Region.of(environment.getRequiredProperty("AWS_REGION")))
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                environment.getRequiredProperty("AWS_ACCESS_KEY_ID"), environment.getRequiredProperty("AWS_SECRET_ACCESS_KEY"))))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).chunkedEncodingEnabled(false).build())
            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
            .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
            .httpClientBuilder(UrlConnectionHttpClient.builder().connectionTimeout(Duration.ofSeconds(10)).socketTimeout(Duration.ofSeconds(30)))
            .build();
    }
}
