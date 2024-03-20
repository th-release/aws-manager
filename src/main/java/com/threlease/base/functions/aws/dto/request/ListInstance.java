package com.threlease.base.functions.aws.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class ListInstance {
    @NotEmpty
    private int take;

    @NotEmpty
    private int skip;
}
