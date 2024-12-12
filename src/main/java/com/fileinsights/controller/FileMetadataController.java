package com.fileinsights.controller;

import com.fileinsights.entity.FileMetadata;
import com.fileinsights.service.FileMetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/metadata")
public class FileMetadataController {

    @Autowired
    private FileMetadataService fileMetadataService;

    /**
     * Endpoint to save file metadata.
     *
     * @param fileMetadata The file metadata to save.
     * @return ResponseEntity indicating the result of the operation.
     */
    @PostMapping("/save")
    public ResponseEntity<String> saveMetadata(@RequestBody FileMetadata fileMetadata) {
        try {
            fileMetadataService.saveFileMetadata(fileMetadata);
            return ResponseEntity.ok("File metadata saved successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error saving metadata.");
        }
    }

    /**
     * Endpoint to retrieve file metadata by ID.
     *
     * @param id The ID of the metadata to retrieve.
     * @return The file metadata with the specified ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<FileMetadata> getMetadata(@PathVariable Long id) {
        FileMetadata fileMetadata = fileMetadataService.getMetadata(id);
        if (fileMetadata != null) {
            return ResponseEntity.ok(fileMetadata);
        } else {
            return ResponseEntity.status(404).body(null);
        }
    }
}
