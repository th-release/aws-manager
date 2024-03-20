package com.threlease.base.functions.aws.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.Getter;

@Data
public class GetInstances {
    @NotEmpty
    @Min(0)
    private int take;

    @NotEmpty
    @Min(0)
    private int skip;
}
