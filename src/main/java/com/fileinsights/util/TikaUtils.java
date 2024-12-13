package com.fileinsights.util;

import com.fileinsights.entity.TikaMetadata;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

public class TikaUtils {

    /**
     * Extracts metadata from the given file using Apache Tika.
     * 
     * @param file The file from which to extract metadata.
     * @param originalFileName The original file name.
     * @return The extracted Tika metadata.
     * @throws Exception If there is an error parsing the file.
     */
    public static TikaMetadata extractTikaMetadata(File file, String originalFileName) throws Exception {
        Metadata metadata = new Metadata();
        BodyContentHandler handler = new BodyContentHandler(-1);
        AutoDetectParser parser = new AutoDetectParser();
        ParseContext context = new ParseContext();

        // Parse the file using Tika
        try (FileInputStream inputStream = new FileInputStream(file)) {
            parser.parse(inputStream, handler, metadata, context);
        }

        // Convert Tika metadata to a map for easier storage in Elasticsearch
        Map<String, String> metadataMap = new HashMap<>();
        for (String name : metadata.names()) {
            metadataMap.put(name, metadata.get(name));
        }

        // Create a TikaMetadata object and populate it
        TikaMetadata tikaMetadata = new TikaMetadata();
        tikaMetadata.setFilePath(file.getAbsolutePath()); // Set the full path of the file
        tikaMetadata.setFileName(originalFileName); // Use the original file name
        tikaMetadata.setMetadataMap(metadataMap); // Set all metadata extracted by Tika
        tikaMetadata.setContent(handler.toString()); // Set the file content extracted by Tika (if any)

        return tikaMetadata;
    }
}
