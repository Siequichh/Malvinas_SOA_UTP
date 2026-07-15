import { Component, OnInit, OnDestroy, HostListener, computed, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from '../core/services/auth.service';
import { LayoutService } from '../core/services/layout.service';
import { NotificationService } from '../core/services/notification.service';
import { VehicleService } from '../core/services/vehicle.service';
import { DispatchService } from '../core/services/dispatch.service';
import { LoadService } from '../core/services/load.service';
import { MessageService } from 'primeng/api';
import { RippleModule } from 'primeng/ripple';
import { BadgeModule } from 'primeng/badge';
import { ToastModule } from 'primeng/toast';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, RippleModule, BadgeModule, ToastModule],
  providers: [MessageService],
  templateUrl: './app.layout.component.html',
  styleUrls: ['./app.layout.component.scss']
})
export class AppLayoutComponent implements OnInit, OnDestroy {

  private readonly allMenuItems = [
    { label: 'Dashboard', icon: 'pi pi-home',      route: '/dashboard', roles: ['ADM','SUP'] },
    { label: 'Vehiculos',  icon: 'pi pi-car',       route: '/vehiculos', roles: ['ADM','SUP'] },
    { label: 'Cargas',     icon: 'pi pi-box',       route: '/cargas',    roles: ['ADM','SUP','MOV'] },
    { label: 'Personal',   icon: 'pi pi-users',     route: '/personal',  roles: ['ADM'] },
    { label: 'Rutas',      icon: 'pi pi-map',       route: '/rutas',     roles: ['ADM','SUP','DRV','SEC'] },
    { label: 'Reportes',   icon: 'pi pi-chart-bar', route: '/reportes',  roles: ['ADM','SUP'] }
  ];

  private readonly titleMap: Record<string, string> = {
    '/dashboard': 'Dashboard',
    '/vehiculos': 'Gestión de Vehículos',
    '/cargas':    'Control de Cargas',
    '/personal':  'Gestión de Personal',
    '/rutas':     'Rutas y Despachos',
    '/reportes':  'Reportes y KPIs',
  };

  private readonly roleLabels: Record<string, string> = {
    ADM: 'Administrador',
    SUP: 'Supervisor',
    MOV: 'Movilizador',
    DRV: 'Conductor',
    SEC: 'Seguridad'
  };

  readonly currentUser   = this.authService.currentUser;
  readonly userRole      = this.authService.userRole;
  readonly notifications = this.notificationService.notifications;
  readonly unreadCount   = this.notificationService.unreadCount;
  notifPanelOpen = signal(false);

  readonly menuItems = computed(() => {
    const role = this.userRole();
    return this.allMenuItems.filter(i => !role || i.roles.includes(role));
  });

  readonly sidebarClass = computed(() => {
    const role = this.userRole() ?? 'ADM';
    return `layout-sidebar role-${role}${this.sidebarOpen() ? ' sidebar--open' : ''}`;
  });

  readonly isDark      = computed(() => this.layout.isDarkTheme());
  readonly sidebarOpen = signal<boolean>(false);
  readonly pageTitle   = signal<string>('Dashboard');

  private pollTimer: ReturnType<typeof setInterval> | null = null;
  private prevVehicleStatusMap = new Map<string, string>();
  private prevDispatchStatusMap = new Map<number, string>();
  private prevLoadStatusMap = new Map<number, string>();
  private vehicleInit = false;
  private dispatchInit = false;
  private loadInit = false;

