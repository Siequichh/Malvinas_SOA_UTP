package com.malvinas.personal.domain.repository;

import com.malvinas.personal.domain.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByCode(String code);
    List<Role> findByIsActiveTrue();
}
