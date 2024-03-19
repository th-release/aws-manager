package com.threlease.base.entites;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import software.amazon.awssdk.services.ec2.model.InstanceType;

import java.time.LocalDateTime;

@Data
@Builder
@Entity
@Table(name = "InstanceEntity")
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class InstanceEntity {
    @Id
    private String uuid;
    private String category;
    private String name;
    private String description;
    private String owner;
    private String type;
    private int storageSize;
//    Comma - separated
    private String ports;
    private String memo;
//  system-specific ---
    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "keypairId", referencedColumnName = "id")
    private KeypairEntity keypairId;

    private String publicIP;

    @Column(columnDefinition = "real")
    private double pricePerHour;

    @CreatedDate
    private LocalDateTime createdAt;
}
