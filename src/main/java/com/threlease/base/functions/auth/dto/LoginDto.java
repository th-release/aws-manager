package com.threlease.base.functions.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class LoginDto {
    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
