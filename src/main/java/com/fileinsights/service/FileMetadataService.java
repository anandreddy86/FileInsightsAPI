package com.fileinsights.service;

import com.fileinsights.entity.FileMetadata;
import com.fileinsights.repository.FileMetadataRepository;
import com.fileinsights.util.TikaUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

@Service
public class FileMetadataService {

    private static final Logger logger = LoggerFactory.getLogger(FileMetadataService.class);

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private ElasticsearchService elasticsearchService;

    /**
     * Processes the uploaded file, extracts metadata, and saves it to MySQL and Elasticsearch.
     *
     * @param file             The uploaded file.
     * @param originalFileName The original file name.
     * @throws Exception If file processing fails.
     */
    public void processFile(MultipartFile file, String originalFileName) throws Exception {
        Path tempFile = saveTempFile(file);

        try {
            processAndSaveFile(tempFile.toFile(), originalFileName);
        } finally {
            Files.deleteIfExists(tempFile); // Clean up temporary file
        }
    }

    /**
     * Processes a folder and extracts metadata for all files inside it.
     *
     * @param folder The folder to process.
     * @throws Exception If an error occurs during processing.
     */
    public void processFolder(File folder) throws Exception {
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        try {
                            processAndSaveFile(file, file.getName()); // Process each file
                        } catch (Exception e) {
                            logger.error("Error processing file: {}", file.getAbsolutePath(), e);
                        }
                    } else if (file.isDirectory()) {
                        processFolder(file); // Recursively process subfolders
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Invalid folder path: " + folder.getAbsolutePath());
        }
    }

    /**
     * Processes and saves a file's metadata to both MySQL and Elasticsearch.
     *
     * @param file             The file to process.
     * @param originalFileName The original file name.
     * @throws Exception If an error occurs.
     */
    private void processAndSaveFile(File file, String originalFileName) throws Exception {
        try {
            // Extract basic metadata
            FileMetadata fileMetadata = extractMetadata(file, originalFileName);
            saveFileMetadata(fileMetadata); // Save to MySQL

            // Extract advanced metadata with Tika
            var tikaMetadata = TikaUtils.extractTikaMetadata(file, originalFileName);
            elasticsearchService.saveTikaMetadata(tikaMetadata); // Save to Elasticsearch
        } catch (Exception e) {
            logger.error("Error processing file: {}", file.getAbsolutePath(), e);
            throw e;
        }
    }

    /**
     * Saves the uploaded file temporarily for processing.
     *
     * @param file The uploaded file.
     * @return The path to the temporary file.
     * @throws IOException If saving the file fails.
     */
    private Path saveTempFile(MultipartFile file) throws IOException {
        Path tempFilePath = Files.createTempFile("upload-", file.getOriginalFilename());
        file.transferTo(tempFilePath.toFile());
        return tempFilePath;
    }

    /**
     * Extracts metadata from a file.
     *
     * @param file             The file to extract metadata from.
     * @param originalFileName The original file name.
     * @return Extracted file metadata.
     */
    public FileMetadata extractMetadata(File file, String originalFileName) {
        FileMetadata metadata = new FileMetadata();
        metadata.setName(originalFileName);
        metadata.setSize(file.length());

        // Convert long to Date for ctime, mtime, and atime
        Date fileDate = new Date(file.lastModified());
        metadata.setCtime(fileDate);
        metadata.setMtime(fileDate);
        metadata.setAtime(fileDate);

        // Set the path
        metadata.setPath(file.getAbsolutePath());

        return metadata;
    }

    /**
     * Saves file metadata to the repository (MySQL).
     *
     * @param fileMetadata The file metadata to save.
     */
    public void saveFileMetadata(FileMetadata fileMetadata) {
        fileMetadataRepository.save(fileMetadata);
    }

    /**
     * Retrieves file metadata by its ID.
     *
     * @param id The ID of the metadata to retrieve.
     * @return The file metadata with the specified ID, or null if not found.
     */
    public FileMetadata getMetadata(Long id) {
        return fileMetadataRepository.findById(id).orElse(null);
    }

    /**
     * Retrieves metadata by file path.
     *
     * @param filePath The file path to search for.
     * @return FileMetadata object or null if not found.
     */
    public FileMetadata getMetadataByPath(String filePath) {
        return fileMetadataRepository.findByPath(filePath);
    }

    /**
     * Deletes metadata from both MySQL and Elasticsearch by ID.
     *
     * @param id The ID of the metadata to delete.
     * @throws Exception If deletion fails.
     */
    public void deleteMetadata(Long id) throws Exception {
        FileMetadata fileMetadata = fileMetadataRepository.findById(id).orElse(null);

        if (fileMetadata != null) {
            try {
                // Delete from MySQL
                fileMetadataRepository.deleteById(id);
                logger.info("Deleted metadata from MySQL with ID: {}", id);

                // Delete from Elasticsearch
                elasticsearchService.deleteMetadataByFilePath(fileMetadata.getPath());
                logger.info("Deleted metadata from Elasticsearch for path: {}", fileMetadata.getPath());
            } catch (Exception e) {
                logger.error("Error deleting metadata with ID: {}", id, e);
                throw e;
            }
        } else {
            logger.warn("Metadata with ID: {} not found for deletion.", id);
        }
    }

    /**
     * Deletes all metadata associated with files in a specific folder.
     *
     * @param folderPath The folder path to delete metadata from.
     * @throws Exception If deletion fails.
     */
    public void deleteMetadataForFolder(String folderPath) throws Exception {
        File folder = new File(folderPath);

        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        FileMetadata metadata = getMetadataByPath(file.getAbsolutePath());
                        if (metadata != null) {
                            deleteMetadata(metadata.getId());
                        }
                    } else if (file.isDirectory()) {
                        deleteMetadataForFolder(file.getAbsolutePath()); // Recursive deletion for subfolders
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Provided path is not a valid directory: " + folderPath);
        }
    }

    /**
     * Retrieves metadata for files in a folder.
     *
     * @param folderPath The path of the folder.
     * @return List of FileMetadata objects.
     */
    public List<FileMetadata> getMetadataForFolder(String folderPath) {
        return fileMetadataRepository.findByPathStartingWith(folderPath);
    }

    /**
     * Retrieves all file metadata with pagination.
     *
     * @param page The page number (0-based index).
     * @param size The size of the page.
     * @return A list of FileMetadata objects.
     */
    public List<FileMetadata> getAllMetadata(int page, int size) {
        try {
            PageRequest pageRequest = PageRequest.of(page, size);
            Page<FileMetadata> metadataPage = fileMetadataRepository.findAll(pageRequest);
            return metadataPage.getContent();
        } catch (Exception e) {
            logger.error("Error retrieving metadata with pagination for page {} and size {}", page, size, e);
            throw new RuntimeException("Error retrieving metadata with pagination: " + e.getMessage());
        }
    }
}
