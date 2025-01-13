package com.fileinsights.service;

import com.fileinsights.repository.FileMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private ElasticsearchService elasticsearchService;

    /**
     * Retrieves file data split by age (atime).
     * @return A map of age categories to file counts.
     */
    public Map<String, Long> getFileDataByAge() {
        List<Object[]> results = fileMetadataRepository.countFilesByAge();
        Map<String, Long> data = new HashMap<>();
        for (Object[] result : results) {
            data.put((String) result[0], (Long) result[1]);
        }
        return data;
    }

    /**
     * Retrieves file data split by type from Elasticsearch.
     * @return A map of file types to file counts.
     */
    public Map<String, Long> getFileDataByType() {
        Map<String, Long> data = new HashMap<>();
        try {
            data = elasticsearchService.countFilesByType();
        } catch (Exception e) {
            e.printStackTrace();
            // Log and handle errors appropriately
        }
        return data;
    }
}
