package com.malvinas.reportes.domain.service.impl;

import com.malvinas.reportes.application.dto.DashboardResponse;
import com.malvinas.reportes.application.dto.FleetReportResponse;
import com.malvinas.reportes.application.dto.KpiResponse;
import com.malvinas.reportes.domain.service.ReportService;
import com.malvinas.reportes.infrastructure.client.AggregatorClient;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    private final AggregatorClient aggregatorClient;

    public ReportServiceImpl(AggregatorClient aggregatorClient) {
        this.aggregatorClient = aggregatorClient;
    }

    @Override
    public DashboardResponse getDashboard() {
        List<Map<String, Object>> vehicles = aggregatorClient.getVehicles();
        List<Map<String, Object>> activeLoads = aggregatorClient.getActiveLoads();
        List<Map<String, Object>> activeDispatches = aggregatorClient.getActiveDispatches();

        long total = vehicles.size();
        Map<String, Long> byStatus = vehicles.stream()
                .collect(Collectors.groupingBy(
                    v -> String.valueOf(v.getOrDefault("statusDisplay", "Unknown")),
                    Collectors.counting()));

        long available = vehicles.stream()
                .filter(v -> "01".equals(v.get("statusCode"))).count();
        long onRoute = vehicles.stream()
                .filter(v -> "04".equals(v.get("statusCode"))).count();
        double utilization = total > 0 ? (double)(total - available) / total * 100 : 0;

        return new DashboardResponse(total, available, onRoute,
            activeLoads.size(), activeDispatches.size(), activeDispatches.size(),
            Math.round(utilization * 10.0) / 10.0, byStatus);
    }

    @Override
    public FleetReportResponse getFleetReport() {
        List<Map<String, Object>> vehicles = aggregatorClient.getVehicles();
        long total = vehicles.size();
        long active = vehicles.stream()
                .filter(v -> !Boolean.FALSE.equals(v.get("isActive"))).count();
        Map<String, Long> byStatus = vehicles.stream()
                .collect(Collectors.groupingBy(
                    v -> String.valueOf(v.getOrDefault("statusDisplay", "Unknown")),
                    Collectors.counting()));

        long available = vehicles.stream().filter(v -> "01".equals(v.get("statusCode"))).count();
        double utilization = total > 0 ? (double)(total - available) / total * 100 : 0;

        List<FleetReportResponse.VehicleSummary> summaries = vehicles.stream()
                .map(v -> new FleetReportResponse.VehicleSummary(
                    String.valueOf(v.getOrDefault("licensePlate", "")),
                    String.valueOf(v.getOrDefault("vehicleTypeName", "")),
                    String.valueOf(v.getOrDefault("statusDisplay", ""))))
                .toList();

        return new FleetReportResponse(total, active,
            Math.round(utilization * 10.0) / 10.0, byStatus, summaries);
    }

    @Override
    public List<KpiResponse> getKpis() {
        DashboardResponse dashboard = getDashboard();
        return List.of(
            new KpiResponse("Total Vehiculos", String.valueOf(dashboard.totalVehicles()), "vehicles", "stable"),
            new KpiResponse("Disponibles", String.valueOf(dashboard.availableVehicles()), "vehicles", "up"),
            new KpiResponse("En Ruta", String.valueOf(dashboard.vehiclesOnRoute()), "vehicles", "stable"),
            new KpiResponse("Utilizacion Flota", String.format("%.1f", dashboard.fleetUtilizationPercent()), "%", "up"),
            new KpiResponse("Cargas Activas", String.valueOf(dashboard.activeLoads()), "loads", "stable"),
            new KpiResponse("Despachos Activos", String.valueOf(dashboard.activeDispatches()), "dispatches", "stable")
        );
    }
}
