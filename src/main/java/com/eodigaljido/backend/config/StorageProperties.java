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
        endpoint = clean(firstNonBlank(
                endpoint,
                env("SUPABASE_S3_ENDPOINT"), env("\uFEFFSUPABASE_S3_ENDPOINT"),
                env("STORAGE_S3_ENDPOINT"), env("\uFEFFSTORAGE_S3_ENDPOINT")
        ));
        region = clean(firstNonBlank(
                region,
                env("SUPABASE_S3_REGION"), env("\uFEFFSUPABASE_S3_REGION"),
                env("STORAGE_S3_REGION"), env("\uFEFFSTORAGE_S3_REGION")
        ));
        bucket = clean(firstNonBlank(
                bucket,
                env("SUPABASE_BUCKET"), env("\uFEFFSUPABASE_BUCKET"),
                env("STORAGE_S3_BUCKET"), env("\uFEFFSTORAGE_S3_BUCKET")
        ));
        accessKey = clean(firstNonBlank(
                accessKey,
                env("SUPABASE_S3_ACCESS_KEY"), env("\uFEFFSUPABASE_S3_ACCESS_KEY"),
                env("STORAGE_S3_ACCESS_KEY"), env("\uFEFFSTORAGE_S3_ACCESS_KEY")
        ));
        secretKey = clean(firstNonBlank(
                secretKey,
                env("SUPABASE_S3_SECRET_KEY"), env("\uFEFFSUPABASE_S3_SECRET_KEY"),
                env("STORAGE_S3_SECRET_KEY"), env("\uFEFFSTORAGE_S3_SECRET_KEY")
        ));
        publicBaseUrl = clean(firstNonBlank(
                publicBaseUrl,
                env("SUPABASE_PUBLIC_BASE_URL"), env("\uFEFFSUPABASE_PUBLIC_BASE_URL"),
                env("STORAGE_PUBLIC_BASE_URL"), env("\uFEFFSTORAGE_PUBLIC_BASE_URL")
        ));
        cacheControl = clean(firstNonBlank(
                cacheControl,
                env("SUPABASE_S3_CACHE_CONTROL"), env("\uFEFFSUPABASE_S3_CACHE_CONTROL"),
                env("STORAGE_S3_CACHE_CONTROL"), env("\uFEFFSTORAGE_S3_CACHE_CONTROL")
        ));

        if (region == null || region.isBlank()) {
            region = "ap-northeast-2";
        }
        if ((publicBaseUrl == null || publicBaseUrl.isBlank())
                && endpoint != null && !endpoint.isBlank()
                && bucket != null && !bucket.isBlank()) {
            publicBaseUrl = derivePublicBaseUrl(endpoint, bucket);
        }
        if (cacheControl == null || cacheControl.isBlank()) {
            cacheControl = "public, max-age=31536000";
        }
    }

    public void validateConfigured() {
        require(endpoint, "SUPABASE_S3_ENDPOINT");
        require(bucket, "SUPABASE_BUCKET");
        require(accessKey, "SUPABASE_S3_ACCESS_KEY");
        require(secretKey, "SUPABASE_S3_SECRET_KEY");
        require(publicBaseUrl, "SUPABASE_PUBLIC_BASE_URL or derived public URL");
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

    private static String env(String name) {
        return System.getenv(name);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String clean(String value) {
        if (value == null) return null;
        String cleaned = value.strip();
        if (!cleaned.isEmpty() && cleaned.charAt(0) == '\uFEFF') {
            cleaned = cleaned.substring(1).strip();
        }
        if ((cleaned.startsWith("\"") && cleaned.endsWith("\""))
                || (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).strip();
        }
        return cleaned;
    }

    private static String derivePublicBaseUrl(String endpoint, String bucket) {
        String base = trimTrailingSlash(endpoint);
        String s3Suffix = "/storage/v1/s3";
        if (base.endsWith(s3Suffix)) {
            base = base.substring(0, base.length() - s3Suffix.length());
        }
        base = base.replace(".storage.supabase.co", ".supabase.co");
        return base + "/storage/v1/object/public/" + bucket;
    }

    private static String trimTrailingSlash(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }
}
