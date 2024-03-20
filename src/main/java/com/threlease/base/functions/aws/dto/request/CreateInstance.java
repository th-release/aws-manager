package com.threlease.base.functions.aws.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.Getter;
import software.amazon.awssdk.services.ec2.model.InstanceType;

import java.util.List;
import java.util.Optional;

@Data
public class CreateInstance {
    @NotEmpty
    private String category;

    @NotEmpty
    private String name;

    @NotEmpty
    private String description;

    @NotEmpty
    private String owner;

    @NotEmpty
    private String instanceType;

    @NotEmpty
    @Min(0)
    private int StorageSize;

    private Optional<String> amiId;

    @NotEmpty
    private String ports;

    @NotEmpty
    private String memo;
}
