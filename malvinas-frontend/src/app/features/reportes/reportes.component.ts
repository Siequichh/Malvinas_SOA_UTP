import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReportService } from '../../core/services/report.service';
import { CardModule } from 'primeng/card';
import { ChartModule } from 'primeng/chart';
import { TableModule } from 'primeng/table';

@Component({
  selector: 'app-reportes',
  standalone: true,
  imports: [CommonModule, CardModule, ChartModule, TableModule],
  templateUrl: './reportes.component.html',
  styleUrls: ['./reportes.component.scss']
})
export class ReportesComponent implements OnInit {
  fleetReport = signal<any>(null);
  kpis = signal<any[]>([]);

  constructor(private reportService: ReportService) {}

  ngOnInit() {
    this.reportService.getFleetReport().subscribe({ next: (r) => this.fleetReport.set(r) });
    this.reportService.getKpis().subscribe({ next: (k) => this.kpis.set(k) });
  }
}
