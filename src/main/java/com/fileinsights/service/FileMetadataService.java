package com.fileinsights.service;

import com.fileinsights.entity.FileMetadata;
import com.fileinsights.repository.FileMetadataRepository;
import com.fileinsights.util.TikaUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class FileMetadataService {

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private ElasticsearchService elasticsearchService;

    /**
     * Process the uploaded file, extract metadata, and save it to MySQL and Elasticsearch.
     *
     * @param file The uploaded file.
     * @throws Exception If file processing fails.
     */
    public void processFile(MultipartFile file) throws Exception {
        // Save the file temporarily
        Path tempFile = saveTempFile(file);

        try {
            // Extract basic metadata
            FileMetadata fileMetadata = extractMetadata(tempFile.toFile());
            fileMetadataRepository.save(fileMetadata); // Save to MySQL

            // Extract advanced metadata with Tika
            var tikaMetadata = TikaUtils.extractTikaMetadata(tempFile.toFile());
            elasticsearchService.saveTikaMetadata(tikaMetadata); // Save to Elasticsearch

        } finally {
            // Clean up temporary file
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * Save the uploaded file temporarily for processing.
     *
     * @param file The uploaded file.
     * @return The path to the temporary file.
     * @throws IOException If saving the file fails.
     */
    private Path saveTempFile(MultipartFile file) throws IOException {
        // Create a temporary file with the prefix "upload-" and the original filename
        Path tempFilePath = Files.createTempFile("upload-", file.getOriginalFilename());
        file.transferTo(tempFilePath.toFile()); // Transfer the content of the uploaded file
        return tempFilePath;
    }

    /**
     * Extract metadata from a file.
     *
     * @param file The file to extract metadata from.
     * @return Extracted file metadata.
     */
    public FileMetadata extractMetadata(File file) {
        FileMetadata metadata = new FileMetadata();
        metadata.setName(file.getName()); // Set file name
        metadata.setSize(file.length()); // Set file size
        metadata.setCtime(file.lastModified()); // Set file creation time (using lastModified as an example)
        metadata.setMtime(file.lastModified()); // Set file modification time
        metadata.setAtime(file.lastModified()); // Set file access time
        return metadata;
    }

    /**
     * Save file metadata to the repository (MySQL).
     *
     * @param fileMetadata The file metadata to save.
     */
    public void saveFileMetadata(FileMetadata fileMetadata) {
        fileMetadataRepository.save(fileMetadata); // Save metadata to MySQL
    }

    /**
     * Retrieve file metadata by its ID.
     *
     * @param id The ID of the metadata to retrieve.
     * @return The file metadata with the specified ID, or null if not found.
     */
    public FileMetadata getMetadata(Long id) {
        return fileMetadataRepository.findById(id).orElse(null); // Retrieve from MySQL
    }
}
