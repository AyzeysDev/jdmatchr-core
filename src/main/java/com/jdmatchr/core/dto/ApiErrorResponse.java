package com.jdmatchr.core.dto; // Adjust to your package structure

import java.time.OffsetDateTime;
import java.util.List; // Optional: for multiple validation errors

// If using Lombok:
// import lombok.Getter;
// import lombok.Setter;
// @Getter
// @Setter
public class ApiErrorResponse {
    private OffsetDateTime timestamp;
    private int status;
    private String error; // e.g., "Unauthorized", "Bad Request", "Conflict"
    private String message; // User-friendly message or specific error detail
    private String path; // Optional: The path where the error occurred

    // Optional: For detailed validation errors
    // private List<String> details;

    // Constructor for general errors
    public ApiErrorResponse(int status, String error, String message) {
        this.timestamp = OffsetDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
    }

    // Constructor including path
    public ApiErrorResponse(int status, String error, String message, String path) {
        this(status, error, message);
        this.path = path;
    }

    // Getters and Setters (Required if not using Lombok)
    public OffsetDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
}
