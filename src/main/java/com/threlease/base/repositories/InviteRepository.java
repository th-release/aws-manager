package com.threlease.base.repositories;

import com.threlease.base.entites.InviteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InviteRepository extends JpaRepository<InviteEntity, String> {
    @Query("SELECT u FROM InviteEntity u WHERE u.uuid = :uuid")
    Optional<InviteEntity> findOneByUUID(@Param("uuid") String uuid);
}
