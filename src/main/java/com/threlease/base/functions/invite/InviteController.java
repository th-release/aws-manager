package com.threlease.base.functions.invite;

import com.threlease.base.entites.InviteEntity;
import com.threlease.base.utils.responses.BasicResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/invite")
public class InviteController {
    private final InviteService inviteService;

    public InviteController(InviteService inviteService) {
        this.inviteService = inviteService;
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
}
