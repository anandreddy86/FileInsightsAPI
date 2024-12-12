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
        elasticsearchClient.index(request -> request
                .index(INDEX_NAME)
                .id(tikaMetadata.getFilePath()) // Use file path as unique identifier
                .document(tikaMetadata)
        );
    }
}
