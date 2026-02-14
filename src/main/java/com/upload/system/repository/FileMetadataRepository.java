package com.upload.system.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upload.system.model.FileMetadata;
import jakarta.annotation.PostConstruct;  // Changed from javax to jakarta for Spring Boot 3+
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;  // Fixed typo: javva -> java

@Slf4j
@Repository
public class FileMetadataRepository {
    private final Map<String, FileMetadata> metadataStore = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${upload.metadata-file:file-metadata.json}")
    private String metadataFilePath;

    @PostConstruct
    public void init() {
        loadMetadata();
    }

    public void save(FileMetadata metadata) {
        metadataStore.put(metadata.getFileHash(), metadata);
        persistMetadata();
    }

    public FileMetadata findByHash(String hash) {
        return metadataStore.get(hash);
    }

    public Map<String, FileMetadata> findAll() {
        return new ConcurrentHashMap<>(metadataStore);
    }

    public void remove(String hash) {
        metadataStore.remove(hash);
        persistMetadata();
    }

    private void loadMetadata() {
        try {
            File metadataFile = new File(metadataFilePath);
            if (metadataFile.exists()) {
                metadataStore.putAll(objectMapper.readValue(
                        metadataFile,
                        new TypeReference<Map<String, FileMetadata>>() {}
                ));
                log.info("Loaded {} metadata records", metadataStore.size());
            }
        } catch (IOException e) {
            log.error("Failed to load metadata", e);
        }
    }

    private void persistMetadata() {
        try {
            // Ensure directory exists
            File metadataFile = new File(metadataFilePath);
            File parentDir = metadataFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            objectMapper.writeValue(metadataFile, metadataStore);
            log.debug("Persisted {} metadata records", metadataStore.size());
        } catch (IOException e) {
            log.error("Failed to persist metadata", e);
        }
    }
}