package com.jdmatchr.core.dto;
import java.util.List;

// Corresponds to "jdSummary"
record JdSummaryDto(
        String summary,
        List<String> responsibilities,
        List<String> requiredQualifications,
        String tone
) {}
