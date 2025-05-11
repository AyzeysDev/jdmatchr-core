package com.jdmatchr.core.repository;

import com.jdmatchr.core.entity.Account; // Import your Account entity
import com.jdmatchr.core.entity.User;    // Import User if needed for query return types or parameters
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

/**
 * Spring Data JPA repository for the {@link Account} entity.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    /**
     * Finds an account by its provider ID (e.g., "google", "credentials")
     * and the user's unique ID within that provider (providerAccountId).
     * This is useful for the /ensure-oauth endpoint to check if an OAuth account already exists.
     *
     * @param providerId The ID of the authentication provider (e.g., "google").
     * @param providerAccountId The user's unique ID for that provider.
     * @return An {@link Optional} containing the found account, or empty if not found.
     */
    Optional<Account> findByProviderIdAndProviderAccountId(String providerId, String providerAccountId);

    /**
     * Finds all accounts associated with a specific user.
     * This might be useful if a user wants to see their linked accounts.
     *
     * @param user The user entity.
     * @return A list of accounts linked to the given user.
     */
    List<Account> findByUser(User user);

    /**
     * Finds an account for a specific user and a specific provider ID.
     * Useful for checking if a user already has a 'credentials' account, for example.
     *
     * @param user The user entity.
     * @param providerId The ID of the authentication provider (e.g., "credentials").
     * @return An {@link Optional} containing the found account, or empty if not found.
     */
    Optional<Account> findByUserAndProviderId(User user, String providerId);

    // You can add other custom query methods as your application evolves.
}
