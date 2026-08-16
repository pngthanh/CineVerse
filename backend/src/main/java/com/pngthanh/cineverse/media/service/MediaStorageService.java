package com.pngthanh.cineverse.media.service;

import com.pngthanh.cineverse.common.exception.ApiException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaStorageService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "avif");
    private final Path uploadDirectory;

    public MediaStorageService(@Value("${app.media.upload-dir:uploads/movies}") String uploadDirectory) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_IMAGE", "Vui lòng chọn ảnh để tải lên.");
        }
        String original = file.getOriginalFilename() == null ? "image.jpg" : file.getOriginalFilename();
        String extension = extensionOf(original);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IMAGE_TYPE", "Chỉ hỗ trợ JPG, PNG, WEBP hoặc AVIF.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IMAGE_TYPE", "Tệp tải lên phải là hình ảnh.");
        }
        try {
            Files.createDirectories(uploadDirectory);
            String filename = UUID.randomUUID() + "." + extension;
            Path target = uploadDirectory.resolve(filename).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/movies/" + filename;
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE_UPLOAD_FAILED", "Không thể lưu ảnh tải lên.");
        }
    }

    public Path uploadDirectory() {
        return uploadDirectory;
    }

    private String extensionOf(String filename) {
        int index = filename.lastIndexOf('.');
        return index < 0 ? "" : filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
