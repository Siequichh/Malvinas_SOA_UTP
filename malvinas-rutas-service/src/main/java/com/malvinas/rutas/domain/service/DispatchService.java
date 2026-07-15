package com.malvinas.rutas.domain.service;

import com.malvinas.rutas.application.dto.DispatchRequest;
import com.malvinas.rutas.application.dto.DispatchResponse;
import java.util.List;

public interface DispatchService {
    DispatchResponse create(DispatchRequest request);
    DispatchResponse findById(Long id);
    List<DispatchResponse> findAll();
    List<DispatchResponse> findActive();
    DispatchResponse registerDeparture(Long id);
    DispatchResponse accept(Long id, Long employeeId);
    DispatchResponse complete(Long id);
    List<DispatchResponse> findByDriver(Long driverId);
    DispatchResponse cancel(Long id);
    DispatchResponse update(Long id, DispatchRequest request);
}
