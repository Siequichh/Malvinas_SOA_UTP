package com.malvinas.personal.domain.entity;

import com.malvinas.personal.domain.audit.AuditableEntity;
import com.malvinas.personal.domain.enumerate.AttendanceStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "attendances", schema = "staff",
       uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "date"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class Attendance extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "check_in_time")
    private LocalTime checkInTime;

    @Column(name = "check_out_time")
    private LocalTime checkOutTime;

    @Column(name = "status", nullable = false, length = 2)
    @Builder.Default
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    @Column(name = "remark", length = 200)
    private String remark;
}
