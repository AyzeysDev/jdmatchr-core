// src/main/java/com/jdmatchr/core/service/AnalyzerServiceImpl.java
// (Modifications to integrate new services)
package com.jdmatchr.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdmatchr.core.dto.*; // Imports all DTOs from the package
import com.jdmatchr.core.entity.Insights;
import com.jdmatchr.core.entity.User;
import com.jdmatchr.core.repository.InsightsRepository;
import com.jdmatchr.core.repository.UserRepository; // Kept for now, but might not be needed directly here
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AnalyzerServiceImpl implements AnalyzerService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzerServiceImpl.class);

    private final UserRepository userRepository; // May not be needed if User is passed directly
    private final InsightsRepository insightsRepository;
    private final ObjectMapper objectMapper;
    private final PdfParserService pdfParserService;
    private final PromptBuilderService promptBuilderService;
    private final AnalysisAiService analysisAiService;

    @Autowired
    public AnalyzerServiceImpl(UserRepository userRepository,
                               InsightsRepository insightsRepository,
                               ObjectMapper objectMapper, // Spring Boot auto-configures this
                               PdfParserService pdfParserService,
                               PromptBuilderService promptBuilderService,
                               AnalysisAiService analysisAiService) {
        this.userRepository = userRepository;
        this.insightsRepository = insightsRepository;
        this.objectMapper = objectMapper;
        this.pdfParserService = pdfParserService;
        this.promptBuilderService = promptBuilderService;
        this.analysisAiService = analysisAiService;
    }

    @Override
    @Transactional // Make this transactional as it involves DB write
    public InsightDetailDto analyzeDocuments(
            MultipartFile resumeFile,
            String jobTitle,
            String jobDescription,
            User authenticatedUser // Directly use the User entity passed from controller
    ) {
        logger.info("analyzeDocuments service called for user ID: {}, Job Title: {}", authenticatedUser.getId(), jobTitle);

        String resumeText;
        String originalResumeFilename = "N/A"; // Default if no file
        long resumeFileSize = 0;

        if (resumeFile != null && !resumeFile.isEmpty()) {
            originalResumeFilename = resumeFile.getOriginalFilename();
            resumeFileSize = resumeFile.getSize();
            try {
                logger.info("Parsing resume file: {}", originalResumeFilename);
                resumeText = pdfParserService.parsePdf(resumeFile);
                if (resumeText.isBlank()) {
                    logger.warn("Extracted resume text is blank for file: {}", originalResumeFilename);
                    // Decide handling: throw error, or proceed with blank resumeText?
                    // For now, proceed, AI might handle it or prompt can be adjusted.
                }
            } catch (IOException e) {
                logger.error("Failed to parse resume PDF '{}': {}", originalResumeFilename, e.getMessage(), e);
                // Consider throwing a specific business exception to be caught by controller
                throw new RuntimeException("Error processing resume file: " + e.getMessage(), e);
            }
        } else {
            logger.warn("No resume file provided for job title: {}. Proceeding without resume text.", jobTitle);
            resumeText = ""; // Or handle as an error if resume is mandatory
        }

        logger.info("Building prompt for AI analysis. Job Title: {}, Resume Text Length: {}", jobTitle, resumeText.length());
        String prompt = promptBuilderService.buildPrompt(jobTitle, jobDescription, resumeText);

        AnalysisResultDto analysisResultDto;
        try {
            logger.info("Sending prompt to AI for analysis...");
            analysisResultDto = analysisAiService.getAnalysisFromAi(prompt);
        } catch (Exception e) { // Catch JsonProcessingException or RuntimeException from AnalysisAiService
            logger.error("Failed to get analysis from AI for job title '{}': {}", jobTitle, e.getMessage(), e);
            throw new RuntimeException("AI analysis failed: " + e.getMessage(), e);
        }

        // If mockProcessingTimestamp is not part of AI response, set it here.
        // For this example, I'll create a new DTO instance if it's immutable (record)
        // or set the field if it's a mutable class.
        // Assuming AnalysisResultDto is a record and we want to add the timestamp:
        if (analysisResultDto.mockProcessingTimestamp() == null) {
            analysisResultDto = new AnalysisResultDto(
                    OffsetDateTime.now().toString(), // Set current timestamp
                    analysisResultDto.matchScore(),
                    analysisResultDto.atsScore(),
                    analysisResultDto.jdSummary(),
                    analysisResultDto.fluffAnalysis(),
                    analysisResultDto.roleFitAndAlignmentMetrics(),
                    analysisResultDto.keywordAnalysis(),
                    analysisResultDto.resumeSuggestions(),
                    analysisResultDto.interviewPreparationTopics()
            );
        }


        logger.info("AI analysis complete. Match Score: {}, ATS Score: {}", analysisResultDto.matchScore(), analysisResultDto.atsScore());

        // Convert AnalysisResultDto to Map<String, Object> for database persistence (Option A)
        Map<String, Object> analysisResultMap = objectMapper.convertValue(analysisResultDto, new TypeReference<Map<String, Object>>() {});

        Insights newInsight = new Insights();
        newInsight.setUser(authenticatedUser);
        newInsight.setJobTitle(jobTitle);
        newInsight.setJobDescriptionSummary(jobDescription.substring(0, Math.min(jobDescription.length(), 250)) + (jobDescription.length() > 250 ? "..." : ""));
        newInsight.setResumeFilename(originalResumeFilename);
        // Scores are now part of AnalysisResultDto, which is stored in analysisResultMap.
        // The Insights entity's direct matchScore and atsScore fields can be populated from the DTO for quick access/querying if desired.
        newInsight.setMatchScore(analysisResultDto.matchScore() != null ? analysisResultDto.matchScore().doubleValue() : null);
        newInsight.setAtsScore(analysisResultDto.atsScore());
        newInsight.setAnalysisResult(analysisResultMap);
        // newInsight.setCreatedAt() is handled by @PrePersist in Insights entity

        Insights savedInsight = insightsRepository.save(newInsight);
        logger.info("Saved new insight with ID: {} for user: {}", savedInsight.getId(), authenticatedUser.getEmail());

        // Construct and Return DTO (InsightDetailDto)
        // The convertToDetailDto method will need to correctly map from the savedInsight,
        // especially the analysisResultMap back to AnalysisResultDto.
        return convertToDetailDto(savedInsight);
    }


    // This method remains largely the same, but ensure it correctly reconstructs
    // AnalysisResultDto from the Map<String, Object> stored in the Insights entity.
    private InsightDetailDto convertToDetailDto(Insights insight) {
        if (insight == null) {
            return null;
        }

        AnalysisResultDto analysisResultDto = null;
        if (insight.getAnalysisResult() != null && !insight.getAnalysisResult().isEmpty()) {
            try {
                // Convert the Map<String, Object> from entity back to AnalysisResultDto
                analysisResultDto = objectMapper.convertValue(insight.getAnalysisResult(), AnalysisResultDto.class);
            } catch (IllegalArgumentException e) {
                logger.error("Error converting analysisResult map to DTO for insight ID {}: {}", insight.getId(), e.getMessage(), e);
            }
        } else {
            logger.warn("Insight ID {} has null or empty analysisResult map.", insight.getId());
        }

        // Use the direct fields from Insight entity for scores if they are populated,
        // otherwise, they should be within the analysisResultDto.
        // The prompt now puts matchScore and atsScore INSIDE the main JSON from AI.
        Integer matchScoreFromDto = (analysisResultDto != null) ? analysisResultDto.matchScore() : null;
        Integer atsScoreFromDto = (analysisResultDto != null) ? analysisResultDto.atsScore() : null;


        return new InsightDetailDto(
                insight.getId(),
                insight.getJobTitle(),
                insight.getResumeFilename(),
                insight.getCreatedAt(),
                matchScoreFromDto, // Get from parsed DTO
                atsScoreFromDto,   // Get from parsed DTO
                analysisResultDto
        );
    }

    // --- Other existing methods (getInsightsHistoryForUser, getLatestInsightForUser, etc.) ---
    // These methods would also use convertToDetailDto or a similar conversion for summaries.
    // Ensure InsightSummaryDto also gets its matchScore and atsScore correctly.
    // The mock method processAnalysisRequestMock can be kept or removed as per your needs.

    @Override
    @Transactional(readOnly = true)
    public List<InsightSummaryDto> getInsightsHistoryForUser(User user) {
        logger.debug("Fetching insights history for user ID: {}", user.getId());
        List<Insights> insightsList = insightsRepository.findByUserOrderByCreatedAtDesc(user);
        return insightsList.stream()
                .map(insight -> {
                    // For summary, we might need to extract scores from the JSON if not top-level on Insight entity
                    Integer matchScore = null;
                    Integer atsScore = null;
                    if (insight.getAnalysisResult() != null) {
                        // A bit inefficient to parse full DTO for summary, but works.
                        // Alternatively, could fetch specific fields from JSON map.
                        try {
                            AnalysisResultDto tempDto = objectMapper.convertValue(insight.getAnalysisResult(), AnalysisResultDto.class);
                            matchScore = tempDto.matchScore();
                            atsScore = tempDto.atsScore();
                        } catch (Exception e) {
                            logger.warn("Could not parse scores from analysisResult for summary of insight ID {}", insight.getId());
                        }
                    } else {
                        // Fallback to direct entity fields if they are still maintained (they are in current Insights entity)
                        matchScore = insight.getMatchScore() != null ? insight.getMatchScore().intValue() : null;
                        atsScore = insight.getAtsScore();
                    }

                    return new InsightSummaryDto(
                            insight.getId(),
                            insight.getJobTitle(),
                            insight.getCreatedAt(),
                            matchScore,
                            atsScore,
                            insight.getResumeFilename()
                    );
                })
                .collect(Collectors.toList());
    }

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

    // Mock method - can be removed or updated if no longer needed.
    @Override
    @Transactional(readOnly = true)
    public AnalysisRequestAckDto processAnalysisRequestMock(
            MultipartFile resumeFile,
            String jobTitle,
            String jobDescription,
            User authenticatedUser
    ) {
        logger.info("--- MOCK ANALYSIS REQUEST RECEIVED (NO DB SAVE) ---");
        // ... (rest of the mock implementation)
        return new AnalysisRequestAckDto(
                "Analysis request successfully received by backend (mock response, no DB save).",
                jobTitle,
                (resumeFile != null ? resumeFile.getOriginalFilename() : "N/A"),
                (resumeFile != null ? resumeFile.getSize() : 0),
                jobDescription.substring(0, Math.min(jobDescription.length(), 50)) + "...",
                authenticatedUser.getId().toString()
        );
    }
}