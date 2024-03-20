package com.threlease.base.functions.aws;

import com.amazonaws.SdkClientException;
import com.threlease.base.entites.InstanceEntity;
import com.threlease.base.entites.KeypairEntity;
import com.threlease.base.repositories.InstanceRepository;
import com.threlease.base.utils.EnumStringComparison;
import com.threlease.base.utils.Failable;
import com.threlease.base.utils.Hash;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class Ec2InstanceService {
    private final NetworkService networkService;

    public Ec2InstanceService(NetworkService networkService) {
        this.networkService = networkService;
    }

    public Failable<Instance, String> createEC2Instance(
            Ec2Client ec2Client,
            String name,
            String i_type,
            Optional<String> amiId,
            Optional<Integer> StorageSize,
            KeypairEntity keypair,
            String SecurityGroupId
    ) {
        if (!EnumStringComparison.compareEnumString(i_type, InstanceType.class) ||
                (!i_type.equals("t3a.nano")
                        && !i_type.equals("t3a.small")
                        && !i_type.equals("t3a.micro")
                        && !i_type.equals("t2.nano"))
        ) {
            return Failable.error("Invalid Instance Type");
        }

        EbsBlockDevice ebd = EbsBlockDevice.builder()
                .volumeSize(StorageSize.orElse(8))
                .volumeType("gp2")
                .build();

        BlockDeviceMapping bdm = BlockDeviceMapping.builder()
                .deviceName("/dev/sda1")
                .ebs(ebd)
                .build();

        Failable<String, String> dsr = networkService.getSubnet();

        if (!dsr.isError()) {
            Tag specification_tag = Tag.builder()
                    .key("Name")
                    .value(name)
                    .build();

            TagSpecification tagSpecification = TagSpecification.builder()
                    .resourceType("instance")
                    .tags(specification_tag)
                    .build();

            RunInstancesRequest runRequest = RunInstancesRequest.builder()
                    .maxCount(1)
                    .minCount(1)
                    .instanceType(i_type)
                    .imageId(amiId.orElse("ami-09a7535106fbd42d5"))
                    .subnetId(dsr.getValue())
                    .tagSpecifications(tagSpecification)
                    .blockDeviceMappings(bdm)
                    .securityGroupIds(SecurityGroupId)
                    .keyName(keypair.getName())
                    .userData(new Hash().base64_encode("#/bin/bash\\ngrowpart /dev/sda 1\\nresize2fs /dev/sda1"))
                    .build();

            try {
                RunInstancesResponse response = ec2Client.runInstances(runRequest);
                if (response.instances().isEmpty()) {
                    return Failable.error("NOT FOUND INSTANCES");
                }

                Instance instance_data = response.instances().get(0);
                return Failable.success(instance_data);
            } catch (Ec2Exception e) {
                return Failable.error(e.toString());
            }
        } else {
            return Failable.error(dsr.getError());
        }
    }

    public Failable<Boolean, String> deleteEC2Instance(
            Ec2Client ec2Client,
            String id
    ) {
        TerminateInstancesRequest request = TerminateInstancesRequest.builder()
                .instanceIds(id)
                .build();

        try {
            ec2Client.terminateInstances(request);

            return Failable.success(true);
        } catch (Ec2Exception e) {
            return Failable.error(e.getMessage());
        }
    }

    public Failable<Boolean, String> updateEC2InstanceType(
            Ec2Client ec2Client,
            String id,
            InstanceType type
    ) {
        AttributeValue instanceType = AttributeValue.builder()
                .value(type.name())
                .build();

        ModifyInstanceAttributeRequest request = ModifyInstanceAttributeRequest.builder()
                .instanceId(id)
                .instanceType(instanceType)
                .build();

        try {
            ec2Client.modifyInstanceAttribute(request);

            return Failable.success(true);
        } catch (Ec2Exception e) {
            return Failable.error(e.getMessage());
        }
    }

    public Failable<Boolean, String> startEC2Instance(
            Ec2Client ec2Client,
            String id
    ) {
        StartInstancesRequest request = StartInstancesRequest.builder()
                .instanceIds(id)
                .build();

        try {
            ec2Client.startInstances(request);
            return Failable.success(true);
        } catch (Ec2Exception e) {
            return Failable.error(e.getMessage());
        }
    }

    public Failable<Boolean, String> stopEC2Instance(
            Ec2Client ec2Client,
            String id
    ) {
        StopInstancesRequest request = StopInstancesRequest.builder()
                .instanceIds(id)
                .build();

        try {
            ec2Client.stopInstances(request);

            return Failable.success(true);
        } catch (Ec2Exception e) {
            return Failable.error(e.getMessage());
        }
    }

    public Failable<Boolean, String> forceStopEC2Instance(
            Ec2Client ec2Client,
            String id
    ) {
        StopInstancesRequest request = StopInstancesRequest.builder()
                .instanceIds(id)
                .force(true)
                .build();

        try {
            ec2Client.stopInstances(request);

            return Failable.success(true);
        } catch (Ec2Exception e) {
            return Failable.error(e.getMessage());
        }
    }

    public List<InstanceStatus> getEC2InstanceStatus (
            Ec2Client ec2Client,
            List<String> id
    ) {
        DescribeInstanceStatusRequest request = DescribeInstanceStatusRequest.builder()
                .instanceIds(id)
                .includeAllInstances(true)
                .build();

        DescribeInstanceStatusResponse response = ec2Client.describeInstanceStatus(request);

        return response.instanceStatuses();
    }

    public Failable<List<Instance>, String> listEC2Instances(
            Ec2Client ec2Client,
            List<String> id
    ) {
        DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                .instanceIds(id)
                .build();

        DescribeInstancesResponse response = ec2Client.describeInstances(request);
        if (response.reservations().isEmpty()) {
            return Failable.error("NOT FOUND RESERVATIONS");
        }

        return Failable.success(response.reservations().get(0).instances());
    }
}
