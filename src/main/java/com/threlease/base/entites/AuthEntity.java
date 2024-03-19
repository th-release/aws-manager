package com.threlease.base.entites;

import com.threlease.base.functions.auth.Roles;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Builder
@Entity
@Table(name = "AuthEntity")
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AuthEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String uuid;

    private String username;

    @Column(columnDefinition = "text")
    private String password;

    private String salt;

    @Enumerated(EnumType.STRING)
    private Roles role;
}
