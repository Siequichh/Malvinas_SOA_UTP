package com.malvinas.vehiculos.domain.entity;

import com.malvinas.vehiculos.domain.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "vehicle_types", schema = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class VehicleType extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "capacity_kg", precision = 10, scale = 2)
    private BigDecimal capacityKg;

    @Column(length = 200)
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
