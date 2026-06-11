import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReportService } from '../../core/services/report.service';
import { CardModule } from 'primeng/card';
import { ChartModule } from 'primeng/chart';
import { TagModule } from 'primeng/tag';
import { SkeletonModule } from 'primeng/skeleton';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, CardModule, ChartModule, TagModule, SkeletonModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  dashboard = signal<any>(null);
  kpis = signal<any[]>([]);
  chartData = signal<any>(null);
  loading = signal(true);

  constructor(private reportService: ReportService) {}

  ngOnInit() {
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
    const labels = Object.keys(data.vehiclesByStatus);
    const values = Object.values(data.vehiclesByStatus) as number[];
    this.chartData.set({
      labels,
      datasets: [{
        data: values,
        backgroundColor: ['#22c55e','#f97316','#3b82f6','#8b5cf6','#ef4444']
      }]
    });
  }
}
