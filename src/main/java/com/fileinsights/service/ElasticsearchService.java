package com.fileinsights.service;

import com.fileinsights.entity.TikaMetadata;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * Retrieves Tika metadata for a given folder path from Elasticsearch.
     *
     * @param folderPath The folder path to query for metadata.
     * @return List of TikaMetadata objects.
     * @throws IOException If there is an error querying Elasticsearch.
     */
    public List<TikaMetadata> getMetadataByFolderPath(String folderPath) throws IOException {
        // Build a match query using the folder path
        Query query = Query.of(q -> q
                .match(m -> m
                        .field("filePath")
                        .query(folderPath)  // The value to search for in the "filePath" field
                )
        );

        // Create the search request
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(INDEX_NAME)
                .query(query)  // Apply the query
                .build();

        // Execute the search
        SearchResponse<TikaMetadata> response = elasticsearchClient.search(searchRequest, TikaMetadata.class);

        // Extract and return the list of TikaMetadata objects from the response
        return response.hits().hits().stream()
                .map(hit -> hit.source())
                .collect(Collectors.toList());
    }
}
