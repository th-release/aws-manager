package com.threlease.base.functions.aws;

import com.threlease.base.entites.KeypairEntity;
import com.threlease.base.repositories.KeypairRepository;
import com.threlease.base.utils.Failable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class KeypairService {
    private final KeypairRepository keypairRepository;

    public KeypairService(KeypairRepository keypairRepository) {
        this.keypairRepository = keypairRepository;
    }

    public Optional<KeypairEntity> findOneById(String id) {
        return keypairRepository.findOneById(id);
    }

    public Optional<KeypairEntity> findOneByName(String name) {
        return keypairRepository.findOneByName(name);
    }

    public Page<KeypairEntity> findByPagination(Pageable pageable) {
        return keypairRepository.findByPagination(pageable);
    }

    public void keypairSave(KeypairEntity data) {
        keypairRepository.save(data);
    }

    public Failable<KeypairEntity, String> createKeypair(Ec2Client ec2Client, String name) {
        CreateKeyPairRequest request = CreateKeyPairRequest.builder()
                .keyName(name)
                .build();

        try {
            CreateKeyPairResponse response = ec2Client.createKeyPair(request);
            KeypairEntity data = KeypairEntity.builder()
                    .id(response.keyPairId())
                    .name(response.keyName())
                    .material(response.keyMaterial())
                    .fingerprint(response.keyFingerprint())
                    .build();

            keypairRepository.save(data);
            return Failable.success(data);
        } catch (Ec2Exception e) {
            return Failable.error(e.getMessage());
        }
    }

    public Failable<Boolean, String> deleteKeypair(Ec2Client ec2Client, String select, String type) {
        if (Objects.equals(type, "name")) {
            DeleteKeyPairRequest request = DeleteKeyPairRequest.builder()
                    .keyName(select)
                    .build();

            try {
                ec2Client.deleteKeyPair(request);
                Optional<KeypairEntity> keypair = keypairRepository.findOneByName(select);
                keypair.ifPresent(keypairRepository::delete);

                return Failable.success(true);
            } catch (Ec2Exception e) {
                return Failable.error(e.getMessage());
            }
        } else if (Objects.equals(type, "id")) {
            DeleteKeyPairRequest request = DeleteKeyPairRequest.builder()
                    .keyPairId(select)
                    .build();

            try {
                ec2Client.deleteKeyPair(request);
                Optional<KeypairEntity> keypair = keypairRepository.findOneById(select);
                keypair.ifPresent(keypairRepository::delete);

                return Failable.success(true);
            } catch (Ec2Exception e) {
                return Failable.error(e.getMessage());
            }
        } else {
            return Failable.error("Invaild Type");
        }
    }
}
