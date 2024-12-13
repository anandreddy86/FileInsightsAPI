package com.fileinsights.service;

import com.fileinsights.entity.TikaMetadata;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ElasticsearchService {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    private static final String INDEX_NAME = "tika_metadata";

    /**
     * Saves Tika metadata to Elasticsearch.
     *
     * @param tikaMetadata The metadata to index.
     * @throws Exception If there is an error saving metadata to Elasticsearch.
     */
    public void saveTikaMetadata(TikaMetadata tikaMetadata) throws Exception {
        if (tikaMetadata.getFilePath() == null || tikaMetadata.getFilePath().isEmpty()) {
            throw new IllegalArgumentException("File path must not be null or empty for Elasticsearch indexing.");
        }

        // Index metadata with file path as the unique identifier
        elasticsearchClient.index(request -> request
                .index(INDEX_NAME)
                .id(tikaMetadata.getFilePath()) // Use file path as unique identifier
                .document(tikaMetadata)
        );
    }
}
