// src/main/java/com/jdmatchr/core/service/AnalyzerServiceImpl.java
package com.jdmatchr.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdmatchr.core.dto.*;
import com.jdmatchr.core.entity.Insights; // Ensure this import is present
import com.jdmatchr.core.entity.User; // Ensure this import is present
import com.jdmatchr.core.repository.InsightsRepository;
import com.jdmatchr.core.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime; // Ensure this is imported for InsightSummaryDto
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AnalyzerServiceImpl implements AnalyzerService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzerServiceImpl.class);

    private final UserRepository userRepository;
    private final InsightsRepository insightsRepository;
    private final ObjectMapper objectMapper;
    private final PdfParserService pdfParserService;
    private final PromptBuilderService promptBuilderService;
    private final AnalysisAiService analysisAiService;

    @Autowired
    public AnalyzerServiceImpl(UserRepository userRepository,
                               InsightsRepository insightsRepository,
                               ObjectMapper objectMapper,
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
    @Transactional
    public InsightDetailDto analyzeDocuments(
            MultipartFile resumeFile,
            String jobTitle,
            String jobDescription,
            User authenticatedUser
    ) {
        // ... (previous logic for resume parsing, prompt building, AI call remains the same) ...
        logger.info("analyzeDocuments service called for user ID: {}, Job Title: {}", authenticatedUser.getId(), jobTitle);

        String resumeText;
        String originalResumeFilename = "N/A";

        if (resumeFile != null && !resumeFile.isEmpty()) {
            originalResumeFilename = resumeFile.getOriginalFilename();
            try {
                logger.info("Parsing resume file: {}", originalResumeFilename);
                resumeText = pdfParserService.parsePdf(resumeFile);
                if (resumeText.isBlank()) {
                    logger.warn("Extracted resume text is blank for file: {}", originalResumeFilename);
                }
            } catch (IOException e) {
                logger.error("Failed to parse resume PDF '{}': {}", originalResumeFilename, e.getMessage(), e);
                throw new RuntimeException("Error processing resume file: " + e.getMessage(), e);
            }
        } else {
            logger.warn("No resume file provided for job title: {}. Proceeding without resume text.", jobTitle);
            resumeText = "";
        }

        logger.info("Building prompt for AI analysis. Job Title: {}, JD Length (chars): {}, Resume Text Length (chars): {}",
                jobTitle, jobDescription.length(), resumeText.length());
        String prompt = promptBuilderService.buildPrompt(jobTitle, jobDescription, resumeText);

        logger.info("Exact prompt constructed by PromptBuilderService (to be sent to AnalysisAiService from AnalyzerServiceImpl):\n{}", prompt);

        AnalysisResultDto analysisResultDtoFromAi;
        try {
            logger.info("Sending prompt to AnalysisAiService for full analysis...");
            analysisResultDtoFromAi = analysisAiService.getAnalysisFromAi(prompt);
        } catch (Exception e) {
            logger.error("Failed to get analysis from AI for job title '{}': {}", jobTitle, e.getMessage(), e);
            throw new RuntimeException("AI analysis failed: " + e.getMessage(), e);
        }

        logger.info("AI analysis complete. Match Score from AI: {}, ATS Score from AI: {}",
                analysisResultDtoFromAi.matchScore(), analysisResultDtoFromAi.atsScore());

        Map<String, Object> analysisResultMapToStore = objectMapper.convertValue(analysisResultDtoFromAi, new TypeReference<Map<String, Object>>() {});

        Insights newInsight = new Insights();
        newInsight.setUser(authenticatedUser);
        newInsight.setJobTitle(jobTitle);
        newInsight.setJobDescriptionSummary(jobDescription.substring(0, Math.min(jobDescription.length(), 250))
                + (jobDescription.length() > 250 ? "..." : ""));
        newInsight.setResumeFilename(originalResumeFilename);
        newInsight.setMatchScore(analysisResultDtoFromAi.matchScore() != null ? analysisResultDtoFromAi.matchScore().doubleValue() : null);
        newInsight.setAtsScore(analysisResultDtoFromAi.atsScore());
        newInsight.setAnalysisResult(analysisResultMapToStore);

        Insights savedInsight = insightsRepository.save(newInsight);
        logger.info("Saved new insight with ID: {} for user: {}", savedInsight.getId(), authenticatedUser.getEmail());

        return convertToDetailDto(savedInsight);
    }

    private InsightDetailDto convertToDetailDto(Insights insight) {
        if (insight == null) {
            return null;
        }
        AnalysisResultDto analysisResultForDetailDto = null;
        if (insight.getAnalysisResult() != null && !insight.getAnalysisResult().isEmpty()) {
            try {
                analysisResultForDetailDto = objectMapper.convertValue(insight.getAnalysisResult(), AnalysisResultDto.class);
            } catch (IllegalArgumentException e) {
                logger.error("Error converting analysisResult map to DTO for insight ID {}: {}", insight.getId(), e.getMessage(), e);
            }
        } else {
            logger.warn("Insight ID {} has null or empty analysisResult map.", insight.getId());
        }

        return new InsightDetailDto(
                insight.getId(),
                insight.getJobTitle(),
                insight.getResumeFilename(),
                insight.getCreatedAt(), // RE-ADDED for InsightDetailDto constructor
                analysisResultForDetailDto
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsightSummaryDto> getInsightsHistoryForUser(User user) {
        // This method remains unchanged from the previous version,
        // as InsightSummaryDto was not part of this specific change request
        // and already includes createdAt, matchScore, and atsScore.
        logger.debug("Fetching insights history for user ID: {}", user.getId());
        List<Insights> insightsList = insightsRepository.findByUserOrderByCreatedAtDesc(user);
        return insightsList.stream()
                .map(insight -> {
                    Integer matchScore = null;
                    Integer atsScore = null;
                    if (insight.getAnalysisResult() != null) {
                        try {
                            Map<String, Object> analysisMap = insight.getAnalysisResult();
                            Object matchScoreObj = analysisMap.get("matchScore");
                            Object atsScoreObj = analysisMap.get("atsScore");
                            if (matchScoreObj instanceof Number) {
                                matchScore = ((Number) matchScoreObj).intValue();
                            }
                            if (atsScoreObj instanceof Number) {
                                atsScore = ((Number) atsScoreObj).intValue();
                            }
                        } catch (Exception e) {
                            logger.warn("Could not extract scores from analysisResult map for summary of insight ID {}", insight.getId());
                        }
                    }
                    if (matchScore == null && insight.getMatchScore() != null) {
                        matchScore = insight.getMatchScore().intValue();
                    }
                    if (atsScore == null && insight.getAtsScore() != null) {
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

    @Override
    @Transactional(readOnly = true)
    public AnalysisRequestAckDto processAnalysisRequestMock(
            MultipartFile resumeFile,
            String jobTitle,
            String jobDescription,
            User authenticatedUser
    ) {
        logger.info("--- MOCK ANALYSIS REQUEST RECEIVED (NO DB SAVE) ---");
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