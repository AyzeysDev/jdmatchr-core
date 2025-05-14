package com.jdmatchr.core.service;

import com.jdmatchr.core.dto.InsightDetailDto; // Import DTO
import com.jdmatchr.core.dto.InsightSummaryDto; // Import DTO
import com.jdmatchr.core.entity.User; // Import User
import org.springframework.web.multipart.MultipartFile;

import java.util.List; // Import List
import java.util.Map;
import java.util.Optional; // Import Optional
import java.util.UUID;    // Import UUID


public interface AnalyzerService {
    // Existing method (or your version of it)
    Map<String, Object> analyzeDocuments(
            MultipartFile resumeFile,
            String jobTitle,
            String jobDescription,
            User authenticatedUser // Pass the User entity
    );

    // New methods for insights and history
    List<InsightSummaryDto> getInsightsHistoryForUser(User user);

    Optional<InsightDetailDto> getLatestInsightForUser(User user); // Returns full detail DTO or just ID

    Optional<InsightDetailDto> getInsightByIdAndUser(UUID insightId, User user);
}
