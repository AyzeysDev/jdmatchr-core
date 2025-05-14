package com.jdmatchr.core.service;

import com.jdmatchr.core.dto.InsightDetailDto;
import com.jdmatchr.core.dto.InsightSummaryDto;
import com.jdmatchr.core.entity.Insights;
import com.jdmatchr.core.entity.User;
import com.jdmatchr.core.repository.InsightsRepository;
import com.jdmatchr.core.repository.UserRepository; // Keep if used by analyzeDocuments
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random; // For mock data
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AnalyzerServiceImpl implements AnalyzerService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzerServiceImpl.class);

    private final UserRepository userRepository; // Keep if used by analyzeDocuments
    private final InsightsRepository insightsRepository;
    // private final AiModelClient aiModelClient; // For actual AI calls later

    @Autowired
    public AnalyzerServiceImpl(UserRepository userRepository, InsightsRepository insightsRepository) {
        this.userRepository = userRepository;
        this.insightsRepository = insightsRepository;
    }

    @Override
    @Transactional // Make sure this is transactional if saving entities
    public Map<String, Object> analyzeDocuments(
            MultipartFile resumeFile,
            String jobTitle,
            String jobDescription,
            User authenticatedUser // Changed from username string to User entity
    ) {
        logger.info("Analyzing documents for user ID: {}, Job Title: {}", authenticatedUser.getId(), jobTitle);

        // 1. Resume Text Extraction (placeholder)
        String resumeText;
        try {
            resumeText = "Extracted text from " + resumeFile.getOriginalFilename() + " (content length: " + resumeFile.getSize() + " bytes)";
            logger.info("Mock resume text extraction successful for: {}", resumeFile.getOriginalFilename());
        } catch (Exception e) {
            logger.error("Failed to process resume file: {}", resumeFile.getOriginalFilename(), e);
            throw new RuntimeException("Failed to read resume file.", e);
        }

        // 2. Call AI Model (Mocked for now)
        Random random = new Random();
        int atsScorePercentage = 60 + random.nextInt(41); // Score between 60-100
        Map<String, Object> analysisResultData = Map.of(
                "keywordMatches", List.of("Java", "Spring", (atsScorePercentage > 80 ? "Advanced SQL" : "Basic SQL")),
                "missingKeywords", List.of("Kubernetes", (atsScorePercentage < 70 ? "CloudFormation" : "Terraform")),
                "suggestions", List.of(
                        "Consider highlighting your project leadership skills.",
                        "Quantify achievements in your past roles with metrics."
                ),
                "overallSentiment", atsScorePercentage > 75 ? "Positive" : "Needs Improvement",
                "atsScoreRaw", atsScorePercentage // Store the raw score if needed
        );
        logger.info("Mock AI analysis complete. ATS Score: {}%", atsScorePercentage);

        // 3. Save Insights to Database
        Insights newInsight = new Insights();
        newInsight.setUser(authenticatedUser);
        newInsight.setJobTitle(jobTitle);
        // newInsight.setJobDescriptionSummary(...); // Populate if you extract this
        newInsight.setResumeFilename(resumeFile.getOriginalFilename());
        newInsight.setMatchScore((double) atsScorePercentage); // Save the score
        newInsight.setAnalysisResult(analysisResultData); // Save the detailed JSON
        // newInsight.setCreatedAt() is handled by @PrePersist in the entity

        Insights savedInsight = insightsRepository.save(newInsight);
        logger.info("Saved new insight with ID: {} for user: {}", savedInsight.getId(), authenticatedUser.getEmail());

        // 4. Construct and Return Response for the analyzeDocuments call
        // This map is what the Next.js /api/analyze/process route will forward
        return Map.of(
                "insightId", savedInsight.getId().toString(),
                "jobTitle", savedInsight.getJobTitle(),
                "matchScore", savedInsight.getMatchScore(),
                "analysisResult", savedInsight.getAnalysisResult(), // Send the detailed results
                "analysisDate", savedInsight.getCreatedAt().toString()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsightSummaryDto> getInsightsHistoryForUser(User user) {
        logger.debug("Fetching insights history for user ID: {}", user.getId());
        List<Insights> insightsList = insightsRepository.findByUserOrderByCreatedAtDesc(user);
        // If insightsList is empty, this will correctly return an empty list.
        return insightsList.stream()
                .map(insight -> new InsightSummaryDto(
                        insight.getId(),
                        insight.getJobTitle(),
                        insight.getCreatedAt(),
                        insight.getMatchScore(),
                        insight.getResumeFilename()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InsightDetailDto> getLatestInsightForUser(User user) {
        logger.debug("Fetching latest insight for user ID: {}", user.getId());
        return insightsRepository.findTopByUserOrderByCreatedAtDesc(user)
                .map(this::convertToDetailDto); // Convert to DTO
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InsightDetailDto> getInsightByIdAndUser(UUID insightId, User user) {
        logger.debug("Fetching insight by ID: {} for user ID: {}", insightId, user.getId());
        return insightsRepository.findByIdAndUser(insightId, user)
                .map(this::convertToDetailDto); // Convert to DTO
    }

    // Helper method to convert Insight entity to InsightDetailDto
    private InsightDetailDto convertToDetailDto(Insights insight) {
        return new InsightDetailDto(
                insight.getId(),
                insight.getJobTitle(),
                insight.getJobDescriptionSummary(),
                insight.getResumeFilename(),
                insight.getMatchScore(),
                insight.getAnalysisResult(),
                insight.getCreatedAt()
        );
    }
}
