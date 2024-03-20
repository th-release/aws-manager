package com.threlease.base.functions.invite;

import com.threlease.base.entites.AuthEntity;
import com.threlease.base.entites.InstanceEntity;
import com.threlease.base.entites.InviteEntity;
import com.threlease.base.functions.auth.AuthService;
import com.threlease.base.functions.aws.ManageInstanceService;
import com.threlease.base.functions.invite.dto.CreateInvite;
import com.threlease.base.utils.Failable;
import com.threlease.base.utils.responses.BasicResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.util.Optional;

@RestController
@RequestMapping("/invite")
public class InviteController {
    private final InviteService inviteService;
    private final AuthService authService;
    private final ManageInstanceService manageInstanceService;

    public InviteController(InviteService inviteService, AuthService authService, ManageInstanceService manageInstanceService) {
        this.inviteService = inviteService;
        this.authService = authService;
        this.manageInstanceService = manageInstanceService;
    }

    @GetMapping("/{uuid}")
    private ResponseEntity<?> getInviteInstance(
            @PathVariable("uuid") String uuid
    ) {
        Optional<InviteEntity> invite = inviteService.findOneByUuid(uuid);
        if (invite.isEmpty()) {
            BasicResponse response = BasicResponse.builder()
                .success(false)
                .message(Optional.of("NOT FOUND INVITE"))
                .data(Optional.empty())
                .build();

            return ResponseEntity.status(404).body(response);
        }

        BasicResponse response = BasicResponse.builder()
            .success(true)
            .message(Optional.empty())
            .data(Optional.of(invite.get()))
            .build();

        return ResponseEntity.status(200).body(response);
    }

    @PostMapping("/")
    private ResponseEntity<?> createInvite(
            @ModelAttribute @Valid CreateInvite dto,
            @RequestHeader("Authorization") String token
    ) {
            Optional<AuthEntity> user = authService.findOneByToken(token);

            if (user.isPresent()) {
                    Optional<InstanceEntity> instance = manageInstanceService.findOneByUuid(dto.getId());
                    if (instance.isEmpty()) {
                            BasicResponse response = BasicResponse.builder()
                                    .success(false)
                                    .message(Optional.of("NOT FOUND INSTANCE"))
                                    .data(Optional.empty())
                                    .build();

                            return ResponseEntity.status(404).body(response);
                    }

                    InviteEntity invite = InviteEntity.builder()
                            .instance(instance.get())
                            .build();

                    inviteService.inviteSave(invite);

                    BasicResponse response = BasicResponse.builder()
                            .success(true)
                            .message(Optional.empty())
                            .data(Optional.of(invite))
                            .build();

                    return ResponseEntity.status(201).body(response);
            } else {
                    BasicResponse response = BasicResponse.builder()
                            .success(false)
                            .message(Optional.of("INVALID SESSION"))
                            .data(Optional.empty())
                            .build();

                    return ResponseEntity.status(403).body(response);
            }
    }

    @PostMapping("/{id}/restart")
    public ResponseEntity<?> restartInviteInstance(
            @PathVariable("id") String id
    ) {
        Ec2Client ec2Client = manageInstanceService.getEc2Client();
        Optional<InviteEntity> invite = inviteService.findOneByUuid(id);
        if (invite.isEmpty()) {
            BasicResponse response = BasicResponse.builder()
                    .success(false)
                    .message(Optional.of("INVALID INVITE"))
                    .data(Optional.empty())
                    .build();

            return ResponseEntity.status(403).body(response);
        }

        Failable<Boolean, String> restartRequest = manageInstanceService.restartInstance(
                ec2Client,
                invite.get().getInstance()
        );
        ec2Client.close();

        if (restartRequest.isError()) {
            BasicResponse response = BasicResponse.builder()
                    .success(false)
                    .message(Optional.of(restartRequest.getError()))
                    .data(Optional.empty())
                    .build();

            return ResponseEntity.status(500).body(response);
        }

        BasicResponse response = BasicResponse.builder()
                .success(true)
                .message(Optional.empty())
                .data(Optional.empty())
                .build();

        return ResponseEntity.status(200).body(response);
    }

    @PostMapping("/{id}/reset")
    public ResponseEntity<?> resetInviteInstance(
            @PathVariable("id") String id
    ) {
        Ec2Client ec2Client = manageInstanceService.getEc2Client();
        Optional<InviteEntity> invite = inviteService.findOneByUuid(id);
        if (invite.isEmpty()) {
                BasicResponse response = BasicResponse.builder()
                        .success(false)
                        .message(Optional.of("INVALID INVITE"))
                        .data(Optional.empty())
                        .build();

                return ResponseEntity.status(403).body(response);
        }

        Failable<Boolean, String> resetRequest = manageInstanceService.resetInstance(
                ec2Client,
                invite.get().getInstance()
        );

        ec2Client.close();

        if (resetRequest.isError()) {
            BasicResponse response = BasicResponse.builder()
                    .success(false)
                    .message(Optional.of(resetRequest.getError()))
                    .data(Optional.empty())
                    .build();

            return ResponseEntity.status(500).body(response);
        }

        BasicResponse response = BasicResponse.builder()
                .success(true)
                .message(Optional.empty())
                .data(Optional.empty())
                .build();

        return ResponseEntity.status(200).body(response);
    }

    @GetMapping("/{id}/keypair")
    public ResponseEntity<?> getKeypairInviteInstance(
            @PathVariable("id") String id
    ) {
        Ec2Client ec2Client = manageInstanceService.getEc2Client();
        Optional<InviteEntity> invite = inviteService.findOneByUuid(id);
        if (invite.isEmpty()) {
            BasicResponse response = BasicResponse.builder()
                    .success(false)
                    .message(Optional.of("INVALID INVITE"))
                    .data(Optional.empty())
                    .build();

            return ResponseEntity.status(403).body(response);
        }

        Failable<Boolean, String> resetRequest = manageInstanceService.resetInstance(
                ec2Client,
                invite.get().getInstance()
        );

        ec2Client.close();

        if (resetRequest.isError()) {
            BasicResponse response = BasicResponse.builder()
                    .success(false)
                    .message(Optional.of(resetRequest.getError()))
                    .data(Optional.empty())
                    .build();

            return ResponseEntity.status(500).body(response);
        }

        BasicResponse response = BasicResponse.builder()
                .success(true)
                .message(Optional.empty())
                .data(Optional.of(invite.get().getInstance().getKeypair()))
                .build();

        return ResponseEntity.status(200).body(response);
    }
}
