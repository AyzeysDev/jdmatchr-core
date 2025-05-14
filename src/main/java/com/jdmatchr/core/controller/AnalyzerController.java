package com.jdmatchr.core.controller;

import com.jdmatchr.core.dto.InsightDetailDto;
import com.jdmatchr.core.dto.InsightSummaryDto;
import com.jdmatchr.core.dto.LatestInsightResponseDto;
// AnalysisRequestAckDto is not used by the primary /process endpoint anymore
// import com.jdmatchr.core.dto.AnalysisRequestAckDto;
import com.jdmatchr.core.entity.User;
import com.jdmatchr.core.repository.UserRepository;
import com.jdmatchr.core.service.AnalyzerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/insights") // Base path for all insight-related operations
public class AnalyzerController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzerController.class);

    private final AnalyzerService analyzerService;
    private final UserRepository userRepository;

    @Autowired
    public AnalyzerController(AnalyzerService analyzerService, UserRepository userRepository) {
        this.analyzerService = analyzerService;
        this.userRepository = userRepository;
    }

    /**
     * Processes the uploaded resume and job description, performs a mock analysis,
     * saves the result to the database, and returns details including the new insight ID.
     */
    @PostMapping("/process")
    public ResponseEntity<?> processDocuments(
            @RequestParam(value = "resumeFile", required = false) MultipartFile resumeFile,
            @RequestParam("jobTitle") String jobTitle,
            @RequestParam("jobDescription") String jobDescription,
            @AuthenticationPrincipal UserDetails userDetails // Injected by Spring Security if JWT is valid
    ) {
        if (userDetails == null) {
            logger.warn("POST /process: Request with no authenticated user.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("POST /process: Authenticated user with email {} not found in repository. This indicates a potential data integrity issue or misconfiguration.", email);
                    return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "User details not found for authenticated user.");
                });

        logger.info("POST /process: Received analysis request for user: {}, Job Title: {}", email, jobTitle);
        if (resumeFile == null || resumeFile.isEmpty()) {
            logger.warn("POST /process: No resume file provided by user {}.", email);
            // The service method analyzeDocuments handles null resumeFile
        }

        try {
            // Call the service method that performs mock analysis AND saves to DB
            Map<String, Object> analysisResultWithId = analyzerService.analyzeDocuments(resumeFile, jobTitle, jobDescription, user);

            if (analysisResultWithId.containsKey("insightId")) {
                logger.info("POST /process: Successfully processed and saved insight. ID: {}", analysisResultWithId.get("insightId"));
                return ResponseEntity.ok(analysisResultWithId);
            } else {
                logger.error("POST /process: Service method analyzeDocuments did not return an insightId for user {}", email);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Analysis completed but failed to retrieve result ID."));
            }
        } catch (RuntimeException e) {
            logger.error("POST /process: Error during document analysis for user {}: {}", email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Analysis failed: " + e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<InsightSummaryDto>> getInsightsHistory(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            logger.warn("GET /history: Request with no authenticated user.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Authenticated user not found for history."));

        logger.info("GET /history: Fetching history for user {}", user.getEmail());
        List<InsightSummaryDto> history = analyzerService.getInsightsHistoryForUser(user);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/latest")
    public ResponseEntity<LatestInsightResponseDto> getLatestInsight(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            logger.warn("GET /latest: Request with no authenticated user.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Authenticated user not found for latest insight."));

        logger.info("GET /latest: Fetching latest insight for user {}", user.getEmail());
        Optional<InsightDetailDto> latestInsightOpt = analyzerService.getLatestInsightForUser(user);

        UUID latestId = latestInsightOpt.map(InsightDetailDto::id).orElse(null);
        logger.info("GET /latest: Latest insight ID for user {}: {}", user.getEmail(), latestId);
        return ResponseEntity.ok(new LatestInsightResponseDto(latestId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InsightDetailDto> getInsightById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            logger.warn("GET /insights/{}: Request with no authenticated user.", id);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Authenticated user not found for insight ID " + id));

        logger.info("GET /insights/{}: Fetching insight for user {}", id, user.getEmail());
        return analyzerService.getInsightByIdAndUser(id, user)
                .map(insightDetailDto -> {
                    logger.info("GET /insights/{}: Found insight for user {}", id, user.getEmail());
                    return ResponseEntity.ok(insightDetailDto);
                })
                .orElseGet(() -> {
                    logger.warn("GET /insights/{}: Insight not found or not owned by user {}", id, user.getEmail());
                    return ResponseEntity.notFound().build();
                });
    }
}
