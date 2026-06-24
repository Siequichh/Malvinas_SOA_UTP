import { Component, OnInit, OnDestroy, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ReportService } from '../../core/services/report.service';
import { AuthService } from '../../core/services/auth.service';
import { ChartModule } from 'primeng/chart';
import { TagModule } from 'primeng/tag';
import { SkeletonModule } from 'primeng/skeleton';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, ChartModule, TagModule, SkeletonModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit, OnDestroy {
  readonly userRole = inject(AuthService).userRole;
  dashboard = signal<any>(null);
  kpis = signal<any[]>([]);
  chartData = signal<any>(null);
  loading = signal(true);

  private refreshTimer: ReturnType<typeof setInterval> | null = null;

  readonly chartOptions = {
    plugins: {
      legend: {
        position: 'right' as const,
        labels: { color: '#94a3b8', padding: 12, font: { family: "'IBM Plex Sans'" } }
      }
    },
    responsive: true,
    maintainAspectRatio: false,
    cutout: '65%'
  };

  // ponytail: vehiclesByStatus keys are display names ("Disponible") not enum names (AVAILABLE)
  private readonly FLEET_COLORS: Record<string, string> = {
    'Disponible':    '#10b981',
    'En Carga':      '#f59e0b',
    'Cargado':       '#3b82f6',
    'En Ruta':       '#8b5cf6',
    'Mantenimiento': '#ef4444',
    AVAILABLE:       '#10b981',
    LOADING:         '#f59e0b',
    LOADED:          '#3b82f6',
    ON_ROUTE:        '#8b5cf6',
    MAINTENANCE:     '#ef4444'
  };

  constructor(private reportService: ReportService) {}

  ngOnInit() {
    this.loadDashboard();
    this.refreshTimer = setInterval(() => this.loadDashboard(), 30_000);
  }

  ngOnDestroy() {
    if (this.refreshTimer) clearInterval(this.refreshTimer);
  }

  private loadDashboard() {
    this.reportService.getDashboard().subscribe({
      next: (data) => {
        this.dashboard.set(data);
        this.buildChart(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
    this.reportService.getKpis().subscribe({ next: (k) => this.kpis.set(k) });
  }

  private buildChart(data: any) {
    if (!data?.vehiclesByStatus) return;
    const entries = Object.entries(data.vehiclesByStatus);
    this.chartData.set({
      labels: entries.map(([k]) => k),
      datasets: [{
        data: entries.map(([, v]) => v),
        backgroundColor:      entries.map(([k]) => this.FLEET_COLORS[k] ?? '#94a3b8'),
        hoverBackgroundColor: entries.map(([k]) => this.FLEET_COLORS[k] ?? '#94a3b8'),
        borderWidth: 0
      }]
    });
  }
}
