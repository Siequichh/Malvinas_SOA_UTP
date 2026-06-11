package com.malvinas.cargas.domain.entity;

import com.malvinas.cargas.domain.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "load_details", schema = "loads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LoadDetail extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "load_id", nullable = false)
    private Load load;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "weight_kg", precision = 10, scale = 2)
    private BigDecimal weightKg;

    @Column(length = 200)
    private String remark;
}
