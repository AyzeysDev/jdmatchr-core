// src/main/java/com/jdmatchr/core/service/PdfParserServiceImpl.java
package com.jdmatchr.core.service;

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

    @Override
    public String parsePdf(MultipartFile pdfFile) throws IOException {
        if (pdfFile == null || pdfFile.isEmpty()) {
            logger.warn("PDF file is null or empty. Cannot parse.");
            return ""; // Or throw IllegalArgumentException
        }

        logger.info("Starting PDF parsing for file: {}", pdfFile.getOriginalFilename());
        try (InputStream inputStream = pdfFile.getInputStream();
             PDDocument document = Loader.loadPDF(pdfFile.getBytes())) {

            if (!document.isEncrypted()) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                logger.info("Successfully parsed PDF file: {}. Text length: {}", pdfFile.getOriginalFilename(), text.length());
                // Basic cleaning: replace multiple spaces/newlines with a single one, trim.
                return text.replaceAll("\\s+", " ").trim();
            } else {
                logger.warn("PDF file {} is encrypted. Cannot extract text.", pdfFile.getOriginalFilename());
                throw new IOException("Cannot parse encrypted PDF: " + pdfFile.getOriginalFilename());
            }
        } catch (IOException e) {
            logger.error("IOException during PDF parsing for file {}: {}", pdfFile.getOriginalFilename(), e.getMessage());
            throw e; // Re-throw to be handled by the caller
        } catch (Exception e) {
            logger.error("Unexpected error during PDF parsing for file {}: {}", pdfFile.getOriginalFilename(), e.getMessage(), e);
            throw new IOException("Failed to parse PDF file: " + pdfFile.getOriginalFilename(), e);
        }
    }
}