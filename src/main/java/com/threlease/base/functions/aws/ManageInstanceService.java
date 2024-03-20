package com.threlease.base.functions.aws;

import com.amazonaws.services.pricing.AWSPricing;
import com.amazonaws.services.pricing.AWSPricingClient;
import com.amazonaws.services.pricing.AWSPricingClientBuilder;
import com.threlease.base.entites.InstanceEntity;
import com.threlease.base.entites.KeypairEntity;
import com.threlease.base.functions.aws.dto.request.UpdateInstance;
import com.threlease.base.functions.aws.dto.returns.GetInstanceReturn;
import com.threlease.base.repositories.InstanceRepository;
import com.threlease.base.utils.EnumStringComparison;
import com.threlease.base.utils.Failable;
import com.threlease.base.utils.responses.BasicResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ManageInstanceService {
    private final Ec2InstanceService ec2InstanceService;
    private final InstanceRepository instanceRepository;
    private final StorageService storageService;
    private final PriceService priceService;
    private final NetworkService networkService;
    private final KeypairService keypairService;
    private final SecurityGroupService securityGroupService;
    private final UtilsService utilsService;

    public ManageInstanceService(
            Ec2InstanceService ec2InstanceService, InstanceRepository instanceRepository,
            StorageService storageService, PriceService priceService,
            NetworkService subnetService,
            KeypairService keypairService,
            SecurityGroupService securityGroupService, UtilsService utilsService) {
        this.ec2InstanceService = ec2InstanceService;
        this.instanceRepository = instanceRepository;
        this.storageService = storageService;
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

    public void instanceSave(InstanceEntity instance) {
        instanceRepository.save(instance);
    }

    public void instanceRemove(InstanceEntity instance) {
        instanceRepository.delete(instance);
    }
    public List<InstanceEntity> findAll() { return instanceRepository.findAll(); }
    public Optional<InstanceEntity> findOneByUuid(String uuid) {
        return instanceRepository.findOneByUUID(uuid);
    }

    public Optional<InstanceEntity> findOneByName(String name) {
        return instanceRepository.findOneByName(name);
    }

    public Failable<GetInstanceReturn, String> getInstance(
            String id
    ) {
        Ec2Client ec2Client = getEc2Client();
        Optional<InstanceEntity> instance = findOneByUuid(id);
        List<InstanceStatus> status = ec2InstanceService.getEC2InstanceStatus(ec2Client, List.of(id));

        if (status.isEmpty() || instance.isEmpty()) {
            return Failable.error("NOT FOUND INSTANCE");
        } else {
            GetInstanceReturn value = new GetInstanceReturn();

            value.setInstance(instance.get());
            value.setStatus(status.get(0));

            return Failable.success(value);
        }
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
        instance.setKeypair(keypair.getValue());

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

        utilsService.waitForState(ec2Client, !instance.getUuid().isEmpty() ? instance.getUuid() : "", InstanceStateName.RUNNING);

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

    public Failable<Boolean, String> deleteInstsance(
            Ec2Client ec2Client,
            InstanceEntity instance
    ) {
        Failable<Boolean, String> detachEIP = networkService.detachEIP(ec2Client, instance.getUuid());
        if (detachEIP.isError()) {
            return Failable.error(detachEIP.getError());
        }

        Failable<Boolean, String> deleteKeypair = keypairService.deleteKeypair(ec2Client, instance.getName(), "name");
        if (deleteKeypair.isError()) {
            return Failable.error(deleteKeypair.getError());
        }

        Failable<Boolean, String> deleteInstance = ec2InstanceService.deleteEC2Instance(ec2Client, instance.getUuid());
        if (deleteInstance.isError()) {
            return Failable.error(deleteInstance.getError());
        }

        utilsService.waitForState(ec2Client, instance.getUuid(), InstanceStateName.TERMINATED);

        Failable<Boolean, String> deleteSecurityGroup = securityGroupService.deleteSecurityGroup(ec2Client, instance.getName());
        if (deleteSecurityGroup.isError()) {
            return Failable.error(deleteSecurityGroup.getError());
        }

        instanceRemove(instance);
        return Failable.success(true);
    }

    public Failable<Boolean, String> updateInstance(
            Ec2Client ec2Client,
            InstanceEntity instance,
            UpdateInstance update
    ) {
        if (!Objects.equals(instance.getPorts(), update.getPorts())) {
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

            Failable<Boolean, String> updateSecurityGroup = securityGroupService.updateSecurityGroup(
                    ec2Client,
                    instance.getName(),
                    ports
            );

            instance.setPorts(update.getPorts());
        }
        double pricePerHour = 0.0;
        if (instance.getStorageSize() != update.getStorageSize() || !Objects.equals(instance.getType(), update.getInstanceType())) {
            Failable<Boolean, String> stopEc2 = ec2InstanceService.stopEC2Instance(
                    ec2Client,
                    instance.getUuid()
            );
            if (stopEc2.isError()) {
                return Failable.error(stopEc2.getError());
            }

            utilsService.waitForState(
                    ec2Client,
                    instance.getUuid(),
                    InstanceStateName.STOPPED
            );

            if (instance.getStorageSize() != update.getStorageSize()) {
                Failable<Instance, String> ec2 = ec2InstanceService.getEc2Instance(
                        ec2Client,
                        instance.getUuid()
                );

                if (ec2.isError()) {
                    return Failable.error(ec2.getError());
                }

                Instance ec2Instance = ec2.getValue();
                if (ec2Instance.blockDeviceMappings().isEmpty()) {
                    return Failable.error("NOT FOUND BLOCK_DEVIDE_MAPPINGS");
                }

                String volumeId = ec2Instance.blockDeviceMappings().get(0).ebs().volumeId();
                Failable<Boolean, String> updateRootStorage = storageService.updateRootStorage(
                        ec2Client,
                        volumeId,
                        update.getStorageSize()
                );

                if (updateRootStorage.isError()) {
                    return Failable.error(updateRootStorage.getError());
                }

                instance.setStorageSize(update.getStorageSize());
            }

            if (!Objects.equals(update.getInstanceType(), instance.getType())) {
                if (!EnumStringComparison.compareEnumString(instance.getType(), InstanceType.class) ||
                        (!instance.getType().equals("t3a.nano")
                                && !instance.getType().equals("t3a.small")
                                && !instance.getType().equals("t3a.micro")
                                && !instance.getType().equals("t2.nano"))
                ) {
                    return Failable.error("Invalid instance type");
                }

                AWSPricingClientBuilder pricingClientBuilder = AWSPricingClient.builder();

                pricingClientBuilder.setRegion("ap-south-1");
                AWSPricing pricingClient = pricingClientBuilder.build();

                Failable<Double, String> price_request = priceService.getTypePricePerHour(
                        pricingClient,
                        update.getInstanceType()
                );
                if (price_request.isError()){
                    return Failable.error(price_request.getError());
                }

                pricePerHour = price_request.getValue();

                Failable<Boolean, String> updateType = ec2InstanceService.updateEC2InstanceType(
                        ec2Client,
                        instance.getUuid(),
                        update.getInstanceType()
                );
                if (updateType.isError()) {
                    return Failable.error(updateType.getError());
                }

                instance.setType(update.getInstanceType());
            }

            Failable<Boolean, String> startRequest= ec2InstanceService.startEC2Instance(ec2Client, instance.getUuid());
            if (startRequest.isError()) {
                return Failable.error(startRequest.getError());
            }
            utilsService.waitForState(
                    ec2Client,
                    instance.getUuid(),
                    InstanceStateName.RUNNING
            );
        }

        instance.setMemo(update.getMemo());
        instance.setOwner(update.getOwner());
        instance.setDescription(update.getDescription());
        instance.setPricePerHour(pricePerHour);

        instanceSave(instance);
        return Failable.success(true);
    }

    public Failable<Boolean, String> restartInstance(
            Ec2Client ec2Client,
            InstanceEntity instance
    ) {
        List<InstanceStatus> status = ec2InstanceService.getEC2InstanceStatus(ec2Client, List.of(instance.getUuid()));

        if (status.isEmpty()) {
            return Failable.error("NOT FOUND INSTANCE STATUS");
        }

        if (status.get(0).instanceState().name() == InstanceStateName.STOPPED) {
            ec2InstanceService.startEC2Instance(ec2Client, instance.getUuid());

            return Failable.success(true);
        }

        ec2InstanceService.forceStopEC2Instance(ec2Client, instance.getUuid());
        utilsService.waitForState(ec2Client, instance.getUuid(), InstanceStateName.STOPPED);
        ec2InstanceService.startEC2Instance(ec2Client, instance.getUuid());

        return Failable.success(true);
    }

    public Failable<Boolean, String> resetInstance(
            Ec2Client ec2Client,
            InstanceEntity instance
    ) {
        Failable<Boolean, String> resetRequest = storageService.resetRootStorage(ec2Client, instance.getUuid());

        if (resetRequest.isError()) {
            return Failable.error(resetRequest.getError());
        }

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
