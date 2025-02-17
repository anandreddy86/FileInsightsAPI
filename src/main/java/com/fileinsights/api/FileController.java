package com.fileinsights.api;

import com.fileinsights.entity.FileMetadata;
import com.fileinsights.entity.TikaMetadata;
import com.fileinsights.service.FileMetadataService;
import com.fileinsights.service.FileService;
import com.fileinsights.service.ElasticsearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileService fileService;

    @Autowired
    private FileMetadataService fileMetadataService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    /**
     * Endpoint to process a folder and extract metadata.
     *
     * @param folderPath Path to the folder to process.
     * @param pathType   The path type: "Local", "NFS", or "SMB".
     * @return ResponseEntity with a success message.
     */
    @PostMapping("/process")
    public ResponseEntity<String> processFolder(@RequestParam String folderPath, 
                                                @RequestParam(defaultValue = "Local") String pathType) {
        File folder = new File(folderPath);

        if (folder.exists() && folder.isDirectory()) {
            try {
                fileService.processFolder(folder, pathType); // Process and store metadata
                return ResponseEntity.ok("Folder processed successfully.");
            } catch (Exception e) {
                logger.error("Error processing folder: {}", folderPath, e);
                return ResponseEntity.status(500).body("Error processing folder: " + e.getMessage());
            }
        } else {
            logger.warn("Invalid folder path: {}", folderPath);
            return ResponseEntity.badRequest().body("Invalid folder path.");
        }
    }

    /**
     * Endpoint to get metadata (Basic or Advanced) of all files in a folder.
     *
     * @param folderPath Path to the folder whose metadata to retrieve.
     * @param type       Metadata type: "basic" (MySQL) or "advanced" (Elasticsearch).
     * @param pathType   The path type: "Local", "NFS", or "SMB".
     * @return ResponseEntity with the metadata.
     */
    @GetMapping("/metadata")
    public ResponseEntity<?> getFolderMetadata(@RequestParam String folderPath,
                                               @RequestParam(defaultValue = "basic") String type,
                                               @RequestParam(defaultValue = "Local") String pathType) {
        try {
            if ("advanced".equalsIgnoreCase(type)) {
                logger.info("Fetching advanced metadata for folder: {}", folderPath);
                List<TikaMetadata> advancedMetadata = fileService.getAdvancedMetadata(folderPath, pathType);
                if (advancedMetadata != null && !advancedMetadata.isEmpty()) {
                    return ResponseEntity.ok(advancedMetadata);
                } else {
                    logger.info("No advanced metadata found for folder: {}", folderPath);
                    return ResponseEntity.notFound().build();
                }
            } else {
                logger.info("Fetching basic metadata for folder: {}", folderPath);
                List<FileMetadata> basicMetadata = fileMetadataService.getMetadataForFolder(folderPath, pathType);
                if (basicMetadata != null && !basicMetadata.isEmpty()) {
                    return ResponseEntity.ok(basicMetadata);
                } else {
                    logger.info("No basic metadata found for folder: {}", folderPath);
                    return ResponseEntity.notFound().build();
                }
            }
        } catch (Exception e) {
            logger.error("Error retrieving metadata for folder: {}", folderPath, e);
            return ResponseEntity.status(500).body("Error retrieving metadata: " + e.getMessage());
        }
    }

    /**
     * Endpoint to reset the index for a folder path.
     *
     * @param folderPath Path to the folder whose metadata needs to be cleared.
     * @return ResponseEntity with a success or error message.
     */
    @DeleteMapping("/reset-index")
    public ResponseEntity<String> resetIndex(@RequestParam String folderPath) {
        try {
            // Delete from MySQL
            logger.info("Resetting MySQL metadata for folder: {}", folderPath);
            fileMetadataService.deleteMetadataForFolder(folderPath);

            // Delete from Elasticsearch
            logger.info("Resetting Elasticsearch metadata for folder: {}", folderPath);
            elasticsearchService.deleteByPath(folderPath);

            return ResponseEntity.ok("Index reset successfully for folder: " + folderPath);
        } catch (Exception e) {
            logger.error("Error resetting index for folder: {}", folderPath, e);
            return ResponseEntity.status(500).body("Error resetting index: " + e.getMessage());
        }
    }
}
