package com.backend.clinic.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class AuthDTOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "Username cannot be empty")
        private String username;

        @NotBlank(message = "Password cannot be empty")
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RegisterRequest {
        @NotBlank(message = "Username cannot be empty")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        private String username;

        @NotBlank(message = "Email cannot be empty")
        @Email(message = "Email is invalid")
        @Size(max = 100)
        private String email;

        @NotBlank(message = "Password cannot be empty")
        @Size(min = 6, max = 255, message = "Password must be at least 6 characters")
        private String password;

        @NotBlank(message = "Full name cannot be empty")
        @Size(max = 100)
        private String fullName;

        @Size(max = 15)
        private String phone;

        private String role;
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class JwtResponse {
        private String token;
        @Builder.Default
        private String type = "Bearer";
        private Long userId;
        private String username;
        private String email;
        private List<String> roles;
    }
}
