package com.fileinsights.repository;

import com.fileinsights.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    // Custom query method to find metadata by file paths
    List<FileMetadata> findByPathIn(List<String> paths);

    // Custom query method to find metadata by a single file path
    FileMetadata findByPath(String path);
}
