package com.threlease.base.functions.aws;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.InstanceStatus;

import java.util.List;

@Service
public class UtilsService {
    private final Ec2InstanceService ec2InstanceService;

    public UtilsService(Ec2InstanceService ec2InstanceService) {
        this.ec2InstanceService = ec2InstanceService;
    }

    public void waitForState(
            Ec2Client ec2Client,
            String id,
            InstanceStateName state
    ) {
        for (;;) {
            delayInSeconds(500);

            List<InstanceStatus> status = ec2InstanceService.getEC2InstanceStatus(ec2Client, List.of(id));

            if (status.get(0).instanceState().name() == state) {
                break;
            }

        }
    }

    private void delayInSeconds(long time) {
        try {
            Thread.sleep(time);
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
