// src/main/java/com/jdmatchr/core/dto/InsightResponseDto.java
package com.jdmatchr.core.dto;

import java.time.OffsetDateTime; // Or LocalDateTime if you prefer
import java.util.UUID;

public record InsightResponseDto(
        UUID insightId,
        String jobTitle,
        Integer matchScore, // From AnalysisResultDto
        Integer atsScore,   // From AnalysisResultDto
        OffsetDateTime createdAt
) {}