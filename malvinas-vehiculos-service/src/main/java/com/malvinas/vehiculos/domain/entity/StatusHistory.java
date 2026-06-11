package com.malvinas.vehiculos.domain.entity;

import com.malvinas.vehiculos.domain.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "status_history", schema = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class StatusHistory extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "previous_status", nullable = false, length = 2)
    private String previousStatus;

    @Column(name = "new_status", nullable = false, length = 2)
    private String newStatus;

    @Column(length = 200)
    private String reason;

    @Column(name = "employee_id")
    private Long employeeId;
}
