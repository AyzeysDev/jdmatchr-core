package com.jdmatchr.core.service;

import com.jdmatchr.core.dto.AnalysisRequestAckDto;
import com.jdmatchr.core.dto.InsightDetailDto;
import com.jdmatchr.core.dto.InsightSummaryDto;
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

// import java.io.IOException; // Only if actually reading file bytes
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AnalyzerServiceImpl implements AnalyzerService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzerServiceImpl.class);

    private final UserRepository userRepository;
    private final InsightsRepository insightsRepository;
    // private final AiModelClient aiModelClient; // For actual AI calls later

    @Autowired
    public AnalyzerServiceImpl(UserRepository userRepository, InsightsRepository insightsRepository) {
        this.userRepository = userRepository;
        this.insightsRepository = insightsRepository;
    }

    @Override
    @Transactional // This operation involves saving to the database
    public Map<String, Object> analyzeDocuments(
            MultipartFile resumeFile,
            String jobTitle,
            String jobDescription,
            User authenticatedUser // Service method now directly accepts the User entity
    ) {
        logger.info("analyzeDocuments service called for user ID: {}, Job Title: {}", authenticatedUser.getId(), jobTitle);

        String originalResumeFilename = "N/A";
        long resumeFileSize = 0;

        if (resumeFile != null && !resumeFile.isEmpty()) {
            originalResumeFilename = resumeFile.getOriginalFilename();
            resumeFileSize = resumeFile.getSize();
            logger.info("Resume received: {}, Size: {} bytes", originalResumeFilename, resumeFileSize);
            // In a real scenario, you'd extract text here.
        } else {
            logger.warn("No resume file provided or file is empty for job title: {}", jobTitle);
        }

        // 2. Mock AI Model Call & Result Generation
        Random random = new Random();
        double matchScore = 60.0 + (random.nextInt(401) / 10.0); // Score between 60.0-100.0

        Map<String, Object> analysisResultData = Map.of(
                "overallMatchScore", String.format("%.1f%%", matchScore),
                "keywordAnalysis", Map.of(
                        "matchedKeywords", List.of("Java", "Spring Boot", (matchScore > 85 ? "Microservices" : "REST APIs")),
                        "missingKeywords", List.of("Kubernetes", (matchScore < 70 ? "AWS" : "Docker")),
                        "keywordDensityScore", random.nextInt(51) + 50
                ),
                "resumeSuggestions", List.of(
                        "Consider highlighting your experience with " + (matchScore < 75 ? "project management." : "scalable systems."),
                        "Quantify achievements in your 'XYZ' role with specific metrics.",
                        "Tailor the summary section to better reflect the job requirements for '" + jobTitle + "'."
                ),
                "interviewPreparationTopics", List.of(
                        // Corrected: Use a property from authenticatedUser for the dummy condition
                        "Be prepared to discuss your experience with " + (authenticatedUser.getEmail() != null && authenticatedUser.getEmail().contains("a") ? "Java concurrency." : "Spring Security."),
                        "Research common behavioral questions related to teamwork and problem-solving.",
                        "Formulate questions to ask the interviewer about the role and company culture."
                ),
                "mockProcessingTimestamp", OffsetDateTime.now().toString()
        );
        logger.info("Mock AI analysis complete for job title: '{}'. Match Score: {}%", jobTitle, matchScore);

        // 3. Save Insights to Database
        Insights newInsight = new Insights();
        newInsight.setUser(authenticatedUser);
        newInsight.setJobTitle(jobTitle);
        newInsight.setJobDescriptionSummary(jobDescription.substring(0, Math.min(jobDescription.length(), 500)) + (jobDescription.length() > 500 ? "..." : ""));
        newInsight.setResumeFilename(originalResumeFilename);
        newInsight.setMatchScore(matchScore);
        newInsight.setAnalysisResult(analysisResultData);
        // newInsight.setCreatedAt() is handled by @PrePersist in the Insights entity

        Insights savedInsight = insightsRepository.save(newInsight);
        logger.info("Saved new insight with ID: {} for user: {}", savedInsight.getId(), authenticatedUser.getEmail());

        // 4. Construct and Return Response for the analyzeDocuments call
        return Map.of(
                "insightId", savedInsight.getId().toString(),
                "jobTitle", savedInsight.getJobTitle(),
                "matchScore", savedInsight.getMatchScore(),
                "analysisResult", savedInsight.getAnalysisResult(),
                "analysisDate", savedInsight.getCreatedAt().toString()
        );
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
