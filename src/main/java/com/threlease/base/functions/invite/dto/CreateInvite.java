package com.threlease.base.functions.invite.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class CreateInvite {
    @NotEmpty
    private String id;
}
