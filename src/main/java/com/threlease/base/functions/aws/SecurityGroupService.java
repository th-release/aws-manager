package com.threlease.base.functions.aws;

import com.threlease.base.utils.Failable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class SecurityGroupService {
    public Failable<String, String> createSecurityGroupId(
            Ec2Client ec2Client,
            String name,
            String vpcId,
            List<Integer> ports
    ) {
        CreateSecurityGroupRequest sg_request = CreateSecurityGroupRequest.builder()
                .groupName(name + "-sg")
                .vpcId(vpcId)
                .description("this security group managed by awsmgr")
                .build();

        try {
            CreateSecurityGroupResponse sg_response = ec2Client.createSecurityGroup(sg_request);

            for (int port: ports) {
                IpRange ipRange = IpRange.builder()
                        .cidrIp("0.0.0.0/0")
                        .build();

                IpPermission ipPermission = IpPermission.builder()
                        .fromPort(port)
                        .toPort(port)
                        .ipProtocol("tcp")
                        .ipRanges(ipRange)
                        .build();

                AuthorizeSecurityGroupIngressRequest asgi_request = AuthorizeSecurityGroupIngressRequest.builder()
                        .groupId(sg_response.groupId())
                        .ipPermissions(ipPermission)
                        .build();

                ec2Client.authorizeSecurityGroupIngress(asgi_request);
            }

            return Failable.success(sg_response.groupId());
        } catch (Ec2Exception e) {
            return Failable.error(e.getMessage());
        }
    }

    public Failable<Boolean, String> updateSecurityGroup(
            Ec2Client ec2Client,
            String name,
            List<Integer> afterPorts
    ) {
        Filter filter = Filter.builder()
                .name("group-name")
                .values(name + "-sg")
                .build();

        DescribeSecurityGroupsRequest sg_request = DescribeSecurityGroupsRequest.builder()
                .filters(filter)
                .build();

        try {
            DescribeSecurityGroupsResponse sg_response = ec2Client.describeSecurityGroups(sg_request);

            if (sg_response.securityGroups().isEmpty())
                return Failable.error("NOT FOUND SECURITY GROUPS");

            SecurityGroup securityGroup = sg_response.securityGroups().get(0);

            String group_id = securityGroup.groupId();
            List<IpPermission> beforeRules = securityGroup.ipPermissions().isEmpty()
                    ? new ArrayList<IpPermission>() : securityGroup.ipPermissions();

            List<Integer> beforePorts = beforeRules.stream().map((value) -> {
                return value.fromPort().toString().isEmpty() ? 0 : value.fromPort();
            }).toList();

            List<Integer> ports = new ArrayList<Integer>();
            ports.addAll(beforePorts);
            ports.addAll(afterPorts);

            for (int port : ports) {
                if (!beforePorts.contains(port) && !afterPorts.contains(port)) {
                    IpRange ipRange = IpRange.builder()
                            .cidrIp("0.0.0.0/0")
                            .build();

                    IpPermission ipPermission = IpPermission.builder()
                            .fromPort(port)
                            .toPort(port)
                            .ipProtocol("tcp")
                            .ipRanges(ipRange)
                            .build();

                    RevokeSecurityGroupIngressRequest rsgi_request = RevokeSecurityGroupIngressRequest.builder()
                            .groupId(group_id)
                            .ipPermissions(ipPermission)
                            .build();

                    ec2Client.revokeSecurityGroupIngress(rsgi_request);
                }
            }

            return Failable.success(true);
        } catch (Ec2Exception e) {
            return Failable.error(e.getMessage());
        }
    }

    public Failable<SecurityGroup, String> findOneByName(
            Ec2Client ec2Client,
            String name
    ) {
        Filter filter = Filter.builder()
                .name("group-name")
                .values(name + "-sg")
                .build();

        DescribeSecurityGroupsRequest sg_request = DescribeSecurityGroupsRequest.builder()
                .filters(filter)
                .build();

        try {
            DescribeSecurityGroupsResponse sg_response = ec2Client.describeSecurityGroups(sg_request);

            if (sg_response.securityGroups().isEmpty())
                return Failable.error("NOT FOUND SECURITY GROUPS");

            SecurityGroup securityGroup = sg_response.securityGroups().get(0);

            return Failable.success(securityGroup);
        } catch (Ec2Exception e) {
            return Failable.error(e.getMessage());
        }
    }

    public Failable<Boolean, String> deleteSecurityGroup(
            Ec2Client ec2Client,
            String name
    ) {
        Filter filter = Filter.builder()
                .name("group-name")
                .values(name + "-sg")
                .build();

        DescribeSecurityGroupsRequest sg_request = DescribeSecurityGroupsRequest.builder()
                .filters(filter)
                .build();

        try {
            DescribeSecurityGroupsResponse sg_response = ec2Client.describeSecurityGroups(sg_request);
            if (sg_response.securityGroups().isEmpty())
                return Failable.error("NOT FOUND SECURITY GROUPS");

            String groupId = sg_response.securityGroups().get(0).groupId();
            DeleteSecurityGroupRequest del_request = DeleteSecurityGroupRequest.builder()
                    .groupId(groupId)
                    .build();

            ec2Client.deleteSecurityGroup(del_request);
            return Failable.success(true);
        } catch (Ec2Exception e) {
            return Failable.error(e.getMessage());
        }
    }
}
