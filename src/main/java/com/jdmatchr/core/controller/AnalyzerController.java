package com.jdmatchr.core.controller;

import com.jdmatchr.core.dto.InsightDetailDto;
import com.jdmatchr.core.dto.InsightSummaryDto;
import com.jdmatchr.core.dto.LatestInsightResponseDto;
import com.jdmatchr.core.entity.User;
import com.jdmatchr.core.repository.UserRepository; // To fetch User entity
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
import org.springframework.web.server.ResponseStatusException; // For cleaner error responses

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/insights") // Changed base path to /insights for these operations
public class AnalyzerController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzerController.class);

    private final AnalyzerService analyzerService;
    private final UserRepository userRepository; // To fetch the full User entity

    @Autowired
    public AnalyzerController(AnalyzerService analyzerService, UserRepository userRepository) {
        this.analyzerService = analyzerService;
        this.userRepository = userRepository;
    }

    // Endpoint for the initial analysis processing
    // Keeping it separate for clarity, though it could be under /insights
    @PostMapping("/process") // Changed from /analyzer/process-documents to /insights/process
    public ResponseEntity<?> processDocuments(
            @RequestParam("resumeFile") MultipartFile resumeFile,
            @RequestParam("jobTitle") String jobTitle,
            @RequestParam("jobDescription") String jobDescription,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            logger.warn("Process documents request with no authenticated user.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("Authenticated user with email {} not found in repository.", email);
                    // This should ideally not happen if JWT authentication is working correctly
                    return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "User details not found for authenticated user.");
                });

        logger.info("Received analysis request for user: {}, Job Title: {}", email, jobTitle);

        try {
            Map<String, Object> analysisResult = analyzerService.analyzeDocuments(resumeFile, jobTitle, jobDescription, user);
            return ResponseEntity.ok(analysisResult);
        } catch (RuntimeException e) {
            logger.error("Error during document analysis for user {}: {}", email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Analysis failed: " + e.getMessage()));
        }
    }


    @GetMapping("/history")
    public ResponseEntity<List<InsightSummaryDto>> getInsightsHistory(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Authenticated user not found"));

        List<InsightSummaryDto> history = analyzerService.getInsightsHistoryForUser(user);
        // If history is empty, an empty list [] will be returned with 200 OK, which is fine.
        return ResponseEntity.ok(history);
    }

    @GetMapping("/latest")
    public ResponseEntity<LatestInsightResponseDto> getLatestInsightId(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Authenticated user not found"));

        Optional<InsightDetailDto> latestInsightOpt = analyzerService.getLatestInsightForUser(user);

        if (latestInsightOpt.isPresent()) {
            return ResponseEntity.ok(new LatestInsightResponseDto(latestInsightOpt.get().id()));
        } else {
            // No latest insight found for the user
            return ResponseEntity.ok(new LatestInsightResponseDto(null)); // Return 200 OK with null ID
            // Alternatively, could return ResponseEntity.noContent().build(); (HTTP 204)
            // but returning a consistent DTO structure might be easier for the frontend.
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<InsightDetailDto> getInsightById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Authenticated user not found"));

        Optional<InsightDetailDto> insightOpt = analyzerService.getInsightByIdAndUser(id, user);

        return insightOpt
                .map(ResponseEntity::ok) // If present, wrap in ResponseEntity.ok()
                .orElseGet(() -> ResponseEntity.notFound().build()); // If not present, return 404
    }
}
