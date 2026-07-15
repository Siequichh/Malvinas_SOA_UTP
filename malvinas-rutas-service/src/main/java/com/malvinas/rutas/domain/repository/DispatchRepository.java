package com.malvinas.rutas.domain.repository;

import com.malvinas.rutas.domain.entity.Dispatch;
import com.malvinas.rutas.domain.enumerate.DispatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface DispatchRepository extends JpaRepository<Dispatch, Long> {
    List<Dispatch> findByStatus(DispatchStatus status);
    List<Dispatch> findByDriverId(Long driverId);

    @Query("SELECT d.loadingOrderCode FROM Dispatch d WHERE d.loadingOrderCode IS NOT NULL ORDER BY d.loadingOrderCode DESC LIMIT 1")
    Optional<String> findMaxLoadingOrderCode();
}
