package com.fileinsights.api;

import com.fileinsights.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileService fileService;

    /**
     * Endpoint to process a folder and extract metadata.
     *
     * @param folderPath Path to the folder to process.
     * @return ResponseEntity with a success message.
     */
    @PostMapping("/process")
    public ResponseEntity<String> processFolder(@RequestParam String folderPath) {
        File folder = new File(folderPath);
        if (folder.exists() && folder.isDirectory()) {
            fileService.processFolder(folder);
            return ResponseEntity.ok("Folder processed successfully.");
        } else {
            return ResponseEntity.badRequest().body("Invalid folder path.");
        }
    }
}
