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

    @Query(value = "SELECT u FROM InstanceEntity u WHERE " +
                    "u.category LIKE %:query% OR "+
                    "u.name LIKE %:query% OR " +
                    "u.description LIKE %:query% OR "+
                    "u.owner LIKE %:query% OR " +
                    "u.memo LIKE %:query% OR " +
                    "u.publicIP LIKE %:query% OR " +
                    "u.uuid LIKE %:query%"
    )
    Page<InstanceEntity> findBySearchPagination(Pageable pageable, @Param("query") String query);

    @Query("SELECT COUNT(u) FROM InstanceEntity u WHERE " +
            "u.category LIKE %:query% OR " +
            "u.name LIKE %:query% OR " +
            "u.description LIKE %:query% OR " +
            "u.owner LIKE %:query% OR " +
            "u.memo LIKE %:query% OR " +
            "u.publicIP LIKE %:query% OR " +
            "u.uuid LIKE %:query%")
    long countBySearch(@Param("query") String query);
}
