package com.malvinas.personal.domain.entity;

import com.malvinas.personal.domain.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens", schema = "staff")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class RefreshToken extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "token", nullable = false, unique = true, length = 500)
    private String token;

    @Column(name = "device_info", length = 200)
    private String deviceInfo;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_revoked", nullable = false)
    private Boolean isRevoked = false;
}
