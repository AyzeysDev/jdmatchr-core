package com.jdmatchr.core.dto;
import java.util.List;

record KeywordAnalysisDto(
        List<String> missingKeywords,
        Integer keywordDensityScore,
        List<String> matchedKeywords
) {
}
