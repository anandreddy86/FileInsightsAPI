package com.fileinsights.service;

import com.fileinsights.entity.TikaMetadata;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ElasticsearchService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchService.class);
    private static final String INDEX_NAME = "tika_metadata";

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    /**
     * Save Tika metadata to Elasticsearch.
     */
    public void saveTikaMetadata(TikaMetadata tikaMetadata) throws Exception {
        if (tikaMetadata.getFilePath() == null || tikaMetadata.getFilePath().isEmpty()) {
            throw new IllegalArgumentException("File path must not be null or empty for Elasticsearch indexing.");
        }

        elasticsearchClient.index(i -> i
                .index(INDEX_NAME)
                .id(tikaMetadata.getFilePath())
                .document(tikaMetadata)
        );
        logger.info("Successfully saved metadata for file: {}", tikaMetadata.getFilePath());
    }

    /**
     * Retrieve metadata by folder path and path type.
     */
    public List<TikaMetadata> getMetadataByFolderPath(String folderPath, String pathType) throws IOException {
        // Modify query to include pathType (Local, NFS, SMB)
        var query = co.elastic.clients.elasticsearch._types.query_dsl.Query.of(q -> q
                .bool(b -> b
                    .filter(
                        f -> f.wildcard(w -> w
                            .field("filePath.keyword") // Use `.keyword` to perform exact matching
                            .value(folderPath + "*")
                        )
                    )
                    .filter(
                        f -> f.term(t -> t
                            .field("pathType.keyword") // Assuming pathType is indexed
                            .value(pathType) // Filtering by pathType
                        )
                    )
                )
        );

        var searchRequest = new SearchRequest.Builder()
                .index(INDEX_NAME)
                .query(query)
                .build();

        var response = elasticsearchClient.search(searchRequest, TikaMetadata.class);
        logger.info("Retrieved {} metadata records for folder path: {} and pathType: {}", response.hits().hits().size(), folderPath, pathType);

        return response.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());
    }

    /**
     * Delete metadata by file path.
     */
    public void deleteMetadataByFilePath(String filePath) throws IOException {
        var deleteRequest = new DeleteRequest.Builder()
                .index(INDEX_NAME)
                .id(filePath)
                .build();

        var deleteResponse = elasticsearchClient.delete(deleteRequest);

        if ("not_found".equalsIgnoreCase(deleteResponse.result().jsonValue())) {
            logger.warn("No document found for file path: {}", filePath);
        } else {
            logger.info("Metadata deleted successfully for file path: {}", filePath);
        }
    }

    /**
     * Bulk delete metadata by file paths.
     */
    public void deleteMetadataByFilePaths(List<String> filePaths) throws IOException {
        BulkRequest.Builder bulkRequest = new BulkRequest.Builder();

        for (String filePath : filePaths) {
            bulkRequest.operations(op -> op
                .delete(del -> del
                    .index(INDEX_NAME)
                    .id(filePath)
                )
            );
        }

        BulkResponse bulkResponse = elasticsearchClient.bulk(bulkRequest.build());

        if (bulkResponse.errors()) {
            logger.error("Errors occurred during bulk delete");
        } else {
            logger.info("Successfully deleted {} metadata records", bulkResponse.items().size());
        }
    }

    /**
     * Delete metadata by folder path.
     */
    public void deleteByPath(String folderPath) throws IOException {
        var query = co.elastic.clients.elasticsearch._types.query_dsl.Query.of(q -> q
                .wildcard(w -> w
                        .field("filePath.keyword")
                        .value(folderPath + "*")
                )
        );

        var deleteRequest = new DeleteByQueryRequest.Builder()
                .index(INDEX_NAME)
                .query(query)
                .conflicts(co.elastic.clients.elasticsearch._types.Conflicts.Proceed)  // Handling conflicts
                .build();

        DeleteByQueryResponse response = elasticsearchClient.deleteByQuery(deleteRequest);

        logger.info("Deleted {} documents for folder path: {}", response.deleted(), folderPath);

        if (response.versionConflicts() > 0) {
            logger.warn("Version conflicts encountered during deleteByQuery for folder: {}. Conflicts: {}",
                    folderPath, response.versionConflicts());
        }
    }

    /**
     * Count files by type using metadataMap.Content-Type.keyword field.
     */
    public Map<String, Long> countFilesByType() throws IOException {
        var request = new SearchRequest.Builder()
                .index(INDEX_NAME)
                .size(0) // Only aggregation, no hits
                .aggregations("fileTypes", agg -> agg
                        .terms(t -> t
                                .field("metadataMap.Content-Type.keyword")
                                .size(1000)
                        )
                )
                .build();

        var response = elasticsearchClient.search(request, Void.class);

        Map<String, Long> fileTypeCounts = new HashMap<>();

        StringTermsAggregate fileTypeAggregation = response.aggregations()
                .get("fileTypes")
                .sterms();

        if (fileTypeAggregation != null && fileTypeAggregation.buckets() != null) {
            for (StringTermsBucket bucket : fileTypeAggregation.buckets().array()) {
                fileTypeCounts.put(bucket.key().stringValue(), bucket.docCount());
            }
        }

        logger.info("File type aggregation result: {}", fileTypeCounts);
        return fileTypeCounts;
    }
}
