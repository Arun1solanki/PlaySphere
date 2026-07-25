package com.playsphere.playsphere_backend.repository;

import com.playsphere.playsphere_backend.entity.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;


@Data
public class RegisterRequest {


    @NotBlank(message="Name is required")
    private String name;


    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email")
    private String email;


    @NotBlank(message = "Password is required")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@#$%^&+=!]).{6,10}$",
        message = "Password must be 6-10 characters and contain at least one letter, one digit, and one special character"
    )
    String password;

    @NotNull(message = "Role is required")
    private Role role;

}
