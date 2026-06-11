package com.malvinas.cargas.domain.repository;

import com.malvinas.cargas.domain.entity.Load;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoadRepository extends JpaRepository<Load, Long> {
    List<Load> findByStatus(String status);
    List<Load> findByVehiclePlate(String vehiclePlate);
}
