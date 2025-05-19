// src/main/java/com/jdmatchr/core/controller/AnalyzerController.java
package com.jdmatchr.core.controller;

import com.jdmatchr.core.dto.InsightDetailDto;
// import com.jdmatchr.core.dto.InsightResponseDto; // No longer needed for /process success response
import com.jdmatchr.core.dto.InsightSummaryDto;
import com.jdmatchr.core.dto.LatestInsightResponseDto;
import com.jdmatchr.core.dto.ApiErrorResponse;
import com.jdmatchr.core.entity.User;
import com.jdmatchr.core.repository.UserRepository;
import com.jdmatchr.core.service.AnalyzerService;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/insights")
public class AnalyzerController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzerController.class);

    private final AnalyzerService analyzerService;
    private final UserRepository userRepository;

    @Autowired
    public AnalyzerController(AnalyzerService analyzerService, UserRepository userRepository) {
        this.analyzerService = analyzerService;
        this.userRepository = userRepository;
    }

    @PostMapping("/process")
    public ResponseEntity<?> processDocuments(
            @RequestParam(value = "resumeFile", required = false) MultipartFile resumeFile,
            @RequestParam("jobTitle") String jobTitle,
            @RequestParam("jobDescription") String jobDescription,
            @AuthenticationPrincipal UserDetails springUserDetails,
            HttpServletRequest request
    ) {
        if (springUserDetails == null) {
            logger.warn("POST /process: Request with no authenticated user details.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", "User not authenticated", request.getRequestURI()));
        }

        User authenticatedUser = userRepository.findByEmail(springUserDetails.getUsername())
                .orElseThrow(() -> {
                    logger.error("POST /process: Authenticated user with email {} not found in repository.", springUserDetails.getUsername());
                    return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Authenticated user details not found in system.");
                });

        logger.info("POST /process: Received analysis request for user: {}, Job Title: {}", authenticatedUser.getEmail(), jobTitle);

        if ((resumeFile == null || resumeFile.isEmpty()) && (jobDescription == null || jobDescription.isBlank())) {
            logger.warn("POST /process: Both resume file and job description are empty/null for user {}.", authenticatedUser.getEmail());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request", "Resume file or job description must be provided.", request.getRequestURI()));
        }

        try {
            InsightDetailDto fullInsightDetail = analyzerService.analyzeDocuments(resumeFile, jobTitle, jobDescription, authenticatedUser);

            if (fullInsightDetail != null && fullInsightDetail.id() != null) {
                logger.info("POST /process: Successfully processed and saved insight. ID: {}", fullInsightDetail.id());
                // Return the full InsightDetailDto as requested for the frontend
                return ResponseEntity.ok(fullInsightDetail); // MODIFIED HERE
            } else {
                logger.error("POST /process: Service method analyzeDocuments did not return a valid InsightDetailDto for user {}", authenticatedUser.getEmail());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Processing Error", "Analysis completed but failed to retrieve structured result details.", request.getRequestURI()));
            }
        } catch (RuntimeException e) {
            logger.error("POST /process: Error during document analysis for user {}: {}", authenticatedUser.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Analysis Failed", e.getMessage(), request.getRequestURI()));
        } catch (Exception e) {
            logger.error("POST /process: Unexpected error for user {}: {}", authenticatedUser.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", "An unexpected error occurred during the analysis process.", request.getRequestURI()));
        }
    }

    // --- GET Endpoints Reverted to User's Previous Working Style ---

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
    public ResponseEntity<?> getLatestInsight(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            logger.warn("GET /latest: Request with no authenticated user.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Authenticated user not found for latest insight."));

        logger.info("GET /latest: Fetching latest insight for user {}", user.getEmail());
        Optional<InsightDetailDto> latestInsightOpt = analyzerService.getLatestInsightForUser(user);

        if (latestInsightOpt.isPresent()) {
            UUID latestId = latestInsightOpt.get().id();
            logger.info("GET /latest: Latest insight ID for user {}: {}", user.getEmail(), latestId);
            return ResponseEntity.ok(new LatestInsightResponseDto(latestId));
        } else {
            logger.info("GET /latest: No insights found for user {}", user.getEmail());
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
