//// src/main/java/com/jdmatchr/core/service/PdfParserServiceImpl.java
//package com.jdmatchr.core.service;
//
//import org.apache.pdfbox.Loader; // Import the new Loader class
//import org.apache.pdfbox.pdmodel.PDDocument;
//import org.apache.pdfbox.text.PDFTextStripper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.io.InputStream;
//
//@Service
//public class PdfParserServiceImpl implements PdfParserService {
//
//    private static final Logger logger = LoggerFactory.getLogger(PdfParserServiceImpl.class);
//
//    @Override
//    public String parsePdfToText(MultipartFile pdfFile) throws IOException {
//        if (pdfFile == null || pdfFile.isEmpty()) {
//            logger.warn("PDF file is null or empty.");
//            throw new IllegalArgumentException("PDF file cannot be null or empty.");
//        }
//
//        String originalFilename = pdfFile.getOriginalFilename();
//        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
//            logger.warn("Invalid file type provided: {}. Expected a PDF.", originalFilename);
//            throw new IllegalArgumentException("Invalid file type. Please upload a PDF document.");
//        }
//
//        try (InputStream inputStream = pdfFile.getInputStream();
//             // Use Loader.loadPDF() instead of PDDocument.load()
//             PDDocument document = Loader.loadPDF(pdfFile.getBytes())) { // Loading from byte array is also common
//
//            // Decryption attempt (if necessary) - remains the same
//            if (document.isEncrypted()) {
//                try {
//                    document.setAllSecurityToBeRemoved(true);
//                } catch (Exception e) { // Catch a more general exception for decryption failure
//                    logger.warn("Could not decrypt PDF: {}", originalFilename, e);
//                    // Note: setAllSecurityToBeRemoved might throw an IOException if it fails.
//                    // Depending on the PDFBox version and specific encryption, this might need more robust handling.
//                    // For simple cases, this might suffice. If it fails, the getText below might also fail or return garbage.
//                }
//            }
//
//            PDFTextStripper pdfStripper = new PDFTextStripper();
//            String text = pdfStripper.getText(document);
//
//            String cleanedText = text.replaceAll("\\s+", " ").trim();
//            logger.info("Successfully extracted and cleaned text from PDF: {}", originalFilename);
//            return cleanedText;
//
//        } catch (IOException e) {
//            logger.error("Error parsing PDF file {}: {}", originalFilename, e.getMessage(), e);
//            throw new IOException("Failed to parse PDF file: " + originalFilename, e);
//        }
//        // Catching Exception for any other PDFBox related issues during loading or processing
//        catch (Exception e) {
//            logger.error("Unexpected error processing PDF file {}: {}", originalFilename, e.getMessage(), e);
//            throw new IOException("Unexpected error processing PDF file: " + originalFilename, e);
//        }
//    }
//}
