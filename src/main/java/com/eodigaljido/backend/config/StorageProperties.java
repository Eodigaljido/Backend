package com.eodigaljido.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.s3")
public record StorageProperties(
        String endpoint,
        String region,
        String bucket,
        String accessKey,
        String secretKey,
        String publicBaseUrl,
        boolean pathStyleAccessEnabled,
        String cacheControl
) {
    public StorageProperties {
        if (region == null || region.isBlank()) {
            region = "ap-northeast-2";
        }
        if (cacheControl == null || cacheControl.isBlank()) {
            cacheControl = "public, max-age=31536000";
        }
    }

    public void validateConfigured() {
        require(endpoint, "storage.s3.endpoint");
        require(bucket, "storage.s3.bucket");
        require(accessKey, "storage.s3.access-key");
        require(secretKey, "storage.s3.secret-key");
        require(publicBaseUrl, "storage.s3.public-base-url");
    }

    public String publicUrl(String objectKey) {
        validateConfigured();
        return trimTrailingSlash(publicBaseUrl) + "/" + objectKey;
    }

    private static void require(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " must be configured for file uploads.");
        }
    }

    private static String trimTrailingSlash(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }
}
