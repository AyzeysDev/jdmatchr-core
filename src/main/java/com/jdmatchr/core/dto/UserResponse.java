// src/main/java/com/jdmatchr/core/dto/UserResponse.java
package com.jdmatchr.core.dto;

import java.util.UUID;

// If using Lombok:
// import lombok.Data;
// import lombok.NoArgsConstructor;
// import lombok.AllArgsConstructor;

// @Data
// @NoArgsConstructor
// @AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String name;
    private String email;
    // Add other fields you want to return to the client after registration/login

    // Constructor
    public UserResponse(UUID id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}