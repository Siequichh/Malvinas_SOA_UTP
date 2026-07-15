import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReportService } from '../../core/services/report.service';
import { DispatchService } from '../../core/services/dispatch.service';
import { EmployeeService } from '../../core/services/employee.service';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ChartModule } from 'primeng/chart';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ProgressBarModule } from 'primeng/progressbar';

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule, ButtonModule, CardModule, ChartModule, TableModule, TagModule, ProgressBarModule],
  templateUrl: './reportes.component.html',
  styleUrls: ['./reportes.component.scss']
})
export class ReportesComponent implements OnInit {
  loading     = signal(false);
  dashboard   = signal<any>(null);
  kpis        = signal<any[]>([]);
  dispatches  = signal<any[]>([]);
  fleetChart  = signal<any>(null);
  dispatchChart = signal<any>(null);

  readonly chartOptions = {
    doughnut: {
      plugins: { legend: { position: 'bottom', labels: { padding: 16, color: '#94a3b8', font: { family: "'IBM Plex Sans'" } } } },
      responsive: true,
      maintainAspectRatio: false,
      cutout: '65%'
    },
    bar: {
      plugins: { legend: { display: false } },
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        x: { grid: { display: false }, border: { display: false }, ticks: { color: '#94a3b8' } },
        y: { grid: { color: 'rgba(100,116,139,0.15)' }, border: { display: false }, ticks: { precision: 0, color: '#94a3b8' } }
      }
    }
  };

  // ponytail: vehiclesByStatus keys are display names — map both display and enum names
  readonly FLEET_COLORS: Record<string, { label: string; color: string }> = {
    AVAILABLE:       { label: 'Disponible',    color: '#10b981' },
    LOADING:         { label: 'En Carga',      color: '#f59e0b' },
    LOADED:          { label: 'Cargado',       color: '#3b82f6' },
    ON_ROUTE:        { label: 'En Ruta',       color: '#8b5cf6' },
    MAINTENANCE:     { label: 'Mantenimiento', color: '#ef4444' },
    'Disponible':    { label: 'Disponible',    color: '#10b981' },
    'En Carga':      { label: 'En Carga',      color: '#f59e0b' },
    'Cargado':       { label: 'Cargado',       color: '#3b82f6' },
    'En Ruta':       { label: 'En Ruta',       color: '#8b5cf6' },
    'Mantenimiento': { label: 'Mantenimiento', color: '#ef4444' }
  };

  constructor(
    private reportService: ReportService,
    private dispatchService: DispatchService,
    private employeeService: EmployeeService
  ) {}

  ngOnInit() { this.loadAll(); }

  loadAll() {
    this.loading.set(true);
    this.reportService.getDashboard().subscribe({ next: d => { this.dashboard.set(d); this.buildFleetChart(d); } });
    this.reportService.getKpis().subscribe({ next: k => this.kpis.set(k) });
    this.employeeService.getEmployees().subscribe({
      next: (emps) => {
        const driverMap = new Map(emps.map((e: any) => [e.id, `${e.firstName} ${e.lastName}`]));
        this.dispatchService.getDispatches().subscribe({
          next: d => {
            const enriched = d.map((x: any) => ({ ...x, driverName: driverMap.get(x.driverId) ?? '—' }));
            this.dispatches.set(enriched.slice(0, 10));
            this.buildDispatchChart(d);
            this.loading.set(false);
          },
          error: () => this.loading.set(false)
        });
      },
      error: () => {
        this.dispatchService.getDispatches().subscribe({
          next: d => { this.dispatches.set(d.slice(0, 10)); this.buildDispatchChart(d); this.loading.set(false); },
          error: () => this.loading.set(false)
        });
      }
    });
  }

  private buildFleetChart(d: any) {
    const entries = Object.entries(d.vehiclesByStatus ?? {});
    this.fleetChart.set({
      labels: entries.map(([k]) => this.FLEET_COLORS[k]?.label ?? k),
      datasets: [{
        data: entries.map(([, v]) => v),
        backgroundColor:      entries.map(([k]) => this.FLEET_COLORS[k]?.color ?? '#94a3b8'),
        hoverBackgroundColor: entries.map(([k]) => this.FLEET_COLORS[k]?.color ?? '#94a3b8'),
        borderWidth: 0
      }]
    });
  }

  private buildDispatchChart(dispatches: any[]) {
    const counts: Record<string, number> = { '01': 0, '02': 0, '03': 0, '04': 0 };
    dispatches.forEach(d => { if (d.statusCode) counts[d.statusCode] = (counts[d.statusCode] ?? 0) + 1; });
    this.dispatchChart.set({
      labels: ['Programado', 'En Ruta', 'Completado', 'Cancelado'],
      datasets: [{
        label: 'Despachos',
        data: [counts['01'], counts['02'], counts['03'], counts['04']],
        backgroundColor: ['#0ea5e9', '#f59e0b', '#10b981', '#ef4444'],
        borderRadius: 6
      }]
    });
  }

  getDispatchSeverity(code: string): string {
    const m: Record<string, string> = { '01': 'info', '02': 'warning', '03': 'success', '04': 'danger' };
    return m[code] ?? 'secondary';
  }

  getDispatchLabel(code: string): string {
    const m: Record<string, string> = { '01': 'Programado', '02': 'En Ruta', '03': 'Completado', '04': 'Cancelado' };
    return m[code] ?? code;
  }
}
