package com.malvinas.reportes.application.dto;

import java.util.Map;

public record DashboardResponse(
    long totalVehicles,
    long availableVehicles,
    long vehiclesOnRoute,
    long activeLoads,
    long activeDispatches,
    long dispatchesToday,
    double fleetUtilizationPercent,
    Map<String, Long> vehiclesByStatus
) {}
