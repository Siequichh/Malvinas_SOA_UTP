package com.malvinas.rutas.domain.entity;

import com.malvinas.rutas.domain.audit.AuditableEntity;
import com.malvinas.rutas.domain.enumerate.DispatchPriority;
import com.malvinas.rutas.domain.enumerate.DispatchStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dispatches", schema = "routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Dispatch extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_plate", nullable = false, length = 10)
    private String vehiclePlate;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "helper1_id")
    private Long helper1Id;

    @Column(name = "helper2_id")
    private Long helper2Id;

    @Column(name = "load_id")
    private Long loadId;

    @Column(nullable = false, length = 2)
    @Builder.Default
    private DispatchPriority priority = DispatchPriority.MEDIUM;

    @Column(nullable = false, length = 2)
    @Builder.Default
    private DispatchStatus status = DispatchStatus.SCHEDULED;

    @Column(name = "scheduled_departure_time")
    private LocalTime scheduledDepartureTime;

    @Column(name = "actual_departure_time")
    private LocalDateTime actualDepartureTime;

    @Column(name = "return_time")
    private LocalDateTime returnTime;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "loading_order_code", unique = true, length = 20)
    private String loadingOrderCode;

    @OneToMany(mappedBy = "dispatch", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DispatchPoint> dispatchPoints = new ArrayList<>();
}
