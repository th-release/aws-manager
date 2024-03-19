package com.threlease.base.repositories;

import com.threlease.base.entites.KeypairEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KeypairRepository extends JpaRepository<KeypairEntity, String> {
    @Query("SELECT u FROM KeypairEntity u WHERE u.id = :id")
    Optional<KeypairEntity> findOneById(@Param("id") String id);

    @Query("SELECT u FROM KeypairEntity u WHERE u.name = :name")
    Optional<KeypairEntity> findOneByName(@Param("name") String name);

    @Query(value = "SELECT u FROM KeypairEntity u")
    Page<KeypairEntity> findByPagination(Pageable pageable);
}
