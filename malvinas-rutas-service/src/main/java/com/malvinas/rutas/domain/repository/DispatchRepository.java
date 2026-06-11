package com.malvinas.rutas.domain.repository;

import com.malvinas.rutas.domain.entity.Dispatch;
import com.malvinas.rutas.domain.enumerate.DispatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DispatchRepository extends JpaRepository<Dispatch, Long> {
    List<Dispatch> findByStatus(DispatchStatus status);
    List<Dispatch> findByDriverId(Long driverId);
}
