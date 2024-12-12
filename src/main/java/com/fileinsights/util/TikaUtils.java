package com.fileinsights.util;

import com.fileinsights.entity.TikaMetadata;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;

import java.io.File;
import java.io.FileInputStream;

public class TikaUtils {

    /**
     * Extracts advanced metadata using Apache Tika.
     *
     * @param file The file to process.
     * @return TikaMetadata object containing extracted metadata.
     */
    public static TikaMetadata extractTikaMetadata(File file) throws Exception {
        TikaMetadata tikaMetadata = new TikaMetadata();
        try (FileInputStream inputStream = new FileInputStream(file)) {
            BodyContentHandler handler = new BodyContentHandler();
            Metadata metadata = new Metadata();
            AutoDetectParser parser = new AutoDetectParser();
            ParseContext context = new ParseContext();

            parser.parse(inputStream, handler, metadata, context);

            // Set file name and path
            tikaMetadata.setFileName(file.getName());
            tikaMetadata.setFilePath(file.getAbsolutePath());
            
            // Set the content of the file
            tikaMetadata.setContent(handler.toString());

            // Convert metadata fields to a map
            for (String name : metadata.names()) {
                tikaMetadata.addMetadata(name, metadata.get(name));
            }
        }
        return tikaMetadata;
    }
}
