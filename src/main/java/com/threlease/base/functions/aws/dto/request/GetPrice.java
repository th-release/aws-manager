package com.threlease.base.functions.aws.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.Getter;
import software.amazon.awssdk.services.ec2.model.InstanceType;

@Data
public class GetPrice {
    @NotEmpty
    InstanceType instanceType;
}
