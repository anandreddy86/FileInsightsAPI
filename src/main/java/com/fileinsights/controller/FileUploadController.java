package com.fileinsights.controller;

import com.fileinsights.service.FileMetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/upload")
public class FileUploadController {

    @Autowired
    private FileMetadataService fileMetadataService;

    @PostMapping
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // Check if the file is empty
            if (file.isEmpty()) {
                return ResponseEntity.status(400).body("File is empty");
            }

            // Process the file and extract metadata
            fileMetadataService.processFile(file);

            return ResponseEntity.ok("File uploaded and metadata extracted successfully");
        } catch (Exception e) {
            // Catch general Exception since processFile may throw Exception
            return ResponseEntity.status(500).body("Failed to process file: " + e.getMessage());
        }
    }
}
