package com.jdmatchr.core.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

// For listing in history - less detail
// Records are a concise way to create DTOs in modern Java
// For showing detailed insight - more detail
public record InsightDetailDto(
        UUID id,
        String jobTitle,
        String jobDescriptionSummary, // From Insights entity
        String resumeFilename,        // From Insights entity
        Double matchScore,            // From Insights entity
        Map<String, Object> analysisResult, // The JSONB field from Insights entity
        OffsetDateTime analysisDate   // same as createdAt in entity
        // Add any other fields from the Insights entity you want to display
) {}