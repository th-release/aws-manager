package com.threlease.base.functions.invite;

import com.threlease.base.entites.InviteEntity;
import com.threlease.base.repositories.InviteRepository;

import java.util.Optional;

public class InviteService {
    private final InviteRepository inviteRepository;

    public InviteService(InviteRepository inviteRepository) {
        this.inviteRepository = inviteRepository;
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
