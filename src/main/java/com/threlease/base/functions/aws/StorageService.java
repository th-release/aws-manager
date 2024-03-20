package com.threlease.base.functions.aws;

import com.threlease.base.utils.Failable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

@Service
public class StorageService {
    public Failable<Boolean, String> resetRootStorage(
            Ec2Client ec2Client,
            String id
    ) {
        CreateReplaceRootVolumeTaskRequest request = CreateReplaceRootVolumeTaskRequest.builder()
                .instanceId(id)
                .deleteReplacedRootVolume(true)
                .build();

        try {
            ec2Client.createReplaceRootVolumeTask(request);

            return Failable.success(true);
        } catch (AwsServiceException | SdkClientException e) {
            return Failable.error(e.getMessage());
        }
    }

    public Failable<Boolean, String> updateRootStorage(
            Ec2Client ec2Client,
            String id,
            int size
    ) {
        ModifyVolumeRequest request = ModifyVolumeRequest.builder()
                .volumeId(id)
                .size(size)
                .build();

        try {
            ec2Client.modifyVolume(request);

            return Failable.success(true);
        } catch(Ec2Exception e) {
            return Failable.error(e.getMessage());
        }
    }
}
