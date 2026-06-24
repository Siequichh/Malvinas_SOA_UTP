import { Component, OnInit, HostListener, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from '../core/services/auth.service';
import { LayoutService } from '../core/services/layout.service';
import { RippleModule } from 'primeng/ripple';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, RippleModule],
  templateUrl: './app.layout.component.html',
  styleUrls: ['./app.layout.component.scss']
})
export class AppLayoutComponent implements OnInit {

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

  readonly currentUser = this.authService.currentUser;
  readonly userRole    = this.authService.userRole;

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

  constructor(
    private authService: AuthService,
    private layout: LayoutService,
    private router: Router,
  ) {
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
  }

  @HostListener('window:resize')
  onResize(): void {
    if (window.innerWidth > 768) this.sidebarOpen.set(false);
  }

  toggleSidebar(): void { this.sidebarOpen.update(v => !v); }
  closeSidebar(): void  { this.sidebarOpen.set(false); }
  toggleDark(): void    { this.layout.toggleDarkMode(); }
  logout(): void        { this.authService.logout(); }

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
