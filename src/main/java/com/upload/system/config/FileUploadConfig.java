package com.upload.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "upload")
public class FileUploadConfig {
    private long maxFileSize = 5 * 1024 * 1024; // 5MB default
    private String uploadDir = "uploads";
    private String metadataFile = "file-metadata.json";
    private int retentionHours = 24;

    // Allowed file types
    private String[] allowedExtensions = {".jpg", ".jpeg", ".png", ".gif", ".webp"};
    private String[] allowedMimeTypes = {
            "image/jpeg", "image/png", "image/gif", "image/webp"
    };
}