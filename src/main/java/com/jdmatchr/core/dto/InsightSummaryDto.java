package com.jdmatchr.core.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

// For listing in history - less detail
// Records are a concise way to create DTOs in modern Java
public record InsightSummaryDto(
        UUID id,
        String jobTitle,
        OffsetDateTime analysisDate, // same as createdAt in entity
        Double matchScore, // Assuming you have this field in Insights entity
        String resumeFilename // Optional, if you want to show it in summary
) {}