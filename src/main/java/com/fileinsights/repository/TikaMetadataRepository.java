package com.fileinsights.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.fileinsights.entity.TikaMetadata;

@Repository
public interface TikaMetadataRepository extends ElasticsearchRepository<TikaMetadata, String> {
    // Custom query methods can be added here if needed
}
