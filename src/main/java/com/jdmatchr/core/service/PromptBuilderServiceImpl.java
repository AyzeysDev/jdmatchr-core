// src/main/java/com/jdmatchr/core/service/PromptBuilderServiceImpl.java
package com.jdmatchr.core.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PromptBuilderServiceImpl implements PromptBuilderService {

    private static final Logger logger = LoggerFactory.getLogger(PromptBuilderServiceImpl.class);
    private static final int MAX_JD_LENGTH = 3000;

    @Override
    public String buildPrompt(String jobTitle, String jobDescription, String resumeText) {

        logger.info("Original JD length: {}", jobDescription.length());
        String cleanedJd = jobDescription.replaceAll("[^\\x00-\\x7F]", "")
                .replaceAll("🧠|🚀|🎯|✅|📌|📍|✨", "")
                .replaceAll("\\s{2,}", " ")
                .trim();
        logger.info("Cleaned JD length: {}", cleanedJd.length());
        String trimmedJd = StringUtils.abbreviate(cleanedJd, MAX_JD_LENGTH);
        logger.info("Truncated JD length for prompt: {}", trimmedJd.length());
        logger.info("Resume text length for prompt: {}", resumeText.length());

        return """
        You are a career analysis assistant.

        Return ONLY a valid JSON object with the exact format below.
        For all arrays, select only the most critical and impactful items, adhering to the maximum number of items specified for each field.

        {
          "matchScore": number,
          "atsScore": number,
          "fluffAnalysis": {
            "summary": "string (1-2 brief sentences)", // Example of specific brevity instruction
            "detected": [{ "original": string, "suggestion": string }] // MAX 5 items - most impactful
          },
          "roleFitAndAlignmentMetrics": {
            "prediction": {
              "verdict": "string (must be one of: 'Misfit', 'Developing', 'Strong', 'Optimal')",
              "reason": "string (concise reason for the verdict, 2-3 sentences)" // Example of specific brevity
            },
            "radarData": { "technicalSkills": number, "softSkills": number, "experienceLevel": number, "cultureFit": number },
            "alignmentBreakdown": { "skills": number, "experience": number, "education": number, "keywords": number }
          },
          "keywordAnalysis": {
            "matchedKeywords": string[], // MAX 10 items - most relevant matched keywords
            "missingKeywords": string[], // MAX 10 items - most critical missing keywords
            "keywordDensityScore": number
          },
          "resumeSuggestions": string[], // MAX 4 concise suggestions - most impactful
          "interviewPreparationTopics": string[] // MAX 4 key topics - most impactful
        }

        Only respond with the JSON — no other commentary.

        Job Title: %s

        Job Description:
        %s

        Resume Text:
        %s
        """.formatted(jobTitle, trimmedJd, resumeText);
    }
}