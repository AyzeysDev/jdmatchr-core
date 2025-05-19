//// src/main/java/com/jdmatchr/core/service/PdfParserService.java
//package com.jdmatchr.core.service;
//
//import org.springframework.web.multipart.MultipartFile;
//import java.io.IOException;
//
//public interface PdfParserService {
//    /**
//     * Extracts plain text from an uploaded PDF file.
//     *
//     * @param pdfFile The PDF file uploaded by the user.
//     * @return The extracted plain text, cleaned of excessive whitespace.
//     * @throws IOException If an error occurs during file reading or PDF parsing.
//     * @throws IllegalArgumentException If the file is not a PDF or is empty.
//     */
//    String parsePdfToText(MultipartFile pdfFile) throws IOException;
//}