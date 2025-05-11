package com.jdmatchr.core.repository;

import com.jdmatchr.core.entity.Insights;
import com.jdmatchr.core.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link Insights} entity.
 */
@Repository
public interface InsightsRepository extends JpaRepository<Insights, UUID> {

    /**
     * Finds all insights associated with a specific user, ordered by creation date descending.
     * This will be useful for the "/history" page.
     *
     * @param user The user entity.
     * @return A list of insights for the given user, ordered by most recent first.
     */
    List<Insights> findByUserOrderByCreatedAtDesc(User user);

    // You can add other custom query methods later, e.g.,
    // List<Insights> findByUserAndJobTitleContainingIgnoreCase(User user, String jobTitleKeyword);
}
