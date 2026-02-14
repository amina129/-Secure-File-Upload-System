package com.upload.system.controller;

import com.upload.system.service.FileCleanupService;
import com.upload.system.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class FileUploadController {
    private final FileStorageService fileStorageService;
    private final FileCleanupService cleanupService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("stats", cleanupService.getCleanupStats());
        return "upload";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file,
                             RedirectAttributes redirectAttributes) {
        try {
            FileStorageService.UploadResult result = fileStorageService.storeFile(file);

            if (result.isSuccess()) {
                String message = result.isDuplicate()
                        ? "File uploaded successfully (duplicate detected - storage reused)"
                        : "File uploaded successfully";

                redirectAttributes.addFlashAttribute("message", message);
                redirectAttributes.addFlashAttribute("fileId", result.getFileId());
                redirectAttributes.addFlashAttribute("isDuplicate", result.isDuplicate());
                redirectAttributes.addFlashAttribute("success", true);
            } else {
                redirectAttributes.addFlashAttribute("message", "Error: " + result.getErrorMessage());
                redirectAttributes.addFlashAttribute("success", false);
            }

        } catch (IOException e) {
            log.error("Upload error", e);
            redirectAttributes.addFlashAttribute("message", "Error uploading file");
            redirectAttributes.addFlashAttribute("success", false);
        }

        return "redirect:/";
    }

    @PostMapping("/api/upload")
    @ResponseBody
    public ResponseEntity<?> apiUploadFile(@RequestParam("file") MultipartFile file) {
        try {
            FileStorageService.UploadResult result = fileStorageService.storeFile(file);

            if (result.isSuccess()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("fileId", result.getFileId());
                response.put("isDuplicate", result.isDuplicate());
                response.put("message", "File uploaded successfully");

                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", result.getErrorMessage());

                return ResponseEntity.badRequest().body(error);
            }

        } catch (IOException e) {
            log.error("API upload error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<?> getStats() {
        return ResponseEntity.ok(cleanupService.getCleanupStats());
    }

    @PostMapping("/api/cleanup")
    @ResponseBody
    public ResponseEntity<?> triggerCleanup() {
        try {
            int remainingFiles = cleanupService.triggerManualCleanup();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cleanup completed",
                    "remainingFiles", remainingFiles
            ));
        } catch (Exception e) {
            log.error("Cleanup error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Cleanup failed"));
        }
    }
}