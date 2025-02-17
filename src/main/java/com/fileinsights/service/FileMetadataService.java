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
     * @param file The uploaded file.
     * @param originalFileName The original file name.
     * @throws Exception If file processing fails.
     */
    public void processFile(MultipartFile file, String originalFileName) throws Exception {
        Path tempFile = saveTempFile(file);

        try {
            processAndSaveFile(tempFile.toFile(), originalFileName, "Local"); // Assuming 'Local' as default path type
        } finally {
            Files.deleteIfExists(tempFile); // Clean up temporary file
        }
    }

    /**
     * Processes a folder and extracts metadata for all files inside it, including NFS and SMB shares.
     *
     * @param folder The folder to process.
     * @param pathType The type of the path (Local, NFS, SMB).
     * @throws Exception If an error occurs during processing.
     */
    public void processFolder(File folder, String pathType) throws Exception {
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        try {
                            processAndSaveFile(file, file.getName(), pathType); // Process each file with the given path type
                        } catch (Exception e) {
                            logger.error("Error processing file: {}", file.getAbsolutePath(), e);
                        }
                    } else if (file.isDirectory()) {
                        processFolder(file, pathType); // Recursively process subfolders
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Invalid folder path: " + folder.getAbsolutePath());
        }
    }

    /**
     * Processes and saves a file's metadata to both MySQL and Elasticsearch, considering path type.
     *
     * @param file             The file to process.
     * @param originalFileName The original file name.
     * @param pathType         The type of the path (Local, NFS, SMB).
     * @throws Exception If an error occurs.
     */
    private void processAndSaveFile(File file, String originalFileName, String pathType) throws Exception {
        try {
            // Extract basic metadata
            FileMetadata fileMetadata = extractMetadata(file, originalFileName, pathType);
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
     * Extracts metadata from a file, including path type (Local, NFS, SMB).
     *
     * @param file             The file to extract metadata from.
     * @param originalFileName The original file name.
     * @param pathType         The type of the path (Local, NFS, SMB).
     * @return Extracted file metadata.
     */
    public FileMetadata extractMetadata(File file, String originalFileName, String pathType) {
        FileMetadata metadata = new FileMetadata();
        metadata.setName(originalFileName);
        metadata.setSize(file.length());

        // Convert long to Date for ctime, mtime, and atime
        Date fileDate = new Date(file.lastModified());
        metadata.setCtime(fileDate);
        metadata.setMtime(fileDate);
        metadata.setAtime(fileDate);

        // Set the path and path type (NFS, SMB, or Local)
        metadata.setPath(file.getAbsolutePath());
        metadata.setType(pathType); // Store path type (Local, NFS, SMB)

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
     * Retrieves file metadata by its ID.
     *
     * @param id The ID of the metadata to retrieve.
     * @return The file metadata with the specified ID, or null if not found.
     */
    public FileMetadata getMetadata(Long id) {
        return fileMetadataRepository.findById(id).orElse(null);
    }

    /**
     * Retrieves file metadata by its path.
     *
     * @param filePath The file path to retrieve metadata for.
     * @return The file metadata associated with the provided path.
     */
    public FileMetadata getMetadataByPath(String filePath) {
        return fileMetadataRepository.findByPath(filePath); // Adjust this to your repository query
    }

    /**
     * Retrieves all file metadata with pagination, and filter by path type (NFS, SMB, Local).
     *
     * @param page The page number (0-based index).
     * @param size The size of the page.
     * @param type The type of path (Local, NFS, SMB).
     * @return A list of FileMetadata objects.
     */
    public List<FileMetadata> getAllMetadataByType(int page, int size, String type) {
        try {
            PageRequest pageRequest = PageRequest.of(page, size);
            Page<FileMetadata> metadataPage;
            if ("NFS".equalsIgnoreCase(type)) {
                metadataPage = fileMetadataRepository.findByTypeAndPathStartingWith("NFS", "/nfs", pageRequest);
            } else if ("SMB".equalsIgnoreCase(type)) {
                metadataPage = fileMetadataRepository.findByTypeAndPathStartingWith("SMB", "/smb", pageRequest);
            } else {
                metadataPage = fileMetadataRepository.findByType(type, pageRequest);
            }
            return metadataPage.getContent(); // Convert Page to List
        } catch (Exception e) {
            logger.error("Error retrieving metadata for path type: {}", type, e);
            throw new RuntimeException("Error retrieving metadata for path type: " + e.getMessage());
        }
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
            return metadataPage.getContent(); // Convert Page to List
        } catch (Exception e) {
            logger.error("Error retrieving metadata with pagination for page {} and size {}", page, size, e);
            throw new RuntimeException("Error retrieving metadata with pagination: " + e.getMessage());
        }
    }

    /**
     * Deletes metadata for a specific file by its path from both MySQL and Elasticsearch.
     *
     * @param filePath The file path for which metadata is to be deleted.
     */
    public void deleteMetadata(String filePath) {
        try {
            // Find metadata by filePath
            FileMetadata fileMetadata = fileMetadataRepository.findByPath(filePath);

            if (fileMetadata != null) {
                deleteMetadata(fileMetadata.getId());  // Pass the Long ID for deletion
                logger.info("Deleted metadata from MySQL for file: {}", filePath);
            } else {
                logger.warn("No MySQL metadata found for file: {}", filePath);
            }

            // Delete metadata from Elasticsearch
            elasticsearchService.deleteMetadataByFilePath(filePath);
            logger.info("Deleted metadata from Elasticsearch for file: {}", filePath);

        } catch (Exception e) {
            logger.error("Error deleting metadata for file: {}", filePath, e);
        }
    }

    /**
     * Deletes metadata for a specific file by its ID from both MySQL and Elasticsearch.
     *
     * @param id The ID of the file to delete metadata for.
     */
    public void deleteMetadata(Long id) {
        try {
            // Delete metadata from MySQL
            FileMetadata fileMetadata = fileMetadataRepository.findById(id).orElse(null);
            if (fileMetadata != null) {
                fileMetadataRepository.delete(fileMetadata);
                logger.info("Deleted metadata from MySQL for file with ID: {}", id);
            } else {
                logger.warn("No MySQL metadata found for file with ID: {}", id);
            }

            // Delete metadata from Elasticsearch
            elasticsearchService.deleteMetadataByFilePath(fileMetadata.getPath());
            logger.info("Deleted metadata from Elasticsearch for file with ID: {}", id);

        } catch (Exception e) {
            logger.error("Error deleting metadata for file with ID: {}", id, e);
        }
    }

    /**
     * Deletes metadata for all files in a specified folder.
     *
     * @param folderPath The folder path for which metadata is to be deleted.
     */
    public void deleteMetadataForFolder(String folderPath) {
        File folder = new File(folderPath);

        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        deleteMetadata(file.getAbsolutePath()); // Delete metadata for each file
                    } else if (file.isDirectory()) {
                        deleteMetadataForFolder(file.getAbsolutePath()); // Recursively delete metadata for subfolders
                    }
                }
            } else {
                logger.warn("The folder is empty or could not be accessed: {}", folderPath);
            }
        } else {
            logger.error("Invalid folder path: {}", folderPath);
        }
    }

    /**
     * Retrieves metadata for all files in the specified folder, filtering by path type (Local, NFS, SMB).
     *
     * @param folderPath Path to the folder whose metadata is to be retrieved.
     * @param pathType   The path type (Local, NFS, SMB) to filter by.
     * @return List of FileMetadata objects for the specified folder and path type.
     */
    public List<FileMetadata> getMetadataForFolder(String folderPath, String pathType) {
        try {
            File folder = new File(folderPath);

            if (folder.exists() && folder.isDirectory()) {
                PageRequest pageRequest = PageRequest.of(0, Integer.MAX_VALUE); // Adjust pagination as needed
                Page<FileMetadata> metadataPage;

                if ("NFS".equalsIgnoreCase(pathType)) {
                    metadataPage = fileMetadataRepository.findByTypeAndPathStartingWith("NFS", folderPath, pageRequest);
                } else if ("SMB".equalsIgnoreCase(pathType)) {
                    metadataPage = fileMetadataRepository.findByTypeAndPathStartingWith("SMB", folderPath, pageRequest);
                } else {
                    metadataPage = fileMetadataRepository.findByType(pathType, pageRequest);
                }

                return metadataPage.getContent(); // Convert Page to List
            } else {
                logger.error("Invalid folder path: {}", folderPath);
                return null;
            }
        } catch (Exception e) {
            logger.error("Error retrieving metadata for folder: {}", folderPath, e);
            return null;
        }
    }
}
