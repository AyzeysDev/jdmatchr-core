package com.jdmatchr.core.repository;

import com.jdmatchr.core.entity.User; // Import your User entity
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link User} entity.
 *
 * JpaRepository provides CRUD (Create, Read, Update, Delete) operations
 * and other common data access functionalities out of the box.
 * We specify the entity type (User) and the type of its primary key (UUID).
 */
@Repository // Marks this interface as a Spring Data repository, making it eligible for component scanning
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their email address.
     * Spring Data JPA automatically implements this method based on its name.
     * "findByEmail" tells Spring Data JPA to generate a query that selects a User
     * where the 'email' attribute matches the provided email parameter.
     *
     * @param email The email address to search for.
     * @return An {@link Optional} containing the found user, or an empty Optional if no user is found.
     * Using Optional helps to avoid null pointer exceptions and clearly indicates that a user might not exist.
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user exists with the given email address.
     * Spring Data JPA automatically implements this method.
     * This can be more efficient than fetching the whole User object if you only need to check existence.
     *
     * @param email The email address to check.
     * @return true if a user with the given email exists, false otherwise.
     */
    boolean existsByEmail(String email);

    // You can add other custom query methods here as needed, for example:
    // List<User> findByNameContainingIgnoreCase(String name);
    // Optional<User> findByIdAndIsActive(UUID id, boolean isActive);
}
