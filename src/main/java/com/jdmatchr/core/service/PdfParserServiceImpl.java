// src/main/java/com/jdmatchr/core/service/PdfParserServiceImpl.java
package com.jdmatchr.core.service;

import org.apache.commons.lang3.StringUtils; // Added
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
public class PdfParserServiceImpl implements PdfParserService {

    private static final Logger logger = LoggerFactory.getLogger(PdfParserServiceImpl.class);
    private static final int MAX_RESUME_LENGTH = 3500;

    @Override
    public String parsePdf(MultipartFile pdfFile) throws IOException {
        if (pdfFile == null || pdfFile.isEmpty()) {
            logger.warn("PDF file is null or empty. Cannot parse.");
            return "";
        }

        logger.info("Starting PDF parsing for file: {}", pdfFile.getOriginalFilename());
        try (InputStream inputStream = pdfFile.getInputStream();
             PDDocument document = Loader.loadPDF(pdfFile.getBytes())) { // Using getBytes() as in original

            if (!document.isEncrypted()) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                logger.info("Successfully parsed PDF file: {}. Original text length: {}", pdfFile.getOriginalFilename(), text.length());

                // Apply cleaning rules
                String cleanedText = text.replaceAll("[^\\x00-\\x7F]", "") // Remove non-ASCII
                        .replaceAll("\\s{2,}", " ") // Replace multiple spaces with one
                        .replaceAll("(?i)(references|declaration).*", "") // Remove references/declaration sections
                        .trim(); // Trim leading/trailing whitespace

                logger.info("Cleaned resume text length: {}", cleanedText.length());

                // Truncate
                String truncatedText = StringUtils.abbreviate(cleanedText, MAX_RESUME_LENGTH);
                logger.info("Truncated resume text length: {}", truncatedText.length());

                return truncatedText;
            } else {
                logger.warn("PDF file {} is encrypted. Cannot extract text.", pdfFile.getOriginalFilename());
                throw new IOException("Cannot parse encrypted PDF: " + pdfFile.getOriginalFilename());
            }
        } catch (IOException e) {
            logger.error("IOException during PDF parsing for file {}: {}", pdfFile.getOriginalFilename(), e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during PDF parsing for file {}: {}", pdfFile.getOriginalFilename(), e.getMessage(), e);
            throw new IOException("Failed to parse PDF file: " + pdfFile.getOriginalFilename(), e);
        }
    }
}