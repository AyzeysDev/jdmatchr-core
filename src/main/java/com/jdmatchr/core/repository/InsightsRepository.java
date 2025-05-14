package com.jdmatchr.core.repository;

import com.jdmatchr.core.entity.Insights;
import com.jdmatchr.core.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InsightsRepository extends JpaRepository<Insights, UUID> {

    /**
     * Finds all insights for a given user, ordered by creation date in descending order (newest first).
     * This will be used for the /history page.
     * If no insights exist, it will return an empty list.
     * @param user The user whose insights are to be fetched.
     * @return A list of insights.
     */
    List<Insights> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Finds the most recent insight for a given user.
     * This can be used by the /insights (generic) endpoint to get the latest.
     * Spring Data JPA's "findTop" or "findFirst" keywords can be used for this.
     * @param user The user whose latest insight is to be fetched.
     * @return An Optional containing the latest insight if found, otherwise empty.
     */
    Optional<Insights> findTopByUserOrderByCreatedAtDesc(User user);

    /**
     * Finds a specific insight by its ID and ensures it belongs to the given user.
     * This will be used for the /insights/:id page.
     * @param id The ID of the insight.
     * @param user The user who should own the insight.
     * @return An Optional containing the insight if found and owned by the user, otherwise empty.
     */
    Optional<Insights> findByIdAndUser(UUID id, User user);
}
