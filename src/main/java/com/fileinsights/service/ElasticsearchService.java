package com.fileinsights.service;

import com.fileinsights.entity.TikaMetadata;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Conflicts;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ElasticsearchService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchService.class);
    private static final String INDEX_NAME = "tika_metadata";

    @Autowired
    private ElasticsearchClient elasticsearchClient;

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

        elasticsearchClient.index(request -> request
                .index(INDEX_NAME)
                .id(tikaMetadata.getFilePath())
                .document(tikaMetadata)
        );
        logger.info("Successfully saved metadata for file: {}", tikaMetadata.getFilePath());
    }

    /**
     * Retrieves Tika metadata for a given folder path from Elasticsearch.
     *
     * @param folderPath The folder path to query for metadata.
     * @return List of TikaMetadata objects.
     * @throws IOException If there is an error querying Elasticsearch.
     */
    public List<TikaMetadata> getMetadataByFolderPath(String folderPath) throws IOException {
        Query query = Query.of(q -> q
                .wildcard(w -> w
                        .field("filePath.keyword") // Use keyword field for exact matching
                        .value(folderPath + "*")  // Match folderPath and its subpaths
                )
        );

        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(INDEX_NAME)
                .query(query)
                .build();

        logger.info("Executing Elasticsearch query for folder path: {}", folderPath);

        SearchResponse<TikaMetadata> response = elasticsearchClient.search(searchRequest, TikaMetadata.class);

        logger.info("Retrieved {} metadata records for folder path: {}", response.hits().hits().size(), folderPath);

        return response.hits().hits().stream()
                .map(hit -> hit.source())
                .collect(Collectors.toList());
    }

    /**
     * Deletes Tika metadata by file path from Elasticsearch.
     *
     * @param filePath The file path whose metadata is to be deleted.
     * @throws IOException If there is an error deleting metadata from Elasticsearch.
     */
    public void deleteMetadataByFilePath(String filePath) throws IOException {
        DeleteRequest deleteRequest = new DeleteRequest.Builder()
                .index(INDEX_NAME)
                .id(filePath)
                .build();

        DeleteResponse deleteResponse = elasticsearchClient.delete(deleteRequest);

        if ("not_found".equalsIgnoreCase(deleteResponse.result().name())) {
            logger.warn("No document found for file path: {}", filePath);
        } else {
            logger.info("Metadata deleted successfully for file path: {}", filePath);
        }
    }

    /**
     * Deletes Tika metadata for multiple file paths from Elasticsearch.
     *
     * @param filePaths The list of file paths whose metadata is to be deleted.
     */
    public void deleteMetadataByFilePaths(List<String> filePaths) {
        for (String filePath : filePaths) {
            try {
                deleteMetadataByFilePath(filePath);
            } catch (IOException e) {
                logger.error("Error deleting metadata for file path: {}", filePath, e);
            }
        }
    }

    /**
     * Deletes all Tika metadata associated with a folder path from Elasticsearch.
     *
     * @param folderPath The folder path to delete metadata from.
     * @throws IOException If there is an error deleting metadata from Elasticsearch.
     */
    public void deleteByPath(String folderPath) throws IOException {
        Query query = Query.of(q -> q
                .wildcard(w -> w
                        .field("filePath.keyword")  // Use keyword field for exact matching
                        .value(folderPath + "*")   // Match all paths under the folder
                )
        );

        DeleteByQueryRequest deleteRequest = new DeleteByQueryRequest.Builder()
                .index(INDEX_NAME)
                .query(query)
                .conflicts(Conflicts.Proceed) // Updated to use the enum correctly
                .build();

        try {
            DeleteByQueryResponse response = elasticsearchClient.deleteByQuery(deleteRequest);

            logger.info("Deleted {} documents for folder path: {}", response.deleted(), folderPath);
            if (response.versionConflicts() > 0) {
                logger.warn("Version conflicts encountered during deleteByQuery for folder: {}. Conflicts: {}",
                        folderPath, response.versionConflicts());
            }

        } catch (IOException e) {
            logger.error("Error occurred while deleting metadata for folder path: {}", folderPath, e);
            throw e;
        }
    }
}
