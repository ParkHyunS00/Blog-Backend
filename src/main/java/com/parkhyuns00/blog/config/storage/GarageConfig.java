package com.parkhyuns00.blog.config.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(GarageProperties.class)
public class GarageConfig {

    @Bean
    public S3Client s3Client(GarageProperties properties) {
        return S3Client.builder()
            .endpointOverride(URI.create(properties.endpoint()))
            .region(Region.of("auto"))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())
            ))
            .forcePathStyle(true)
            .build();
    }
}
