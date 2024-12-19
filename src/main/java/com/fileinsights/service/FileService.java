package com.fileinsights.service;

import com.fileinsights.entity.FileMetadata;
import com.fileinsights.entity.TikaMetadata;
import com.fileinsights.util.TikaUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public class FileService {

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
            }
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

        } catch (Exception e) {
            // Log the error and proceed with the next file
            System.err.println("Error processing file: " + file.getAbsolutePath());
            e.printStackTrace();
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
            return elasticsearchService.getMetadataByFolderPath(folderPath);
        } catch (Exception e) {
            System.err.println("Error fetching advanced metadata: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
