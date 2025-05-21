package com.jdmatchr.core.dto;

import java.time.OffsetDateTime; // Re-added if it was removed, or ensure it's present
import java.util.UUID;
// Map import might not be needed directly here if AnalysisResultDto handles its own complexities
// import java.util.Map;


public record InsightDetailDto(
        UUID id,
        String jobTitle,
        String resumeFilename,
        OffsetDateTime createdAt, // RE-ADDED
        AnalysisResultDto analysisResult
) {
}