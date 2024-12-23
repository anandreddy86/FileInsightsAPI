package com.fileinsights.controller;

import com.fileinsights.service.FileMetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

@RestController
@RequestMapping("/upload")
public class FileUploadController {

    @Autowired
    private FileMetadataService fileMetadataService;

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    /**
     * Endpoint to handle single file uploads.
     * 
     * @param file MultipartFile uploaded by the user.
     * @return ResponseEntity with success or error message.
     */
    @PostMapping
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // Check if the file is empty
            if (file.isEmpty()) {
                return ResponseEntity.status(400).body("File is empty");
            }

            // Extract the original file name
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || originalFileName.isEmpty()) {
                return ResponseEntity.status(400).body("Invalid file name.");
            }

            // Process the file and extract metadata
            fileMetadataService.processFile(file, originalFileName);

            return ResponseEntity.ok("File uploaded and metadata extracted successfully");
        } catch (Exception e) {
            // Log the error details
            logger.error("Error processing file upload", e);

            // Catch general Exception since processFile may throw Exception
            return ResponseEntity.status(500).body("Failed to process file: " + e.getMessage());
        }
    }

    /**
     * New endpoint to handle folder path uploads.
     * 
     * @param folderPath Path of the folder to process.
     * @return ResponseEntity with success or error message.
     */
    @PostMapping("/folder")
    public ResponseEntity<String> uploadFolder(@RequestParam("folderPath") String folderPath) {
        try {
            // Validate the folder path
            File folder = new File(folderPath);
            if (!folder.exists() || !folder.isDirectory()) {
                return ResponseEntity.status(400).body("Invalid folder path.");
            }

            // Process the folder and extract metadata for all files inside the folder
            fileMetadataService.processFolder(folder);

            return ResponseEntity.ok("Folder processed and metadata extracted successfully.");
        } catch (Exception e) {
            // Log the error details
            logger.error("Error processing folder upload", e);

            // Catch general Exception since processFolder may throw Exception
            return ResponseEntity.status(500).body("Failed to process folder: " + e.getMessage());
        }
    }
}
