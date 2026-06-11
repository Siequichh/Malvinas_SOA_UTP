package com.malvinas.rutas.domain.entity;

import com.malvinas.rutas.domain.audit.AuditableEntity;
import com.malvinas.rutas.domain.enumerate.DeliveryStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "dispatch_points", schema = "routes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DispatchPoint extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatch_id", nullable = false)
    private Dispatch dispatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_point_id", nullable = false)
    private DeliveryPoint deliveryPoint;

    @Column(name = "visit_order", nullable = false)
    @Builder.Default
    private Short visitOrder = 1;

    @Column(name = "delivery_status", length = 2)
    @Builder.Default
    private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;

    @Column(name = "delivery_time")
    private LocalDateTime deliveryTime;

    @Column(length = 200)
    private String remark;
}
