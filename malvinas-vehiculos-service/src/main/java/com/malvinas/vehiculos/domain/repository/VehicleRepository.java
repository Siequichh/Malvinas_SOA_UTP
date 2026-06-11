package com.malvinas.vehiculos.domain.repository;

import com.malvinas.vehiculos.domain.entity.Vehicle;
import com.malvinas.vehiculos.domain.enumerate.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByLicensePlateAndIsActiveTrue(String licensePlate);
    List<Vehicle> findByStatusAndIsActiveTrue(VehicleStatus status);
    List<Vehicle> findByIsActiveTrue();
    boolean existsByLicensePlate(String licensePlate);
}
