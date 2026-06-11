package com.malvinas.reportes.application.dto;

import java.util.List;
import java.util.Map;

public record FleetReportResponse(
    long totalVehicles,
    long activeVehicles,
    double utilizationPercent,
    Map<String, Long> vehiclesByStatus,
    List<VehicleSummary> vehicles
) {
    public record VehicleSummary(String licensePlate, String type, String status) {}
}
