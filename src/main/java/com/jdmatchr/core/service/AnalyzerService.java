package com.jdmatchr.core.service;

import com.jdmatchr.core.dto.AnalysisRequestAckDto; // For the mock confirmation if still needed elsewhere
import com.jdmatchr.core.dto.InsightDetailDto;
import com.jdmatchr.core.dto.InsightSummaryDto;
import com.jdmatchr.core.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map; // analyzeDocuments currently returns a Map
import java.util.Optional;
import java.util.UUID;

public interface AnalyzerService {

    /**
     * Processes the uploaded resume and job description, performs mock analysis,
     * saves the result to the Insights table, and returns details of the new insight.
     *
     * @param resumeFile The uploaded resume file.
     * @param jobTitle The job title for the analysis.
     * @param jobDescription The job description text.
     * @param authenticatedUser The authenticated User entity.
     * @return A Map containing details of the created insight, including "insightId".
     */
    Map<String, Object> analyzeDocuments(
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

    // The mock confirmation method, if you want to keep it for other testing purposes
    // If not needed, it can be removed from the interface and implementation.
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
