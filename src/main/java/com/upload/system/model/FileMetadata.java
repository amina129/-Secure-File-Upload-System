package com.upload.system.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FileMetadata {
    private String fileId;
    private String originalFilename;
    private String storedPath;
    private String fileHash;
    private long size;
    private LocalDateTime uploadTime;
    private LocalDateTime lastAccessTime;
    private int uploadCount;
    private String mimeType;
}