package com.jdmatchr.core.dto;
import java.util.List;

// Corresponds to "fluffAnalysis"
record FluffAnalysisDto(
        List<FluffDetailDto> detected,
        String summary
) {
}