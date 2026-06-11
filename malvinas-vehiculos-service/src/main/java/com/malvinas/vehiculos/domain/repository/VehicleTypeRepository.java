package com.malvinas.vehiculos.domain.repository;

import com.malvinas.vehiculos.domain.entity.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VehicleTypeRepository extends JpaRepository<VehicleType, Long> {
    List<VehicleType> findByIsActiveTrue();
}
