package com.example.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class UserCreationRequest {

    @Size(min = 3, message = "Username must be at least 3 characters")
    @NotBlank(message = "Username is required")
    @Pattern(regexp = "^[^0-9]*$", message = "Username must be a string, not contain numbers")
    String username;

    @Size(min = 3, message = "Email must be at least 3 characters")
    @NotBlank(message = "Email is required")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Email must be a valid email address")
    String email;

    Integer birthYear;

    String gender;

    @Size(min = 3, message = "Password must be at least 3 characters")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$", message = "Password must contain at least one digit, one lowercase letter, one uppercase letter, and one special character")
    String password;
}