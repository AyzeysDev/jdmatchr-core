package com.jdmatchr.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

public record AnalysisResultDto(
        @JsonInclude(JsonInclude.Include.NON_NULL) // If backend sets this, not AI
        String mockProcessingTimestamp, // As per discussion, AI won't send this.
        // Backend can set this after receiving AI response.
        Integer matchScore, // Added from prompt, was previously in InsightDetailDto directly
        Integer atsScore,   // Added from prompt, was previously in InsightDetailDto directly
        JdSummaryDto jdSummary,
        FluffAnalysisDto fluffAnalysis,
        RoleFitAndAlignmentMetricsDto roleFitAndAlignmentMetrics,
        KeywordAnalysisDto keywordAnalysis,
        List<String> resumeSuggestions,
        List<String> interviewPreparationTopics
) {
}