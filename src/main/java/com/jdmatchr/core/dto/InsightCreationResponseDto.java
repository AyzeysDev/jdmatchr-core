// src/main/java/com/jdmatchr/core/dto/InsightCreationResponseDto.java
package com.jdmatchr.core.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InsightCreationResponseDto(
        UUID insightId,
        String jobTitle,
        Integer matchScore, // Assuming this comes from the AI analysis
        Integer atsScore,   // Assuming this comes from the AI analysis
        OffsetDateTime createdAt
) {}