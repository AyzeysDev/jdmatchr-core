package com.jdmatchr.core.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

// @Getter
// @Setter
// @NoArgsConstructor
// @AllArgsConstructor
@Entity
@Table(name = "accounts", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider_id", "provider_account_id"})
})
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID id;

    // Many Accounts can belong to one User
    // FetchType.LAZY is generally good for performance.
    // 'nullable = false' ensures an account must be linked to a user.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "provider_type", length = 50, nullable = false)
    private String providerType; // e.g., "credentials", "oauth"

    @Column(name = "provider_id", length = 255, nullable = false)
    private String providerId; // e.g., "credentials", "google"

    @Column(name = "provider_account_id", columnDefinition = "TEXT", nullable = false)
    private String providerAccountId; // User's ID from the provider, or email for credentials

    @Column(name = "password_hash", columnDefinition = "TEXT") // Only for 'credentials' provider
    private String passwordHash;

    @Column(name = "oauth_access_token", columnDefinition = "TEXT")
    private String oauthAccessToken;

    @Column(name = "oauth_refresh_token", columnDefinition = "TEXT")
    private String oauthRefreshToken;

    @Column(name = "oauth_token_expires_at")
    @Temporal(TemporalType.TIMESTAMP)
    private OffsetDateTime oauthTokenExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private OffsetDateTime updatedAt;


    public Account() {
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getProviderType() { return providerType; }
    public void setProviderType(String providerType) { this.providerType = providerType; }
    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }
    public String getProviderAccountId() { return providerAccountId; }
    public void setProviderAccountId(String providerAccountId) { this.providerAccountId = providerAccountId; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getOauthAccessToken() { return oauthAccessToken; }
    public void setOauthAccessToken(String oauthAccessToken) { this.oauthAccessToken = oauthAccessToken; }
    public String getOauthRefreshToken() { return oauthRefreshToken; }
    public void setOauthRefreshToken(String oauthRefreshToken) { this.oauthRefreshToken = oauthRefreshToken; }
    public OffsetDateTime getOauthTokenExpiresAt() { return oauthTokenExpiresAt; }
    public void setOauthTokenExpiresAt(OffsetDateTime oauthTokenExpiresAt) { this.oauthTokenExpiresAt = oauthTokenExpiresAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

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
}
