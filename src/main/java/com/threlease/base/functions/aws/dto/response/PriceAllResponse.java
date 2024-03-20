package com.threlease.base.functions.aws.dto.response;

import lombok.Data;

@Data
public class PriceAllResponse {
    private double pricePerHour;
    private double storageSize;
}
