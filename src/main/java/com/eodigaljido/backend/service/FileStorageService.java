package com.eodigaljido.backend.service;

import com.eodigaljido.backend.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    private final StorageProperties storageProperties;

    public String store(MultipartFile file, String subDir, String filename) throws IOException {
        validateImageFile(file);
        storageProperties.validateConfigured();

        byte[] bytes = file.getBytes();
        String ext = extractExtension(file.getOriginalFilename());
        String objectKey = normalizeObjectKey(subDir + "/" + filename + "." + ext);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(storageProperties.bucket())
                .key(objectKey)
                .contentType(file.getContentType())
                .cacheControl(storageProperties.cacheControl())
                .contentLength((long) bytes.length)
                .build();

        try (S3Client s3Client = createS3Client()) {
            s3Client.putObject(request, RequestBody.fromBytes(bytes));
        }
        return storageProperties.publicUrl(objectKey);
    }

    public void delete(String fileUrl) {
        try {
            storageProperties.validateConfigured();
            String objectKey = objectKeyFromUrl(fileUrl);
            if (objectKey == null) return;

            try (S3Client s3Client = createS3Client()) {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(storageProperties.bucket())
                        .key(objectKey)
                        .build());
            }
        } catch (Exception ignored) {
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어 있습니다.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다. (JPEG, PNG, GIF, WebP)");
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private S3Client createS3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(storageProperties.endpoint()))
                .region(Region.of(storageProperties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(storageProperties.accessKey(), storageProperties.secretKey())
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(storageProperties.pathStyleAccessEnabled())
                        .chunkedEncodingEnabled(false)
                        .build())
                .build();
    }

    private String objectKeyFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return null;

        String publicBaseUrl = storageProperties.publicBaseUrl();
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            String normalizedBaseUrl = trimTrailingSlash(publicBaseUrl) + "/";
            if (fileUrl.startsWith(normalizedBaseUrl)) {
                return normalizeObjectKey(fileUrl.substring(normalizedBaseUrl.length()));
            }
        }

        try {
            URI uri = URI.create(fileUrl);
            String path = uri.getPath();
            String bucketSegment = "/" + storageProperties.bucket() + "/";
            int bucketIndex = path.indexOf(bucketSegment);
            if (bucketIndex >= 0) {
                return normalizeObjectKey(path.substring(bucketIndex + bucketSegment.length()));
            }
        } catch (IllegalArgumentException ignored) {
        }

        return null;
    }

    private String normalizeObjectKey(String key) {
        return key.replace('\\', '/').replaceAll("/{2,}", "/").replaceAll("^/+", "");
    }

    private String trimTrailingSlash(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }
}