  constructor(
    private authService: AuthService,
    private layout: LayoutService,
    private router: Router,
    private messageService: MessageService,
    readonly notificationService: NotificationService,
    private vehicleService: VehicleService,
    private dispatchService: DispatchService,
    private loadService: LoadService,
  ) {
    // Forward notifications to the layout's toast outlet
    effect(() => {
      const n = this.notificationService.latestToast();
      if (n) this.messageService.add({ severity: n.severity, summary: n.title, detail: n.detail, life: 5000 });
    });
    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd)
    ).subscribe((e: any) => {
      const url = (e.urlAfterRedirects as string).split('?')[0];
      this.pageTitle.set(this.titleMap[url] ?? 'Malvina');
      if (window.innerWidth <= 768) this.sidebarOpen.set(false);
    });
  }

  ngOnInit(): void {
    this.layout.applyTheme();
    const url = this.router.url.split('?')[0];
    this.pageTitle.set(this.titleMap[url] ?? 'Malvina');
    this.startBackgroundPolling();
  }

  ngOnDestroy(): void {
    if (this.pollTimer) clearInterval(this.pollTimer);
  }

  private startBackgroundPolling() {
    const role = this.userRole();
    if (role === 'MOV') {
      this.pollVehicles();
      this.pollTimer = setInterval(() => this.pollVehicles(), 30_000);
    } else if (role === 'SUP' || role === 'ADM') {
      this.pollLoads();
      this.pollDispatches();
      this.pollTimer = setInterval(() => { this.pollLoads(); this.pollDispatches(); }, 30_000);
    }
  }

  private pollVehicles() {
    this.vehicleService.getVehicles().subscribe({
      next: (vehicles: any[]) => {
        if (this.vehicleInit) {
          for (const v of vehicles) {
            const prev = this.prevVehicleStatusMap.get(v.licensePlate);
            if (prev && prev !== v.statusCode) {
              if (v.statusCode === '01') {
                this.notificationService.notify('pi pi-check-circle', 'Vehículo disponible',
                  `${v.licensePlate} está disponible para nueva carga`, '/vehiculos', 'success');
              } else if (v.statusCode === '05') {
                this.notificationService.notify('pi pi-wrench', 'Vehículo en mantenimiento',
                  `${v.licensePlate} fue marcado como no disponible`, '/vehiculos', 'warn');
              }
            }
          }
        }
        this.vehicleInit = true;
        this.prevVehicleStatusMap = new Map(vehicles.map((v: any) => [v.licensePlate, v.statusCode]));
      },
      error: () => {}
    });
  }

  private pollLoads() {
    this.loadService.getLoads().subscribe({
      next: (loads: any[]) => {
        if (this.loadInit) {
          for (const l of loads) {
            const prev = this.prevLoadStatusMap.get(l.id);
            if (prev && prev !== '03' && l.statusCode === '03') {
              this.notificationService.notify('pi pi-check-circle', 'Vehículo Cargado',
                `${l.vehiclePlate} listo para despacho`, `/rutas?plate=${l.vehiclePlate}`, 'success');
            }
          }
        }
        this.loadInit = true;
        this.prevLoadStatusMap = new Map(loads.map((l: any) => [l.id, l.statusCode]));
      },
      error: () => {}
    });
  }

  private pollDispatches() {
    const userId = this.currentUser()?.id;
    this.dispatchService.getDispatches().subscribe({
      next: (dispatches: any[]) => {
        if (this.dispatchInit) {
          for (const d of dispatches) {
            const prev = this.prevDispatchStatusMap.get(d.id);
            if (!this.prevDispatchStatusMap.has(d.id) && String(d.createdBy) !== String(userId)) {
              this.notificationService.notify('pi pi-map', 'Nuevo despacho',
                `Despacho ${d.vehiclePlate} creado`, '/rutas', 'info');
            }
            if (prev === '01' && d.statusCode === '02') {
              this.notificationService.notify('pi pi-send', 'Despacho en ruta',
                `${d.vehiclePlate} aceptado por conductor`, '/rutas', 'success');
            }
            if (prev === '01' && d.statusCode === '04') {
              this.notificationService.notify('pi pi-times-circle', 'Despacho rechazado',
                `Conductor rechazó ${d.vehiclePlate} — reasignar conductor`, `/rutas?plate=${d.vehiclePlate}`, 'warn');
            }
            if (prev === '02' && d.statusCode === '03') {
              this.notificationService.notify('pi pi-home', 'Ruta cerrada',
                `${d.vehiclePlate} retornó a base`, '/rutas', 'success');
            }
          }
        }
        this.dispatchInit = true;
        this.prevDispatchStatusMap = new Map(dispatches.map((d: any) => [d.id, d.statusCode]));
      },
      error: () => {}
    });
  }

  @HostListener('window:resize')
  onResize(): void {
    if (window.innerWidth > 768) this.sidebarOpen.set(false);
  }

  toggleSidebar(): void   { this.sidebarOpen.update(v => !v); }
  closeSidebar(): void    { this.sidebarOpen.set(false); }
  toggleDark(): void      { this.layout.toggleDarkMode(); }
  logout(): void          { this.notificationService.stopDrvReminder(); this.authService.logout(); }
  toggleNotifPanel(): void { this.notifPanelOpen.update(v => !v); }
  closeNotifPanel(): void  { this.notifPanelOpen.set(false); }

  get roleLabel(): string {
    return this.roleLabels[this.userRole() ?? ''] ?? this.userRole() ?? '';
  }

  get userInitials(): string {
    const name  = this.currentUser()?.fullName ?? '';
    const parts = name.trim().split(' ');
    return parts.length >= 2
      ? `${parts[0][0]}${parts[1][0]}`.toUpperCase()
      : name.substring(0, 2).toUpperCase() || 'U';
  }
}
