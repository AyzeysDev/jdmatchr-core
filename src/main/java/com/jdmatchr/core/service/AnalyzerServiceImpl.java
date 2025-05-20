// src/main/java/com/jdmatchr/core/service/AnalyzerServiceImpl.java
// This is the version you provided, with the specific logging for the prompt it generates.
package com.jdmatchr.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdmatchr.core.dto.*;
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
    // aiIoLogger removed.

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

        // Log the exact prompt being sent from AnalyzerServiceImpl using the main logger
        // Be mindful of PII and log verbosity in production.
        logger.info("Exact prompt constructed by PromptBuilderService (to be sent to AnalysisAiService from AnalyzerServiceImpl):\n{}", prompt);


        AnalysisResultDto analysisResultDto;
        try {
            logger.info("Sending prompt to AnalysisAiService for full analysis...");
            analysisResultDto = analysisAiService.getAnalysisFromAi(prompt);
            // Exact AI output and token counts are now logged within analysisAiService.getAnalysisFromAi using logger.info()
        } catch (Exception e) {
            logger.error("Failed to get analysis from AI for job title '{}': {}", jobTitle, e.getMessage(), e);
            throw new RuntimeException("AI analysis failed: " + e.getMessage(), e);
        }

        if (analysisResultDto.mockProcessingTimestamp() == null) {
            analysisResultDto = new AnalysisResultDto(
                    OffsetDateTime.now().toString(),
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

        logger.info("AI analysis complete. Match Score: {}, ATS Score: {}",
                analysisResultDto.matchScore(), analysisResultDto.atsScore());

        Map<String, Object> analysisResultMap = objectMapper.convertValue(analysisResultDto, new TypeReference<Map<String, Object>>() {});

        Insights newInsight = new Insights();
        newInsight.setUser(authenticatedUser);
        newInsight.setJobTitle(jobTitle);
        newInsight.setJobDescriptionSummary(jobDescription.substring(0, Math.min(jobDescription.length(), 250))
                + (jobDescription.length() > 250 ? "..." : ""));
        newInsight.setResumeFilename(originalResumeFilename);
        newInsight.setMatchScore(analysisResultDto.matchScore() != null ? analysisResultDto.matchScore().doubleValue() : null);
        newInsight.setAtsScore(analysisResultDto.atsScore());
        newInsight.setAnalysisResult(analysisResultMap);

        Insights savedInsight = insightsRepository.save(newInsight);
        logger.info("Saved new insight with ID: {} for user: {}", savedInsight.getId(), authenticatedUser.getEmail());

        return convertToDetailDto(savedInsight);
    }

    private InsightDetailDto convertToDetailDto(Insights insight) {
        if (insight == null) {
            return null;
        }
        AnalysisResultDto analysisResultDto = null;
        if (insight.getAnalysisResult() != null && !insight.getAnalysisResult().isEmpty()) {
            try {
                analysisResultDto = objectMapper.convertValue(insight.getAnalysisResult(), AnalysisResultDto.class);
            } catch (IllegalArgumentException e) {
                logger.error("Error converting analysisResult map to DTO for insight ID {}: {}", insight.getId(), e.getMessage(), e);
            }
        } else {
            logger.warn("Insight ID {} has null or empty analysisResult map.", insight.getId());
        }
        Integer matchScoreFromDto = (analysisResultDto != null) ? analysisResultDto.matchScore() : null;
        Integer atsScoreFromDto = (analysisResultDto != null) ? analysisResultDto.atsScore() : null;

        return new InsightDetailDto(
                insight.getId(),
                insight.getJobTitle(),
                insight.getResumeFilename(),
                insight.getCreatedAt(),
                matchScoreFromDto,
                atsScoreFromDto,
                analysisResultDto
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsightSummaryDto> getInsightsHistoryForUser(User user) {
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
                            logger.warn("Could not extract scores from analysisResult for summary of insight ID {}", insight.getId());
                        }
                    } else {
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