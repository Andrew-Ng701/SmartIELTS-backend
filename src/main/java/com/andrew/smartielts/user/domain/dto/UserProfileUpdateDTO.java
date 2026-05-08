package com.andrew.smartielts.user.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserProfileUpdateDTO {

    @NotBlank(message = "Email cannot be empty")
    private String email;
}
