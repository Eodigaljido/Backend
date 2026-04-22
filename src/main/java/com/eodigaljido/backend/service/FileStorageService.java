package com.eodigaljido.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    public String store(MultipartFile file, String subDir, String filename) throws IOException {
        validateImageFile(file);

        Path dir = Paths.get(uploadDir, subDir).toAbsolutePath();
        Files.createDirectories(dir);

        String ext = extractExtension(file.getOriginalFilename());
        String storedFilename = filename + "." + ext;
        Files.copy(file.getInputStream(), dir.resolve(storedFilename), StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/" + subDir + "/" + storedFilename;
    }

    public void delete(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) return;
        String relativePath = fileUrl.substring("/uploads/".length());
        Path target = Paths.get(uploadDir).toAbsolutePath().resolve(relativePath);
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {}
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
}
