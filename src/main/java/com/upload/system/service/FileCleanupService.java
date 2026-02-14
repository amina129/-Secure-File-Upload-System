package com.upload.system.service;

import com.upload.system.config.FileUploadConfig;
import com.upload.system.model.FileMetadata;
import com.upload.system.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileCleanupService {
    private final FileStorageService fileStorageService;
    private final FileMetadataRepository metadataRepository;
    private final FileUploadConfig config;

    @Scheduled(fixedRate = 3600000) // Run every hour
    public void cleanupOldFiles() {
        log.info("Starting scheduled cleanup of old files...");

        LocalDateTime cutoffTime = LocalDateTime.now()
                .minusHours(config.getRetentionHours());

        List<String> filesToDelete = new ArrayList<>();
        Map<String, FileMetadata> allFiles = metadataRepository.findAll();

        for (Map.Entry<String, FileMetadata> entry : allFiles.entrySet()) {
            FileMetadata metadata = entry.getValue();
            if (metadata.getUploadTime().isBefore(cutoffTime)) {
                filesToDelete.add(entry.getKey());
            }
        }

        for (String fileHash : filesToDelete) {
            fileStorageService.deleteFile(fileHash);
        }

        log.info("Cleanup completed. Deleted {} files.", filesToDelete.size());
    }

    // Manual cleanup trigger
    public int triggerManualCleanup() {
        cleanupOldFiles();
        return metadataRepository.findAll().size();
    }

    public CleanupStats getCleanupStats() {
        Map<String, FileMetadata> files = metadataRepository.findAll();

        long totalSize = files.values().stream()
                .mapToLong(FileMetadata::getSize)
                .sum();

        long totalUploads = files.values().stream()
                .mapToInt(FileMetadata::getUploadCount)
                .sum();

        CleanupStats stats = new CleanupStats();
        stats.setUniqueFiles(files.size());
        stats.setTotalSizeBytes(totalSize);
        stats.setTotalSizeMB(totalSize / (1024.0 * 1024.0));
        stats.setTotalUploads(totalUploads);

        return stats;
    }

    @lombok.Data
    public static class CleanupStats {
        private int uniqueFiles;
        private long totalSizeBytes;
        private double totalSizeMB;
        private long totalUploads;
    }
}