package com.jdmatchr.core.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// If using Lombok:
// import lombok.Data;
// import lombok.NoArgsConstructor;
// import lombok.AllArgsConstructor;

// @Data
// @NoArgsConstructor
// @AllArgsConstructor
public class EnsureOAuthRequest {

    @NotBlank(message = "Provider ID cannot be blank")
    private String providerId; // e.g., "google"

    @NotBlank(message = "Provider Account ID cannot be blank")
    private String providerAccountId; // User's unique ID from the OAuth provider (e.g., Google sub)

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Please provide a valid email address")
    private String email;

    private String name; // Name can be optional or have different handling

    private String imageUrl; // URL to the profile picture

    // Getters and Setters
    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }
    public String getProviderAccountId() { return providerAccountId; }
    public void setProviderAccountId(String providerAccountId) { this.providerAccountId = providerAccountId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
