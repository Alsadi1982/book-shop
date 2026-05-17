package com.otus.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class LoginResponseDTO {
    private boolean success;
    private String message;
    private UserResponseDTO user;
}
