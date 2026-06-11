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
    DispatchResponse complete(Long id);
}
