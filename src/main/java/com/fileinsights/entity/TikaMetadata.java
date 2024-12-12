package com.fileinsights.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.util.HashMap;
import java.util.Map;

@Document(indexName = "tika_metadata")
public class TikaMetadata {

    @Id
    private String filePath;  // Unique identifier for the document, file path is assumed to be unique

    private String fileName;
    private String content;
    
    // A map to store any additional metadata fields
    private Map<String, String> metadataMap = new HashMap<>();

    // Getter and setter for filePath (or the unique identifier)
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    // Getter and setter for fileName
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    // Getter and setter for content
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    // Getter and setter for metadataMap
    public Map<String, String> getMetadataMap() {
        return metadataMap;
    }

    public void setMetadataMap(Map<String, String> metadataMap) {
        this.metadataMap = metadataMap;
    }

    // Optionally, you can add methods to add individual metadata entries if needed
    public void addMetadata(String key, String value) {
        this.metadataMap.put(key, value);
    }
}
