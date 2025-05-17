// src/main/java/com/jdmatchr/core/dto/InsightSummaryDto.java
package com.jdmatchr.core.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

// For listing in history - less detail
public record InsightSummaryDto(
        UUID id,
        String jobTitle,
        OffsetDateTime analysisDate, // same as createdAt in entity
        Integer matchScore, // Changed to Integer to be consistent with InsightDetailDto
        Integer atsScore, // New field
        String resumeFilename
) {}