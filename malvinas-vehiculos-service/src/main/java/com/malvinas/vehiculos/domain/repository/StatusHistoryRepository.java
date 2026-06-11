package com.malvinas.vehiculos.domain.repository;

import com.malvinas.vehiculos.domain.entity.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {
    List<StatusHistory> findByVehicle_LicensePlateOrderByCreatedAtDesc(String plate);
}
