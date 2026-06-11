package com.malvinas.reportes.domain.service;

import com.malvinas.reportes.application.dto.DashboardResponse;
import com.malvinas.reportes.application.dto.FleetReportResponse;
import com.malvinas.reportes.application.dto.KpiResponse;
import java.util.List;

public interface ReportService {
    DashboardResponse getDashboard();
    FleetReportResponse getFleetReport();
    List<KpiResponse> getKpis();
}
