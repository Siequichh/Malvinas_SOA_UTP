package com.malvinas.personal.domain.entity;

import com.malvinas.personal.domain.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "roles", schema = "staff")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class Role extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code", nullable = false, unique = true, length = 3)
    private String code;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
