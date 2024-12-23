package com.fileinsights.repository;

import com.fileinsights.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    /**
     * Find metadata for multiple file paths.
     *
     * @param paths List of file paths to query.
     * @return List of FileMetadata for the specified file paths.
     */
    List<FileMetadata> findByPathIn(List<String> paths);

    /**
     * Find metadata by a single file path.
     *
     * @param path The file path to query.
     * @return FileMetadata for the specified path, or null if not found.
     */
    FileMetadata findByPath(String path);

    /**
     * Delete metadata by file path.
     *
     * @param path The file path for which metadata is to be deleted.
     */
    void deleteByPath(String path);

    /**
     * Delete metadata for multiple file paths.
     *
     * @param paths List of file paths to delete metadata for.
     */
    void deleteByPathIn(List<String> paths);

    /**
     * Find metadata by file path that starts with a specific string.
     *
     * @param pathPrefix The prefix of the file path.
     * @return List of FileMetadata for the paths that start with the specified prefix.
     */
    List<FileMetadata> findByPathStartingWith(String pathPrefix);
}
