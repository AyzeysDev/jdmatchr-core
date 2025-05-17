package com.jdmatchr.core.dto;
import java.util.List;

// Corresponds to the "analysisResult" object
public record AnalysisResultDto(
        String mockProcessingTimestamp,
        JdSummaryDto jdSummary,
        FluffAnalysisDto fluffAnalysis,
        RoleFitAndAlignmentMetricsDto roleFitAndAlignmentMetrics,
        KeywordAnalysisDto keywordAnalysis,
        List<String> resumeSuggestions,
        List<String> interviewPreparationTopics
) {
}
