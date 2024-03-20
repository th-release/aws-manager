package com.threlease.base.functions.aws.dto.returns;

import com.threlease.base.entites.InstanceEntity;
import lombok.Data;
import software.amazon.awssdk.services.ec2.model.InstanceStatus;

@Data
public class GetInstanceReturn {
    InstanceEntity instance;
    InstanceStatus status;

}
