package com.malvinas.vehiculos.domain.entity;

import com.malvinas.vehiculos.domain.audit.AuditableEntity;
import com.malvinas.vehiculos.domain.enumerate.VehicleStatus;
import com.malvinas.vehiculos.infrastructure.util.converter.VehicleStatusConverter;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "vehicles", schema = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Vehicle extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "license_plate", nullable = false, unique = true, length = 10)
    private String licensePlate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_id", nullable = false)
    private VehicleType vehicleType;

    @Column(length = 50)
    private String brand;

    @Column(length = 50)
    private String model;

    private Short year;

    @Column(length = 30)
    private String color;

    @Convert(converter = VehicleStatusConverter.class)
    @Column(nullable = false, length = 2)
    @Builder.Default
    private VehicleStatus status = VehicleStatus.AVAILABLE;

    @Builder.Default
    private Integer mileage = 0;

    @Column(name = "soat_expiry_date")
    private LocalDate soatExpiryDate;

    @Column(name = "technical_inspection_date")
    private LocalDate technicalInspectionDate;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
