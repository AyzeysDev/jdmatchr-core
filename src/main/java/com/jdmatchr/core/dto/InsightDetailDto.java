package com.jdmatchr.core.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

// For listing in history - less detail
// Records are a concise way to create DTOs in modern Java
// For showing detailed insight - more detail
// Main DTO for the insight detail response
public record InsightDetailDto(
        UUID id,
        String jobTitle,
        String resumeFilename,
        OffsetDateTime createdAt, // Corresponds to "createdAt" in the example JSON
        Integer matchScore,
        Integer atsScore,
        AnalysisResultDto analysisResult
) {
}