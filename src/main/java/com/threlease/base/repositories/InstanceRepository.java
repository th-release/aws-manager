package com.threlease.base.repositories;

import com.threlease.base.entites.InstanceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InstanceRepository extends JpaRepository<InstanceEntity, String> {
    @Query("SELECT u FROM InstanceEntity u WHERE u.uuid = :uuid")
    Optional<InstanceEntity> findOneByUUID(@Param("uuid") String uuid);

    @Query("SELECT u FROM InstanceEntity u WHERE u.name = :name")
    Optional<InstanceEntity> findOneByName(@Param("name") String name);

    @Query(value = "SELECT u FROM InstanceEntity u")
    Page<InstanceEntity> findByPagination(Pageable pageable);
}
