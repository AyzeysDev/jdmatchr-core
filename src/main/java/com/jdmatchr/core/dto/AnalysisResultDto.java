package com.jdmatchr.core.dto;

// import com.fasterxml.jackson.annotation.JsonInclude; // No longer needed for mockProcessingTimestamp
import java.util.List;

public record AnalysisResultDto(
        // @JsonInclude(JsonInclude.Include.NON_NULL) // Removed for mockProcessingTimestamp
        // String mockProcessingTimestamp, // REMOVED
        Integer matchScore, // KEPT - part of AI's core analysis output
        Integer atsScore,   // KEPT - part of AI's core analysis output
        FluffAnalysisDto fluffAnalysis,
        RoleFitAndAlignmentMetricsDto roleFitAndAlignmentMetrics,
        KeywordAnalysisDto keywordAnalysis,
        List<String> resumeSuggestions,
        List<String> interviewPreparationTopics
) {
}