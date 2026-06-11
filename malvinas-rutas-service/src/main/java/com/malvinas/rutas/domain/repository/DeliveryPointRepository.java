package com.malvinas.rutas.domain.repository;

import com.malvinas.rutas.domain.entity.DeliveryPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeliveryPointRepository extends JpaRepository<DeliveryPoint, Long> {
    List<DeliveryPoint> findByIsActiveTrue();
}
