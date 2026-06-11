package com.malvinas.rutas.domain.entity;

import com.malvinas.rutas.domain.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "delivery_points", schema = "routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DeliveryPoint extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 200)
    private String address;

    @Column(length = 100)
    private String district;

    @Column(length = 200)
    private String reference;

    @Column(length = 100)
    private String contact;

    @Column(length = 15)
    private String phone;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
