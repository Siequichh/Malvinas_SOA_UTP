package com.malvinas.personal.domain.entity;

import com.malvinas.personal.domain.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDate;

@Entity
@Table(name = "employees", schema = "staff")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class Employee extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "dni", nullable = false, unique = true, length = 8)
    private String dni;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "phone", length = 15)
    private String phone;

    @Column(name = "email", unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "license_number", length = 20)
    private String licenseNumber;

    @Column(name = "license_category", length = 5)
    private String licenseCategory;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
