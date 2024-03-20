package com.threlease.base.functions.aws;

import com.threlease.base.utils.Failable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class NetworkService {
    @Value("${env.aws.subnet.tag}")
    private String tag;

    @Value("${env.aws.subnet.value}")
    private String value;

    public Failable<String, String> getSubnet() {
        Filter dsrFilter = Filter.builder()
                .name("tag:" + this.tag)
                .values(this.value)
                .build();

        DescribeSubnetsRequest request = DescribeSubnetsRequest.builder()
                .filters(dsrFilter)
                .build();

        if (!request.subnetIds().get(0).isEmpty() && !request.hasSubnetIds()) {
            return Failable.success(request.subnetIds().get(0));
        } else {
            return Failable.error("Subnet with \"awsmgr-managed: true\" tag not found.");
        }
    }

    public Failable<Address, String> attachEIP(
            Ec2Client ec2Client,
            String id
    ) {
        AllocateAddressRequest AlAd_request = AllocateAddressRequest.builder()
                .domain("vpc")
                .build();

        AllocateAddressResponse AlAd_response = ec2Client.allocateAddress(AlAd_request);
        if (AlAd_response.allocationId().isEmpty()) {
            return Failable.error("NOT FOUND ALLOCATION_ID");
        }

        AssociateAddressRequest attach_request = AssociateAddressRequest.builder()
                .instanceId(id)
                .allocationId(AlAd_response.allocationId())
                .build();

        AssociateAddressResponse attach_response = ec2Client.associateAddress(attach_request);

        if (attach_response.associationId().isEmpty()) {
            return Failable.error("NOT FOUND ASSOCIATION_ID");
        }

        DescribeAddressesRequest da_request = DescribeAddressesRequest.builder()
                .allocationIds(AlAd_response.allocationId())
                .build();

        DescribeAddressesResponse da_response = ec2Client.describeAddresses(da_request);

        if (da_response.addresses().isEmpty()) {
            return Failable.error("Internal error has been occurred during attach EIP.");
        }

        List<Address> addresses = da_response.addresses().stream().filter((v) -> Objects.equals(v.associationId(), attach_response.associationId())).toList();
        if (addresses.isEmpty()) {
            return Failable.error("NOT FOUND ATTACH ADDRESS");
        }

        return Failable.success(addresses.get(0));
    }

    public Failable<Boolean, String> detachEIP(
            Ec2Client ec2Client,
            String id
    ) {
        ArrayList<String> ids = new ArrayList<String>();

        ids.add(id);

        Filter filter = Filter.builder()
                .name("instance-id")
                .values(ids)
                .build();

        DescribeAddressesRequest da_request = DescribeAddressesRequest.builder()
                .filters(filter)
                .build();

        DescribeAddressesResponse da_response = ec2Client.describeAddresses(da_request);

        if (da_response.addresses().isEmpty()) {
            return Failable.error("NOT FOUND ADDRESSES");
        }

        DisassociateAddressRequest detach_request = DisassociateAddressRequest.builder()
                .associationId(da_response.addresses().get(0).associationId())
                .build();

        ec2Client.disassociateAddress(detach_request);

        ReleaseAddressRequest release_request = ReleaseAddressRequest.builder()
                .allocationId(da_request.allocationIds().get(0))
                .build();

        ec2Client.releaseAddress(release_request);

        return Failable.success(true);
    }
}
