// src/main/java/com/jdmatchr/core/service/PdfParserService.java
package com.jdmatchr.core.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface PdfParserService {
    /**
     * Extracts text content from an uploaded PDF file.
     * @param pdfFile The PDF file uploaded by the user.
     * @return The extracted text content as a String.
     * @throws IOException If an error occurs during file reading or PDF parsing.
     */
    String parsePdf(MultipartFile pdfFile) throws IOException;
}