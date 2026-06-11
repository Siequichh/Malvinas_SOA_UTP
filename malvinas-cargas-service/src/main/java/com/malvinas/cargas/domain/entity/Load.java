package com.malvinas.cargas.domain.entity;

import com.malvinas.cargas.domain.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loads", schema = "loads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Load extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_plate", nullable = false, length = 10)
    private String vehiclePlate;

    @Column(name = "mobilizer_id", nullable = false)
    private Long mobilizerId;

    @Column(nullable = false, length = 2)
    @Builder.Default
    private String status = "01";

    @Column(name = "loading_start_time")
    private LocalDateTime loadingStartTime;

    @Column(name = "loading_end_time")
    private LocalDateTime loadingEndTime;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "loading_plant", length = 100)
    @Builder.Default
    private String loadingPlant = "Babel - Huachipa";

    @OneToMany(mappedBy = "load", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LoadDetail> details = new ArrayList<>();
}
