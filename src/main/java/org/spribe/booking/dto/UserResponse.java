package org.spribe.booking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.spribe.booking.model.enumeration.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UserResponse {
    @JsonProperty("id")
    private UUID id;

    @JsonProperty("email")
    private String email;

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("role")
    private UserRole role;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
} 