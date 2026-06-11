package com.malvinas.personal.domain.repository;

import com.malvinas.personal.domain.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
    Optional<Employee> findByDni(String dni);
    Optional<Employee> findByEmail(String email);
    List<Employee> findByIsActiveTrue();
    List<Employee> findByRoleCode(String roleCode);
    boolean existsByDni(String dni);
    boolean existsByEmail(String email);
}
