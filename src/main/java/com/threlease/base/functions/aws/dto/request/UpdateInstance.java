package com.threlease.base.functions.aws.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class UpdateInstance {
    @NotEmpty
    private String description;

    @NotEmpty
    private String owner;

    @NotEmpty
    private String instanceType;

    @NotEmpty
    @Min(0)
    private int StorageSize;

    @NotEmpty
    private String ports;

    @NotEmpty
    private String memo;
}