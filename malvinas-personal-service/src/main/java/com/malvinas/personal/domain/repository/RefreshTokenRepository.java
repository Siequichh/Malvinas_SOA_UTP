package com.malvinas.personal.domain.repository;

import com.malvinas.personal.domain.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByEmployeeId(Integer employeeId);
}
