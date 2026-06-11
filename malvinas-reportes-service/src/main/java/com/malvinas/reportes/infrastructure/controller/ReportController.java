package com.malvinas.reportes.infrastructure.controller;

import com.malvinas.reportes.application.dto.DashboardResponse;
import com.malvinas.reportes.application.dto.FleetReportResponse;
import com.malvinas.reportes.domain.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.REPORTS)
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Analytics and KPI reporting")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get real-time dashboard KPIs")
    public ResponseEntity<DashboardResponse> getDashboard() {
        return ResponseEntity.ok(reportService.getDashboard());
    }

    @GetMapping("/fleet")
    @Operation(summary = "Get fleet utilization report")
    public ResponseEntity<FleetReportResponse> getFleetReport() {
        return ResponseEntity.ok(reportService.getFleetReport());
    }
}
