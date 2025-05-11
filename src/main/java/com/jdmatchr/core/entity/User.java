package com.jdmatchr.core.entity;

import jakarta.persistence.*; // Make sure to use jakarta.persistence for Spring Boot 3+
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// Lombok annotations can be added later if you included Lombok dependency
// import lombok.Getter;
// import lombok.Setter;
// import lombok.NoArgsConstructor;
// import lombok.AllArgsConstructor;

@Entity
@Table(name = "users") // This will be the table name in PostgreSQL
// @Getter
// @Setter
// @NoArgsConstructor
// @AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO) // Or GenerationType.UUID if your DB supports it directly and you prefer
    @Column(columnDefinition = "UUID") // Standard way to specify UUID for PostgreSQL
    private UUID id;

    @Column(length = 255)
    private String name;

    @Column(length = 255, unique = true, nullable = false)
    private String email;

    @Column(name = "email_verified")
    private OffsetDateTime emailVerified;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private OffsetDateTime updatedAt;

    // Relationship: One User can have multiple Accounts (e.g., one for 'credentials', one for 'google')
    // 'mappedBy = "user"' indicates that the 'user' field in the Account entity owns the relationship.
    // CascadeType.ALL means operations (persist, remove, refresh, merge, detach) on User will cascade to associated Accounts.
    // FetchType.LAZY means accounts are not loaded from DB unless explicitly accessed.
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Account> accounts = new HashSet<>();

    // JPA requires a no-arg constructor
    public User() {
    }

    // Constructor for essential fields (example)
    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }


    // Getters and Setters (Manually or generate with IDE / Lombok)
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public OffsetDateTime getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(OffsetDateTime emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(Set<Account> accounts) {
        this.accounts = accounts;
    }

    // Helper methods for bi-directional relationship management (good practice)
    public void addAccount(Account account) {
        accounts.add(account);
        account.setUser(this);
    }

    public void removeAccount(Account account) {
        accounts.remove(account);
        account.setUser(null);
    }

    // Lifecycle Callbacks to set created_at and updated_at timestamps automatically
    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // toString, hashCode, equals (Optional, can be generated)
}
