package com.upload.system.service;

import com.upload.system.config.FileUploadConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
public class FileValidationService {

    private final FileUploadConfig config;
    private final Tika tika;
    private final Set<String> allowedExtensions;
    private final Set<String> allowedMimeTypes;

    public FileValidationService(FileUploadConfig config) {
        this.config = config;
        this.tika = new Tika();

        // Convert arrays to Sets
        this.allowedExtensions = new HashSet<>(Arrays.asList(config.getAllowedExtensions()));
        this.allowedMimeTypes = new HashSet<>(Arrays.asList(config.getAllowedMimeTypes()));
    }

    public ValidationResult validateFile(MultipartFile file) {
        // Check if file is empty
        if (file.isEmpty()) {
            return ValidationResult.invalid("File is empty");
        }

        // Check file size
        if (file.getSize() > config.getMaxFileSize()) {
            return ValidationResult.invalid(String.format(
                    "File size exceeds %dMB limit (Size: %.2fMB)",
                    config.getMaxFileSize() / (1024 * 1024),
                    file.getSize() / (1024.0 * 1024.0)
            ));
        }

        // Get file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            return ValidationResult.invalid("Invalid filename");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!allowedExtensions.contains(extension)) {
            return ValidationResult.invalid(String.format(
                    "File extension '%s' not allowed. Allowed: %s",
                    extension, allowedExtensions
            ));
        }

        // Check MIME type
        try {
            String mimeType = tika.detect(file.getInputStream());

            if (!allowedMimeTypes.contains(mimeType)) {
                return ValidationResult.invalid(String.format(
                        "File type '%s' not allowed. Only images are accepted",
                        mimeType
                ));
            }

            // Verify extension matches MIME type
            if (!isValidExtensionForMimeType(extension, mimeType)) {
                return ValidationResult.invalid(String.format(
                        "File extension '%s' does not match actual file type '%s'",
                        extension, mimeType
                ));
            }

            return ValidationResult.valid(mimeType, file.getSize());

        } catch (IOException e) {
            log.error("Error detecting file type: {}", e.getMessage());
            return ValidationResult.invalid("Error validating file");
        }
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : "";
    }

    private boolean isValidExtensionForMimeType(String extension, String mimeType) {
        return switch (mimeType) {
            case "image/jpeg" -> extension.equals(".jpg") || extension.equals(".jpeg");
            case "image/png" -> extension.equals(".png");
            case "image/gif" -> extension.equals(".gif");
            case "image/webp" -> extension.equals(".webp");
            default -> false;
        };
    }

    @lombok.Data
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final String mimeType;
        private final long fileSize;

        public static ValidationResult valid(String mimeType, long fileSize) {
            return new ValidationResult(true, null, mimeType, fileSize);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage, null, 0);
        }

        private ValidationResult(boolean valid, String errorMessage, String mimeType, long fileSize) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.mimeType = mimeType;
            this.fileSize = fileSize;
        }
    }
}