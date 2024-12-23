package com.fileinsights.service;

import com.fileinsights.entity.FileMetadata;
import com.fileinsights.entity.TikaMetadata;
import com.fileinsights.util.TikaUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;

@Service
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    @Autowired
    private FileMetadataService fileMetadataService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    /**
     * Processes the given folder and extracts metadata for each file.
     *
     * @param folder The folder to process.
     */
    public void processFolder(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        processFile(file); // Process individual files
                    } else if (file.isDirectory()) {
                        processFolder(file); // Recursively process subfolders
                    }
                }
            } else {
                logger.warn("The folder is empty or could not be accessed: {}", folder.getAbsolutePath());
            }
        } else {
            logger.error("Provided path is not a valid directory: {}", folder.getAbsolutePath());
        }
    }

    /**
     * Processes a single file by extracting and saving its metadata.
     *
     * @param file The file to process.
     */
    private void processFile(File file) {
        try {
            // Step 1: Extract basic file metadata and save to MySQL
            FileMetadata fileMetadata = fileMetadataService.extractMetadata(file, file.getName());
            fileMetadataService.saveFileMetadata(fileMetadata);

            // Step 2: Extract advanced metadata using Tika and save to Elasticsearch
            TikaMetadata tikaMetadata = TikaUtils.extractTikaMetadata(file, file.getName());
            elasticsearchService.saveTikaMetadata(tikaMetadata);

            logger.info("Successfully processed and stored metadata for file: {}", file.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Error processing file: {}", file.getAbsolutePath(), e);
        }
    }

    /**
     * Deletes metadata for a specific file by its path.
     *
     * @param filePath The file path for which metadata is to be deleted.
     */
    public void deleteMetadata(String filePath) {
        try {
            // Delete metadata from MySQL
            FileMetadata fileMetadata = fileMetadataService.getMetadataByPath(filePath);
            if (fileMetadata != null) {
                fileMetadataService.deleteMetadata(fileMetadata.getId());
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
                        deleteMetadata(file.getAbsolutePath());
                    } else if (file.isDirectory()) {
                        deleteMetadataForFolder(file.getAbsolutePath()); // Recursively delete subfolders
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
     * Fetches advanced metadata for a given folder from Elasticsearch.
     *
     * @param folderPath The folder path to query for advanced metadata.
     * @return List of TikaMetadata objects.
     */
    public List<TikaMetadata> getAdvancedMetadata(String folderPath) {
        try {
            logger.info("Fetching advanced metadata for folder: {}", folderPath);
            List<TikaMetadata> metadataList = elasticsearchService.getMetadataByFolderPath(folderPath);
            if (metadataList != null && !metadataList.isEmpty()) {
                logger.info("Fetched {} records from Elasticsearch.", metadataList.size());
            } else {
                logger.warn("No advanced metadata found for folder: {}", folderPath);
            }
            return metadataList != null ? metadataList : Collections.emptyList();
        } catch (Exception e) {
            logger.error("Error fetching advanced metadata for folder: {}", folderPath, e);
            return Collections.emptyList();
        }
    }
}
