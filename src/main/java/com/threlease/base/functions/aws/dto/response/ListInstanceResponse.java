package com.threlease.base.functions.aws.dto.response;

import com.threlease.base.entites.InstanceEntity;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

@Data
@Builder
public class ListInstanceResponse {
    private Page<InstanceEntity> instances;
    private long pageCount;
}
