// src/main/java/com/jdmatchr/core/controller/AnalyzerController.java
package com.jdmatchr.core.controller;

import com.jdmatchr.core.dto.InsightDetailDto;
import com.jdmatchr.core.dto.InsightSummaryDto;
import com.jdmatchr.core.dto.LatestInsightResponseDto;
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
import java.util.Map; // Keep for error response Map.of("error", "message")
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
     * saves the result to the database, and returns the detailed insight.
     */
    @PostMapping("/process")
    public ResponseEntity<?> processDocuments( // Return type changed to ResponseEntity<?> to handle potential errors with Map
                                               @RequestParam(value = "resumeFile", required = false) MultipartFile resumeFile,
                                               @RequestParam("jobTitle") String jobTitle,
                                               @RequestParam("jobDescription") String jobDescription,
                                               @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            logger.warn("POST /process: Request with no authenticated user.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }

        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("POST /process: Authenticated user with email {} not found in repository.", email);
                    return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "User details not found for authenticated user.");
                });

        logger.info("POST /process: Received analysis request for user: {}, Job Title: {}", email, jobTitle);
        if (resumeFile == null || resumeFile.isEmpty()) {
            logger.warn("POST /process: No resume file provided by user {}. Mock filename will be used if service implies.", email);
        }

        try {
            // Call the service method that performs mock analysis AND saves to DB
            InsightDetailDto analysisResultDto = analyzerService.analyzeDocuments(resumeFile, jobTitle, jobDescription, user);

            if (analysisResultDto != null && analysisResultDto.id() != null) {
                logger.info("POST /process: Successfully processed and saved insight. ID: {}", analysisResultDto.id());
                return ResponseEntity.ok(analysisResultDto);
            } else {
                logger.error("POST /process: Service method analyzeDocuments did not return a valid InsightDetailDto for user {}", email);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Analysis completed but failed to retrieve result details."));
            }
        } catch (Exception e) { // Catching general exception for robustness
            logger.error("POST /process: Error during document analysis for user {}: {}", email, e.getMessage(), e);
            // Consider creating a more structured error response if needed, similar to ApiErrorResponse
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
    public ResponseEntity<?> getLatestInsight(@AuthenticationPrincipal UserDetails userDetails) { // Changed to ResponseEntity<?>
        if (userDetails == null) {
            logger.warn("GET /latest: Request with no authenticated user.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Authenticated user not found for latest insight."));

        logger.info("GET /latest: Fetching latest insight for user {}", user.getEmail());
        Optional<InsightDetailDto> latestInsightOpt = analyzerService.getLatestInsightForUser(user);

        if (latestInsightOpt.isPresent()) {
            // If you want to return the full InsightDetailDto for /latest:
            // return ResponseEntity.ok(latestInsightOpt.get());

            // If you want to return just the ID as per the old LatestInsightResponseDto:
            UUID latestId = latestInsightOpt.get().id();
            logger.info("GET /latest: Latest insight ID for user {}: {}", user.getEmail(), latestId);
            return ResponseEntity.ok(new LatestInsightResponseDto(latestId));
            // For now, I'll keep the LatestInsightResponseDto as the problem description implies a specific structure for POST and GET /{id}
            // but doesn't explicitly state /latest should also change to the full DTO.
            // If /latest should also return the full InsightDetailDto, change the return type to ResponseEntity<InsightDetailDto>
            // and return ResponseEntity.ok(latestInsightOpt.get());
        } else {
            logger.info("GET /latest: No insights found for user {}", user.getEmail());
            // Return 200 OK with null ID as per previous LatestInsightResponseDto behavior for no insights
            return ResponseEntity.ok(new LatestInsightResponseDto(null));
        }
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
