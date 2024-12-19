package com.fileinsights.service;

import com.fileinsights.entity.FileMetadata;
import com.fileinsights.repository.FileMetadataRepository;
import com.fileinsights.util.TikaUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FileMetadataService {

    private static final Logger logger = LoggerFactory.getLogger(FileMetadataService.class);

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private ElasticsearchService elasticsearchService;

    /**
     * Process the uploaded file, extract metadata, and save it to MySQL and Elasticsearch.
     *
     * @param file The uploaded file.
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
     * Processes and saves a file's metadata to both MySQL and Elasticsearch.
     *
     * @param file The file to process.
     * @param originalFileName The original file name.
     * @throws Exception If an error occurs.
     */
    private void processAndSaveFile(File file, String originalFileName) throws Exception {
        try {
            // Extract basic metadata
            FileMetadata fileMetadata = extractMetadata(file, originalFileName);
            fileMetadataRepository.save(fileMetadata); // Save to MySQL

            // Extract advanced metadata with Tika
            var tikaMetadata = TikaUtils.extractTikaMetadata(file, originalFileName);
            elasticsearchService.saveTikaMetadata(tikaMetadata); // Save to Elasticsearch
        } catch (Exception e) {
            logger.error("Error processing file: {}", file.getAbsolutePath(), e);
            throw e;
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
        Path tempFilePath = Files.createTempFile("upload-", file.getOriginalFilename());
        file.transferTo(tempFilePath.toFile());
        return tempFilePath;
    }

    /**
     * Extract metadata from a file.
     *
     * @param file The file to extract metadata from.
     * @param originalFileName The original file name.
     * @return Extracted file metadata.
     */
    public FileMetadata extractMetadata(File file, String originalFileName) {
        FileMetadata metadata = new FileMetadata();
        metadata.setName(originalFileName);
        metadata.setSize(file.length());
        metadata.setCtime(file.lastModified());
        metadata.setMtime(file.lastModified());
        metadata.setAtime(file.lastModified());
        
        // Set the path (you can adjust this to your needs)
        metadata.setPath(file.getAbsolutePath()); // Use the absolute path of the file
        
        return metadata;
    }

    /**
     * Save file metadata to the repository (MySQL).
     *
     * @param fileMetadata The file metadata to save.
     */
    public void saveFileMetadata(FileMetadata fileMetadata) {
        fileMetadataRepository.save(fileMetadata);
    }

    /**
     * Retrieve file metadata by its ID.
     *
     * @param id The ID of the metadata to retrieve.
     * @return The file metadata with the specified ID, or null if not found.
     */
    public FileMetadata getMetadata(Long id) {
        return fileMetadataRepository.findById(id).orElse(null);
    }

    /**
     * Retrieve all file metadata with pagination.
     *
     * @param page The page number (0-based index).
     * @param size The page size.
     * @return A list of file metadata.
     */
    public List<FileMetadata> getAllMetadata(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<FileMetadata> pageResult = fileMetadataRepository.findAll(pageRequest);
        return pageResult.getContent();
    }

    /**
     * Processes a folder and indexes metadata of all files within the folder and subfolders.
     *
     * @param folder The folder to process.
     * @throws IOException If an error occurs while reading the folder.
     */
    public void processFolder(File folder) throws IOException {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        try {
                            processAndSaveFile(file, file.getName());
                        } catch (Exception e) {
                            logger.error("Error processing file: {}", file.getAbsolutePath(), e);
                        }
                    } else if (file.isDirectory()) {
                        processFolder(file); // Recursively process subfolders
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Provided path is not a directory: " + folder.getAbsolutePath());
        }
    }

    /**
     * Get all metadata for files in a specific folder.
     *
     * @param folderPath The folder path to retrieve metadata from.
     * @return A list of FileMetadata for all files in the folder.
     */
    public List<FileMetadata> getMetadataForFolder(String folderPath) {
        List<FileMetadata> metadataList = new ArrayList<>();
        File folder = new File(folderPath);

        if (folder.exists() && folder.isDirectory()) {
            try {
                // Recursively process folder and subfolders
                File[] files = folder.listFiles();
                if (files != null) {
                    metadataList = getMetadataForFiles(files);
                }
            } catch (Exception e) {
                logger.error("Error retrieving metadata for folder: {}", folderPath, e);
            }
        } else {
            logger.warn("The provided folder path is invalid or not a directory: {}", folderPath);
        }

        return metadataList; // Return an empty list if no metadata is found
    }

    /**
     * Helper method to extract metadata for an array of files.
     *
     * @param files The array of File objects.
     * @return A list of FileMetadata objects.
     */
    private List<FileMetadata> getMetadataForFiles(File[] files) {
        List<FileMetadata> metadataList = new ArrayList<>();
        for (File file : files) {
            if (file.isFile()) {
                // Fetch metadata for each file based on its absolute path
                FileMetadata metadata = fileMetadataRepository.findByPath(file.getAbsolutePath());
                if (metadata != null) {
                    metadataList.add(metadata);
                }
            } else if (file.isDirectory()) {
                // Recursively fetch metadata from subfolders
                metadataList.addAll(getMetadataForFolder(file.getAbsolutePath()));
            }
        }
        return metadataList;
    }

    /**
     * Helper method to extract file paths from an array of File objects.
     *
     * @param files The array of File objects.
     * @return A list of file paths as strings.
     */
    private List<String> getFilePaths(File[] files) {
        List<String> paths = new ArrayList<>();
        for (File file : files) {
            if (file.isFile()) {
                paths.add(file.getAbsolutePath());
            }
        }
        return paths;
    }
}
