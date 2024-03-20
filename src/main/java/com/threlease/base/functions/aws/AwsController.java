package com.threlease.base.functions.aws;

import com.amazonaws.services.pricing.AWSPricing;
import com.amazonaws.services.pricing.AWSPricingClient;
import com.amazonaws.services.pricing.AWSPricingClientBuilder;
import com.threlease.base.entites.InstanceEntity;
import com.threlease.base.functions.aws.dto.request.CreateInstance;
import com.threlease.base.functions.aws.dto.request.ListInstance;
import com.threlease.base.functions.aws.dto.request.SearchInstance;
import com.threlease.base.functions.aws.dto.response.ListInstanceResponse;
import com.threlease.base.functions.aws.dto.response.PriceAllResponse;
import com.threlease.base.functions.aws.dto.returns.GetInstanceReturn;
import com.threlease.base.functions.notice.NoticeResponse;
import com.threlease.base.functions.notice.WebSocketHandler;
import com.threlease.base.utils.EnumStringComparison;
import com.threlease.base.utils.Failable;
import com.threlease.base.utils.responses.BasicResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.InstanceType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/aws")
public class AwsController {
    private final Ec2InstanceService ec2InstanceService;
    private final ManageInstanceService manageInstanceService;
    private final PriceService priceService;
    private final KeypairService keypairService;

    public AwsController(Ec2InstanceService ec2InstanceService, ManageInstanceService manageInstanceService, PriceService priceService, KeypairService keypairService) {
        this.ec2InstanceService = ec2InstanceService;
        this.manageInstanceService = manageInstanceService;
        this.priceService = priceService;
        this.keypairService = keypairService;
    }

    @GetMapping("/prices/all")
    private ResponseEntity<?> getPriceAll() {
        List<InstanceEntity> instances = manageInstanceService.findAll();

        PriceAllResponse priceAllResponse = new PriceAllResponse();

        AtomicReference<Double> all_getPricePerHour = new AtomicReference<>(0.0);
        AtomicReference<Integer> all_Storage = new AtomicReference<>(0);
        instances.forEach((v) -> {
            all_getPricePerHour.set(all_getPricePerHour.get() + v.getPricePerHour());
            all_Storage.set(all_Storage.get() + v.getStorageSize());
        });

        priceAllResponse.setPricePerHour(all_getPricePerHour.get());
        priceAllResponse.setStorageSize(all_Storage.get());

        BasicResponse response = BasicResponse.builder()
                .success(true)
                .message(Optional.empty())
                .data(Optional.of(priceAllResponse))
                .build();

        return ResponseEntity.status(200).body(response);
    }

    @GetMapping("/prices/{instanceType}")
    private ResponseEntity<?> getPriceByInstanceType(
            @PathVariable("instanceType") String instanceType
    ) {
        AWSPricingClientBuilder pricingClientBuilder = AWSPricingClient.builder();

        pricingClientBuilder.setRegion("ap-south-1");
        AWSPricing pricingClient = pricingClientBuilder.build();

        Failable<Double, String> price = priceService.getTypePricePerHour(pricingClient, instanceType);

        if (price.isError()) {
            BasicResponse response = BasicResponse.builder()
                    .success(false)
                    .message(Optional.of(price.getError()))
                    .data(Optional.empty())
                    .build();

            return ResponseEntity.status(403).body(response);
        } else {
            BasicResponse response = BasicResponse.builder()
                    .success(true)
                    .message(Optional.empty())
                    .data(Optional.of(price.getValue() < 0.0 ? 0.0117 : price.getValue()))
                    .build();

            return ResponseEntity.status(200).body(response);
        }
    }

    @GetMapping("/instance")
    public ResponseEntity<?> listInstance(
            @RequestParam @Valid ListInstance dto
    ) {
        Page<InstanceEntity> instances = manageInstanceService.listInstances(PageRequest.of(dto.getTake(), dto.getSkip()));
        long pageCount = manageInstanceService.countInstancePages(dto.getTake());
        ListInstanceResponse data_response = ListInstanceResponse.builder()
                .instances(instances)
                .pageCount(pageCount)
                .build();

        BasicResponse response = BasicResponse.builder()
                .success(true)
                .message(Optional.empty())
                .data(Optional.of(data_response))
                .build();

        return ResponseEntity.status(200).body(response);
    }

    @GetMapping("/instance/{id}")
    private ResponseEntity<?> getInstance(
            @PathVariable("id") String id
    ) {
        Failable<GetInstanceReturn, String> instance = this.manageInstanceService.getInstance(id);

        if (instance.isError()) {
            BasicResponse response = BasicResponse.builder()
                    .success(false)
                    .message(Optional.of(instance.getError()))
                    .data(Optional.empty())
                    .build();

            return ResponseEntity.status(404).body(response);
        }

        BasicResponse response = BasicResponse.builder()
                .success(true)
                .message(Optional.empty())
                .data(Optional.of(instance.getValue()))
                .build();

        return ResponseEntity.status(200).body(response);
    }

