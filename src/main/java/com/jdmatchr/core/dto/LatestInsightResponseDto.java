package com.jdmatchr.core.dto;
import java.util.Map;
import java.util.UUID;

// For the /latest endpoint response
public record LatestInsightResponseDto(
        UUID latestInsightId // Can be null if no insights exist
) {}
