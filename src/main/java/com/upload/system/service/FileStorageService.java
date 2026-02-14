package com.upload.system.service;

import com.upload.system.config.FileUploadConfig;
import com.upload.system.model.FileMetadata;
import com.upload.system.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {
    private final FileUploadConfig config;
    private final FileMetadataRepository metadataRepository;
    private final FileValidationService validationService;

    private Path uploadPath;

    @PostConstruct
    public void init() {
        uploadPath = Paths.get(config.getUploadDir());
        try {
            Files.createDirectories(uploadPath);
            log.info("Upload directory created: {}", uploadPath);
        } catch (IOException e) {
            log.error("Could not create upload directory", e);
        }
    }

    public UploadResult storeFile(MultipartFile file) throws IOException {
        // Validate file first
        FileValidationService.ValidationResult validationResult =
                validationService.validateFile(file);

        if (!validationResult.isValid()) {
            return UploadResult.error(validationResult.getErrorMessage());
        }

        // Calculate file hash for deduplication
        String fileHash;
        try (InputStream is = file.getInputStream()) {
            fileHash = DigestUtils.sha256Hex(is);
        }

        // Check for duplicate
        FileMetadata existing = metadataRepository.findByHash(fileHash);
        if (existing != null) {
            // Update existing metadata
            existing.setLastAccessTime(LocalDateTime.now());
            existing.setUploadCount(existing.getUploadCount() + 1); // Using uploadCount from your model
            metadataRepository.save(existing);

            log.info("Duplicate file detected: {} (hash: {})",
                    file.getOriginalFilename(), fileHash.substring(0, 8));

            return UploadResult.duplicate(existing.getFileId());
        }

        // Generate file ID
        String fileId = generateFileId(file.getOriginalFilename(), fileHash);

        // Create date-based subdirectory
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String extension = getExtension(file.getOriginalFilename());
        Path targetLocation = uploadPath
                .resolve(datePath)
                .resolve(fileHash + extension);

        Files.createDirectories(targetLocation.getParent());

        // Save file
        try (InputStream is = file.getInputStream()) {
            Files.copy(is, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        }

        // Create metadata (matching your model)
        FileMetadata metadata = new FileMetadata();
        metadata.setFileId(fileId);
        metadata.setOriginalFilename(file.getOriginalFilename());
        metadata.setStoredPath(uploadPath.relativize(targetLocation).toString());
        metadata.setFileHash(fileHash);
        metadata.setSize(file.getSize()); // Using size from your model
        metadata.setUploadTime(LocalDateTime.now());
        metadata.setLastAccessTime(LocalDateTime.now());
        metadata.setUploadCount(1); // Using uploadCount from your model
        metadata.setMimeType(validationResult.getMimeType());

        metadataRepository.save(metadata);

        log.info("New file stored: {} (ID: {})", file.getOriginalFilename(), fileId);

        return UploadResult.success(fileId, false);
    }

    private String generateFileId(String originalFilename, String fileHash) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("file_%s_%s", timestamp, fileHash.substring(0, 8));
    }

    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : "";
    }

    public void deleteFile(String fileHash) {
        FileMetadata metadata = metadataRepository.findByHash(fileHash);
        if (metadata != null) {
            try {
                Path filePath = uploadPath.resolve(metadata.getStoredPath());
                Files.deleteIfExists(filePath);

                // Try to delete empty parent directories
                deleteEmptyParentDirectories(filePath.getParent());

                metadataRepository.remove(fileHash);
                log.info("Deleted file: {}", filePath);
            } catch (IOException e) {
                log.error("Error deleting file: {}", metadata.getStoredPath(), e);
            }
        }
    }

    private void deleteEmptyParentDirectories(Path dir) throws IOException {
        while (dir != null && !dir.equals(uploadPath)) {
            if (Files.list(dir).findAny().isEmpty()) {
                Files.delete(dir);
                dir = dir.getParent();
            } else {
                break;
            }
        }
    }

    @lombok.Data
    public static class UploadResult {
        private final boolean success;
        private final String fileId;
        private final boolean duplicate;
        private final String errorMessage;

        public static UploadResult success(String fileId, boolean duplicate) {
            return new UploadResult(true, fileId, duplicate, null);
        }

        public static UploadResult duplicate(String fileId) {
            return new UploadResult(true, fileId, true, null);
        }

        public static UploadResult error(String errorMessage) {
            return new UploadResult(false, null, false, errorMessage);
        }

        private UploadResult(boolean success, String fileId, boolean duplicate, String errorMessage) {
            this.success = success;
            this.fileId = fileId;
            this.duplicate = duplicate;
            this.errorMessage = errorMessage;
        }
    }
}