    @GetMapping("/instance/search")
    public ResponseEntity<?> searchInstance(
            @RequestParam @Valid SearchInstance dto
    ) {
        Page<InstanceEntity> instances = manageInstanceService.searchInstances(PageRequest.of(dto.getTake(), dto.getSkip()), dto.getQuery());
        long pageCount = manageInstanceService.countSearchInstancePages(dto.getTake(), dto.getQuery());
        ListInstanceResponse data_response = ListInstanceResponse.builder()
                .instances(instances)
                .pageCount(pageCount)
                .build();

        BasicResponse response = BasicResponse.builder()
                .success(true)
                .message(Optional.empty())
                .data(Optional.of(data_response))
                .build();

        return ResponseEntity.status(200).body(response);
    }

    @PostMapping("/instance/create")
    private ResponseEntity<?> createInstance(
            @ModelAttribute @Valid CreateInstance dto,
            HttpServletResponse res
    ) {
        if (!EnumStringComparison.compareEnumString(dto.getInstanceType(), InstanceType.class) ||
                (!dto.getInstanceType().equals("t3a.nano")
                        && !dto.getInstanceType().equals("t3a.small")
                        && !dto.getInstanceType().equals("t3a.micro")
                        && !dto.getInstanceType().equals("t2.nano"))
        ) {
            BasicResponse response = BasicResponse.builder()
                    .success(false)
                    .message(Optional.of("Invalid Instance Type"))
                    .data(Optional.empty())
                    .build();

            return ResponseEntity.status(400).body(response);
        }

        BasicResponse response = BasicResponse.builder()
                .success(true)
                .message(Optional.empty())
                .data(Optional.empty())
                .build();

        Ec2Client ec2Client = manageInstanceService.getEc2Client();
        InstanceEntity instance = InstanceEntity.builder()
                .category(dto.getCategory())
                .name(dto.getName())
                .description(dto.getDescription())
                .owner(dto.getOwner())
                .type(dto.getInstanceType())
                .storageSize(dto.getStorageSize())
                .ports(dto.getPorts())
                .memo(dto.getMemo())
                .createdAt(LocalDateTime.now())
                .build();



        CompletableFuture<Failable<Boolean, String>> createInstanceFuture =
                CompletableFuture.supplyAsync(() ->
                    manageInstanceService.createInstance(ec2Client, instance)
                );

        createInstanceFuture.thenAccept(result -> {
                ec2Client.close();
                if (result.isError()) {
                    NoticeResponse gateway = NoticeResponse.builder()
                            .type("ERROR")
                            .message(Optional.of("인스턴스 생성 중 문제가 발생하였습니다.\n"+result.getError()))
                            .build();

                    new WebSocketHandler().send(gateway);
                } else {
                    NoticeResponse gateway = NoticeResponse.builder()
                            .type("SUCCESS")
                            .message(Optional.of("인스턴스를 성공적으로 생성했습니다."))
                            .build();

                    new WebSocketHandler().send(gateway);
                }
            createInstanceFuture.complete(null);
        });

        createInstanceFuture.exceptionally(ex -> {
                NoticeResponse gateway = NoticeResponse.builder()
                        .type("ERROR")
                        .message(Optional.of("인스턴스 생성 중 문제가 발생하였습니다."))
                        .build();

                new WebSocketHandler().send(gateway);
                ec2Client.close();
                createInstanceFuture.completeExceptionally(new ResponseStatusException(500, "인스턴스 생성 중 문제가 발생하였습니다.", ex));
                return null;
        });

        return ResponseEntity.status(200).body(response);
    }

    @DeleteMapping("/instance/{id}")
    private ResponseEntity<?> deleteInstance(
            @PathVariable("id") String id
    ) {
        Ec2Client ec2Client = manageInstanceService.getEc2Client();

        Optional<InstanceEntity> instance = manageInstanceService.findOneByUuid(id);
        if (instance.isEmpty()) {
            BasicResponse response = BasicResponse.builder()
                    .success(false)
                    .message(Optional.of("NOT FOUND INSTANCE"))
                    .data(Optional.empty())
                    .build();

            return ResponseEntity.status(404).body(response);
        }

        BasicResponse response = BasicResponse.builder()
                .success(true)
                .message(Optional.empty())
                .data(Optional.empty())
                .build();

        CompletableFuture<Failable<Boolean, String>> deleteInstanceFuture = CompletableFuture.supplyAsync(() ->
                manageInstanceService.deleteInstsance(ec2Client, instance.get())
        );

        deleteInstanceFuture.thenAccept(result -> {
            ec2Client.close();
            if (result.isError()) {
                NoticeResponse gateway = NoticeResponse.builder()
                        .type("ERROR")
                        .message(Optional.of("인스턴스 삭제 중 문제가 발생하였습니다.\n"+result.getError()))
                        .build();

                new WebSocketHandler().send(gateway);
            } else {
                NoticeResponse gateway = NoticeResponse.builder()
                        .type("SUCCESS")
                        .message(Optional.of("인스턴스를 성공적으로 생성했습니다."))
                        .build();

                new WebSocketHandler().send(gateway);
            }
            deleteInstanceFuture.complete(null);
        });

        deleteInstanceFuture.exceptionally(ex -> {
            NoticeResponse gateway = NoticeResponse.builder()
                    .type("ERROR")
                    .message(Optional.of("인스턴스 삭제 중 문제가 발생하였습니다."))
                    .build();

            new WebSocketHandler().send(gateway);
            ec2Client.close();
            deleteInstanceFuture.completeExceptionally(new ResponseStatusException(500, "인스턴스 생성 중 문제가 발생하였습니다.", ex));
            return null;
        });

        return ResponseEntity.status(200).body(response);
    }
}
