package com.threlease.base.functions.invite;

import com.threlease.base.entites.InviteEntity;
import com.threlease.base.functions.aws.ManageInstanceService;
import com.threlease.base.repositories.InviteRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class InviteService {
    private final InviteRepository inviteRepository;
    private final ManageInstanceService manageInstanceService;

    public InviteService(InviteRepository inviteRepository, ManageInstanceService manageInstanceService) {
        this.inviteRepository = inviteRepository;
        this.manageInstanceService = manageInstanceService;
    }

    public void inviteSave(InviteEntity data) {
        inviteRepository.save(data);
    }

    public void inviteRemove(InviteEntity data) {
        inviteRepository.delete(data);
    }

    public Optional<InviteEntity> findOneByUuid(String uuid) {
        return inviteRepository.findOneByUUID(uuid);
    }
}
