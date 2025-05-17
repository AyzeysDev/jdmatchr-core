// src/main/java/com/jdmatchr/core/service/AnalyzerService.java
package com.jdmatchr.core.service;

import com.jdmatchr.core.dto.AnalysisRequestAckDto;
import com.jdmatchr.core.dto.InsightDetailDto; // Ensure this is imported
import com.jdmatchr.core.dto.InsightSummaryDto;
import com.jdmatchr.core.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
// Removed import java.util.Map; as analyzeDocuments no longer returns it.

public interface AnalyzerService {

    /**
     * Processes the uploaded resume and job description, performs mock analysis,
     * saves the result to the Insights table, and returns details of the new insight
     * as an InsightDetailDto.
     *
     * @param resumeFile The uploaded resume file.
     * @param jobTitle The job title for the analysis.
     * @param jobDescription The job description text.
     * @param authenticatedUser The authenticated User entity.
     * @return An InsightDetailDto containing details of the created insight.
     */
    InsightDetailDto analyzeDocuments( // Return type changed to InsightDetailDto
                                       MultipartFile resumeFile,
                                       String jobTitle,
                                       String jobDescription,
                                       User authenticatedUser
    );

    /**
     * Retrieves a summary list of all insights for a given user, ordered by most recent.
     * @param user The user whose insights history is to be fetched.
     * @return A list of InsightSummaryDto objects.
     */
    List<InsightSummaryDto> getInsightsHistoryForUser(User user);

    /**
     * Retrieves the most recent insight (as a detailed DTO) for a given user.
     * @param user The user whose latest insight is to be fetched.
     * @return An Optional containing InsightDetailDto if an insight exists, otherwise empty.
     */
    Optional<InsightDetailDto> getLatestInsightForUser(User user);

    /**
     * Retrieves a specific insight by its ID, ensuring it belongs to the given user.
     * @param insightId The UUID of the insight to fetch.
     * @param user The user who should own the insight.
     * @return An Optional containing InsightDetailDto if found and owned by user, otherwise empty.
     */
    Optional<InsightDetailDto> getInsightByIdAndUser(UUID insightId, User user);

    /**
     * FOR INITIAL TESTING/DEBUGGING: Receives analysis request, logs inputs, and returns a mock acknowledgment.
     * Does not perform full analysis or save to the main Insights table.
     */
    AnalysisRequestAckDto processAnalysisRequestMock(
            MultipartFile resumeFile,
            String jobTitle,
            String jobDescription,
            User authenticatedUser
    );
}
