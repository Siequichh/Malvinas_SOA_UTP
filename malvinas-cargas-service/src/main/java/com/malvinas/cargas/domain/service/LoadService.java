package com.malvinas.cargas.domain.service;

import com.malvinas.cargas.application.dto.LoadRequest;
import com.malvinas.cargas.application.dto.LoadResponse;
import java.util.List;

public interface LoadService {
    LoadResponse create(LoadRequest request);
    LoadResponse findById(Long id);
    List<LoadResponse> findAll();
    List<LoadResponse> findActive();
    LoadResponse update(Long id, LoadRequest request);
    LoadResponse complete(Long id);
    LoadResponse cancel(Long id);
}
