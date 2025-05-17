// src/main/java/com/jdmatchr/core/service/AnalyzerServiceImpl.java
package com.jdmatchr.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jdmatchr.core.dto.*; // Imports all DTOs from the package
import com.jdmatchr.core.entity.Insights;
import com.jdmatchr.core.entity.User;
import com.jdmatchr.core.repository.InsightsRepository;
import com.jdmatchr.core.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.LinkedHashMap; // To preserve order in maps for JSON

@Service
public class AnalyzerServiceImpl implements AnalyzerService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzerServiceImpl.class);

    private final UserRepository userRepository; // Kept for processAnalysisRequestMock, can be removed if that method is removed
    private final InsightsRepository insightsRepository;
    private final ObjectMapper objectMapper; // For converting Map to DTO

    @Autowired
    public AnalyzerServiceImpl(UserRepository userRepository, InsightsRepository insightsRepository) {
        this.userRepository = userRepository;
        this.insightsRepository = insightsRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule()); // Important for OffsetDateTime
    }

    @Override
    @Transactional
    public InsightDetailDto analyzeDocuments( // Return type changed
                                              MultipartFile resumeFile,
                                              String jobTitle,
                                              String jobDescription,
                                              User authenticatedUser
    ) {
        logger.info("analyzeDocuments service called for user ID: {}, Job Title: {}", authenticatedUser.getId(), jobTitle);

        String originalResumeFilename = "Updated_Resume_FE.pdf"; // Mocked as per example
        long resumeFileSize = 0;

        if (resumeFile != null && !resumeFile.isEmpty()) {
            originalResumeFilename = resumeFile.getOriginalFilename();
            resumeFileSize = resumeFile.getSize();
            logger.info("Resume received: {}, Size: {} bytes", originalResumeFilename, resumeFileSize);
        } else {
            logger.warn("No resume file provided or file is empty for job title: {}. Using mock filename.", jobTitle);
        }

        // 2. Mock AI Model Call & Result Generation (New Structure)
        Random random = new Random();
        int matchScoreInt = 84; // From example
        int atsScoreInt = 76;   // From example

        // Using LinkedHashMap to preserve insertion order for JSON output if it matters
        Map<String, Object> analysisResultData = new LinkedHashMap<>();
        analysisResultData.put("mockProcessingTimestamp", OffsetDateTime.now(ZoneOffset.UTC).toString());

        Map<String, Object> jdSummaryMap = new LinkedHashMap<>();
        jdSummaryMap.put("summary", "Seeking a frontend engineer with 3+ years of experience building scalable web interfaces. Strong emphasis on React, component libraries, and unit testing. Team-oriented, remote-first culture.");
        jdSummaryMap.put("responsibilities", List.of(
                "Develop and maintain component libraries using React",
                "Collaborate with design teams to implement UI/UX features",
                "Write unit and integration tests"
        ));
        jdSummaryMap.put("requiredQualifications", List.of(
                "3+ years experience with JavaScript/TypeScript",
                "Deep knowledge of React and modern frontend tooling",
                "Familiarity with CI/CD and testing frameworks"
        ));
        jdSummaryMap.put("tone", "Technical, collaborative, modern");
        analysisResultData.put("jdSummary", jdSummaryMap);

        Map<String, Object> fluffAnalysisMap = new LinkedHashMap<>();
        fluffAnalysisMap.put("detected", List.of(
                Map.of("original", "Helped with building internal dashboards.", "suggestion", "Designed and developed internal dashboards using React and D3.js."),
                Map.of("original", "Worked on performance improvements.", "suggestion", "Optimized React components, reducing page load times by 40%.")
        ));
        fluffAnalysisMap.put("summary", "Your resume contains 2 areas where stronger, action-based phrasing can improve clarity and impact.");
        analysisResultData.put("fluffAnalysis", fluffAnalysisMap);

        Map<String, Object> roleFitMap = new LinkedHashMap<>();
        roleFitMap.put("prediction", Map.of("verdict", "Strong Match", "reason", "Your resume demonstrates clear alignment with the job’s core responsibilities, particularly in React, TypeScript, and UI design patterns."));
        Map<String, Integer> radarDataMap = new LinkedHashMap<>();
        radarDataMap.put("technicalSkills", 90);
        radarDataMap.put("softSkills", 70);
        radarDataMap.put("experienceLevel", 85);
        radarDataMap.put("cultureFit", 60);
        roleFitMap.put("radarData", radarDataMap);
        Map<String, Integer> alignmentBreakdownMap = new LinkedHashMap<>();
        alignmentBreakdownMap.put("skills", 90);
        alignmentBreakdownMap.put("experience", 85);
        alignmentBreakdownMap.put("education", 75);
        alignmentBreakdownMap.put("keywords", 72);
        roleFitMap.put("alignmentBreakdown", alignmentBreakdownMap);
        analysisResultData.put("roleFitAndAlignmentMetrics", roleFitMap);

        Map<String, Object> keywordAnalysisMap = new LinkedHashMap<>();
        keywordAnalysisMap.put("missingKeywords", List.of("GraphQL", "Next.js"));
        keywordAnalysisMap.put("keywordDensityScore", 72);
        keywordAnalysisMap.put("matchedKeywords", List.of("React", "TypeScript", "JavaScript", "UI Design"));
        analysisResultData.put("keywordAnalysis", keywordAnalysisMap);

        analysisResultData.put("resumeSuggestions", List.of(
                "Consider adding a section for personal projects if relevant.",
                "Quantify achievements in your 'Frontend Developer at XYZ' role with specific metrics (e.g., 'Improved performance by X%').",
                "Tailor the summary section to better reflect the job requirements for 'Frontend Engineer'."
        ));
        analysisResultData.put("interviewPreparationTopics", List.of(
                "Be prepared to discuss your experience with state management in React (e.g., Redux, Zustand, Context API).",
                "Review common JavaScript data structures and algorithms.",
                "Formulate questions to ask the interviewer about their development workflow and team collaboration tools."
        ));
        logger.info("Mock AI analysis complete for job title: '{}'. Match Score: {}%, ATS Score: {}%", jobTitle, matchScoreInt, atsScoreInt);

        // 3. Save Insights to Database
        Insights newInsight = new Insights();
        newInsight.setUser(authenticatedUser);
        newInsight.setJobTitle(jobTitle); // Using the actual job title from request
        // Set the brief summary for the entity field, detailed one is in analysisResult
        newInsight.setJobDescriptionSummary(jobDescription.substring(0, Math.min(jobDescription.length(), 250)) + (jobDescription.length() > 250 ? "..." : ""));
        newInsight.setResumeFilename(originalResumeFilename);
        newInsight.setMatchScore((double) matchScoreInt); // Store as double
        newInsight.setAtsScore(atsScoreInt); // Store new ATS score
        newInsight.setAnalysisResult(analysisResultData);
        // newInsight.setCreatedAt() is handled by @PrePersist

        Insights savedInsight = insightsRepository.save(newInsight);
        logger.info("Saved new insight with ID: {} for user: {}", savedInsight.getId(), authenticatedUser.getEmail());

        // 4. Construct and Return DTO
        return convertToDetailDto(savedInsight);
    }

    @Override
    @Transactional(readOnly = true)
    public AnalysisRequestAckDto processAnalysisRequestMock(
            MultipartFile resumeFile,
            String jobTitle,
            String jobDescription,
            User authenticatedUser
    ) {
        logger.info("--- MOCK ANALYSIS REQUEST RECEIVED (NO DB SAVE) ---");
        logger.info("User: {}", authenticatedUser.getEmail());
        logger.info("User ID: {}", authenticatedUser.getId());
        logger.info("Job Title: {}", jobTitle);
        String resumeFilename = "N/A";
        long resumeSize = 0;
        if (resumeFile != null && !resumeFile.isEmpty()) {
            resumeFilename = resumeFile.getOriginalFilename();
            resumeSize = resumeFile.getSize();
            logger.info("Resume Filename: {}", resumeFilename);
            logger.info("Resume Size: {} bytes", resumeSize);
        } else {
            logger.warn("No resume file received or file is empty.");
        }
        String jdSnippet = jobDescription != null ? jobDescription.substring(0, Math.min(jobDescription.length(), 50)) + "..." : "N/A";
        logger.info("Job Description Snippet: {}", jdSnippet);
        logger.info("--- END MOCK ANALYSIS REQUEST (NO DB SAVE) ---");

        return new AnalysisRequestAckDto(
                "Analysis request successfully received by backend (mock response, no DB save).",
                jobTitle,
                resumeFilename,
                resumeSize,
                jdSnippet,
                authenticatedUser.getId().toString()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsightSummaryDto> getInsightsHistoryForUser(User user) {
        logger.debug("Fetching insights history for user ID: {}", user.getId());
        List<Insights> insightsList = insightsRepository.findByUserOrderByCreatedAtDesc(user);
        return insightsList.stream()
                .map(insight -> new InsightSummaryDto(
                        insight.getId(),
                        insight.getJobTitle(),
                        insight.getCreatedAt(),
                        insight.getMatchScore() != null ? insight.getMatchScore().intValue() : null, // Convert to Integer for DTO
                        insight.getAtsScore(), // Add atsScore
                        insight.getResumeFilename()
                ))
                .collect(Collectors.toList());
    }

    // Overload to adapt to the new InsightSummaryDto if it changes
    // For now, the existing InsightSummaryDto does not have atsScore, so I'll update it.
    // See updated InsightSummaryDto below.

    @Override
    @Transactional(readOnly = true)
    public Optional<InsightDetailDto> getLatestInsightForUser(User user) {
        logger.debug("Fetching latest insight for user ID: {}", user.getId());
        return insightsRepository.findTopByUserOrderByCreatedAtDesc(user)
                .map(this::convertToDetailDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InsightDetailDto> getInsightByIdAndUser(UUID insightId, User user) {
        logger.debug("Fetching insight by ID: {} for user ID: {}", insightId, user.getId());
        return insightsRepository.findByIdAndUser(insightId, user)
                .map(this::convertToDetailDto);
    }

    private InsightDetailDto convertToDetailDto(Insights insight) {
        if (insight == null) {
            return null;
        }

        AnalysisResultDto analysisResultDto = null;
        if (insight.getAnalysisResult() != null) {
            try {
                // Convert the Map<String, Object> to AnalysisResultDto using ObjectMapper
                analysisResultDto = objectMapper.convertValue(insight.getAnalysisResult(), AnalysisResultDto.class);
            } catch (IllegalArgumentException e) {
                logger.error("Error converting analysisResult map to DTO for insight ID {}: {}", insight.getId(), e.getMessage(), e);
                // Optionally, create a default/error state for analysisResultDto or leave it null
            }
        }

        Integer matchScoreInt = insight.getMatchScore() != null ? insight.getMatchScore().intValue() : null;

        return new InsightDetailDto(
                insight.getId(),
                insight.getJobTitle(),
                insight.getResumeFilename(),
                insight.getCreatedAt(),
                matchScoreInt,
                insight.getAtsScore(),
                analysisResultDto
        );
    }
}
