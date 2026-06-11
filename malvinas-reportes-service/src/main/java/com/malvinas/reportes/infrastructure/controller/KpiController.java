package com.malvinas.reportes.infrastructure.controller;

import com.malvinas.reportes.application.dto.KpiResponse;
import com.malvinas.reportes.domain.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping(ApiPaths.KPIS)
@RequiredArgsConstructor
@Tag(name = "KPIs", description = "Key performance indicators")
public class KpiController {

    private final ReportService reportService;

    @GetMapping
    @Operation(summary = "Get key performance indicators")
    public ResponseEntity<List<KpiResponse>> getKpis() {
        return ResponseEntity.ok(reportService.getKpis());
    }
}
