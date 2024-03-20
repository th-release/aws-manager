package com.threlease.base.functions.aws;

import com.amazonaws.services.pricing.AWSPricing;
import com.amazonaws.services.pricing.AWSPricingClient;
import com.amazonaws.services.pricing.AWSPricingClientBuilder;
import com.threlease.base.entites.InstanceEntity;
import com.threlease.base.entites.KeypairEntity;
import com.threlease.base.repositories.InstanceRepository;
import com.threlease.base.utils.EnumStringComparison;
import com.threlease.base.utils.Failable;
import com.threlease.base.utils.responses.BasicResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Address;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceType;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class ManageInstanceService {
    private final Ec2InstanceService ec2InstanceService;
    private final InstanceRepository instanceRepository;
    private final PriceService priceService;
    private final NetworkService networkService;
    private final KeypairService keypairService;
    private final SecurityGroupService securityGroupService;
    private final UtilsService utilsService;

    public ManageInstanceService(
            Ec2InstanceService ec2InstanceService, InstanceRepository instanceRepository,
            PriceService priceService,
            NetworkService subnetService,
            KeypairService keypairService,
            SecurityGroupService securityGroupService, UtilsService utilsService) {
        this.ec2InstanceService = ec2InstanceService;
        this.instanceRepository = instanceRepository;
        this.priceService = priceService;
        this.networkService = subnetService;
        this.keypairService = keypairService;
        this.securityGroupService = securityGroupService;
        this.utilsService = utilsService;
    }

    public Ec2Client getEc2Client() {
        return Ec2Client.builder()
                .region(Region.AP_NORTHEAST_2)
                .build();
    }

    public void InstanceSave(InstanceEntity instance) {
        instanceRepository.save(instance);
    }

    public void InstanceRemove(InstanceEntity instance) {
        instanceRepository.delete(instance);
    }
    public List<InstanceEntity> findAll() { return instanceRepository.findAll(); }
    public Optional<InstanceEntity> findOneByUuid(String uuid) {
        return instanceRepository.findOneByUUID(uuid);
    }

    public Optional<InstanceEntity> findOneByName(String name) {
        return instanceRepository.findOneByName(name);
    }

    public Failable<Boolean, String> createInstance(
            Ec2Client ec2Client,
            InstanceEntity instance
    ) {
        String instance_name = instance.getName();

        if (!EnumStringComparison.compareEnumString(instance.getType(), InstanceType.class) ||
                (!instance.getType().equals("t3a.nano")
                        && !instance.getType().equals("t3a.small")
                        && !instance.getType().equals("t3a.micro")
                        && !instance.getType().equals("t2.nano"))
        ) {
            return Failable.error("Invalid instance type");
        }

        Optional<InstanceEntity> isAlready = instanceRepository.findOneByName(instance_name);

        if (isAlready.isPresent()) {
            return Failable.error("Instance name \"" + instance_name + "\" already exists.");
        }

        AWSPricingClientBuilder pricingClientBuilder = AWSPricingClient.builder();

        pricingClientBuilder.setRegion("ap-south-1");
        AWSPricing pricingClient = pricingClientBuilder.build();

        Failable<Double, String> price = priceService.getTypePricePerHour(pricingClient, instance.getType());
        if (price.isError()) {
            return Failable.error(price.getError());
        }
        instance.setPricePerHour(price.getValue());

        Failable<String, String> subnet = networkService.getSubnet();
        if(subnet.isError()) {
            BasicResponse response = BasicResponse.builder()
                    .success(false)
                    .message(Optional.of(subnet.getError()))
                    .data(Optional.empty())
                    .build();

            return Failable.error(subnet.getError());
        }

        Failable<KeypairEntity, String> keypair = this.keypairService.createKeypair(ec2Client, instance_name);
        if (keypair.isError() || keypair.getValue() == null) {
            return Failable.error(!keypair.getError().isEmpty() ?
                    keypair.getError() :
                    "Internal error has been occurred during create keypair."
            );
        }
        instance.setKeypairId(keypair.getValue());

        List<Integer> ports = Arrays.stream(instance.getPorts().split(","))
                .map(v -> {
                    try {
                        return Math.abs(Integer.parseInt(v.trim()));
                    } catch (NumberFormatException e) {
                        return null; // 유효하지 않은 숫자는 null로 처리
                    }
                })
                .filter(Objects::nonNull) // null이 아닌 값만 필터링
                .toList();

        Failable<String, String> securityGroup = securityGroupService.createSecurityGroupId(
                ec2Client,
                instance.getName(),
                subnet.getValue(),
                ports
        );

        if (securityGroup.isError()) {
            return Failable.error(
                    securityGroup.getError().isEmpty() ?
                            securityGroup.getError() : "Internal error has been occurred during create securityGroup.");
        }

        Failable<Instance, String> ec2Instance = ec2InstanceService.createEC2Instance(
                ec2Client,
                instance_name,
                instance.getType(),
                Optional.empty(),
                Optional.of(instance.getStorageSize()),
                keypair.getValue(),
                securityGroup.getValue()
        );

        if (ec2Instance.isError()) {
            BasicResponse response = BasicResponse.builder()
                    .success(false)
                    .message(Optional.of(ec2Instance.getError()))
                    .data(Optional.empty())
                    .build();

            return Failable.error(ec2Instance.getError());
        }

        utilsService.waitForState(ec2Client, !instance.getUuid().isEmpty() ? instance.getUuid() : "", "running");

        Failable<Address, String> address = networkService.attachEIP(ec2Client,
                !instance.getUuid().isEmpty() ?
                        instance.getUuid() : ""
        );

        if (address.isError()) {
            return Failable.error(address.getError());
        }

        instance.setPublicIP(address.getValue().publicIp());

        instanceRepository.save(instance);

        return Failable.success(true);
    }

    public Page<InstanceEntity> listInstances(Pageable pageable) {
        return instanceRepository.findByPagination(pageable);
    }

    public Page<InstanceEntity> searchInstances(Pageable pageable, String query) {
        return instanceRepository.findBySearchPagination(pageable, query);
    }

    public long countInstancePages(long take) {
        return (long) Math.floor((double) instanceRepository.count() / take);
    }

    public long countSearchInstancePages(long take, String query) {
        return (long) Math.floor((double) instanceRepository.countBySearch(query) / take);
    }
}
