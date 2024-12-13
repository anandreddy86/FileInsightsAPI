package com.fileinsights.controller;

import com.fileinsights.entity.FileMetadata;
import com.fileinsights.service.FileMetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

//import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/metadata")
public class FileMetadataController {

    private static final Logger logger = LoggerFactory.getLogger(FileMetadataController.class);

    @Autowired
    private FileMetadataService fileMetadataService;

    /**
     * Endpoint to save file metadata.
     *
     * @param fileMetadata The file metadata to save.
     * @return ResponseEntity indicating the result of the operation.
     */
    @PostMapping("/save")
    public ResponseEntity<?> saveMetadata(@Valid @RequestBody FileMetadata fileMetadata) {
        try {
            fileMetadataService.saveFileMetadata(fileMetadata);
            logger.info("File metadata saved successfully: {}", fileMetadata);
            return ResponseEntity.ok("File metadata saved successfully.");
        } catch (Exception e) {
            logger.error("Error saving file metadata: {}", fileMetadata, e);
            return ResponseEntity.status(500).body("Error saving metadata: " + e.getMessage());
        }
    }

    /**
     * Endpoint to retrieve file metadata by ID.
     *
     * @param id The ID of the metadata to retrieve.
     * @return The file metadata with the specified ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getMetadata(@PathVariable Long id) {
        try {
            FileMetadata fileMetadata = fileMetadataService.getMetadata(id);
            if (fileMetadata != null) {
                logger.info("File metadata retrieved for ID {}: {}", id, fileMetadata);
                return ResponseEntity.ok(fileMetadata);
            } else {
                logger.warn("File metadata not found for ID: {}", id);
                return ResponseEntity.status(404).body("File metadata not found for ID: " + id);
            }
        } catch (Exception e) {
            logger.error("Error retrieving metadata for ID: {}", id, e);
            return ResponseEntity.status(500).body("Error retrieving metadata: " + e.getMessage());
        }
    }

    /**
     * Endpoint to retrieve all file metadata with pagination.
     *
     * @param page The page number (default is 0).
     * @param size The size of the page (default is 10).
     * @return List of file metadata.
     */
    @GetMapping
    public ResponseEntity<?> getAllMetadata(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            List<FileMetadata> metadataList = fileMetadataService.getAllMetadata(page, size);
            if (!metadataList.isEmpty()) {
                logger.info("Retrieved {} metadata records for page {} with size {}", metadataList.size(), page, size);
                return ResponseEntity.ok(metadataList);
            } else {
                logger.warn("No metadata records found for page {} with size {}", page, size);
                return ResponseEntity.ok(Collections.emptyList());
            }
        } catch (Exception e) {
            logger.error("Error retrieving all metadata for page {} with size {}", page, size, e);
            return ResponseEntity.status(500).body("Error retrieving metadata: " + e.getMessage());
        }
    }
}
