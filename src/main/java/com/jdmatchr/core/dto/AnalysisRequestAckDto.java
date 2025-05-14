package com.jdmatchr.core.dto;

// Using a record for a concise DTO
public record AnalysisRequestAckDto(
        String message,
        String receivedJobTitle,
        String receivedResumeFilename,
        long receivedResumeSize, // size in bytes
        String jobDescriptionSnippet,
        String processedByUserId // Could be email or UUID string
) {}