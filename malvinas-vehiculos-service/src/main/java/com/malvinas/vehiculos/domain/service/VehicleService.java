package com.malvinas.vehiculos.domain.service;

import com.malvinas.vehiculos.application.dto.*;
import java.util.List;

public interface VehicleService {
    List<VehicleResponse> findAll();
    VehicleResponse findByPlate(String plate);
    VehicleResponse create(VehicleRequest request);
    VehicleResponse update(String plate, VehicleRequest request);
    VehicleResponse changeStatus(String plate, StatusChangeRequest request);
    List<VehicleResponse> findByStatus(String statusCode);
    List<StatusHistoryResponse> findHistory(String plate);
    void deactivate(String plate);
}
