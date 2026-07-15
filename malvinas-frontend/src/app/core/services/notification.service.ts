import { Injectable, signal, computed, inject } from '@angular/core';
import { Router } from '@angular/router';

export interface AppNotification {
  id: number;
  icon: string;
  title: string;
  detail: string;
  time: Date;
  read: boolean;
  route?: string;
  severity: string;
}

let _seq = 0;

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private router = inject(Router);

  private _notifications = signal<AppNotification[]>([]);
  readonly notifications = this._notifications.asReadonly();
  readonly unreadCount = computed(() => this._notifications().filter(n => !n.read).length);
  /** Latest notification not yet consumed by the toast outlet — layout reads this to show toast */
  private _latestToast = signal<AppNotification | null>(null);
  readonly latestToast = this._latestToast.asReadonly();

  // DRV 5-min reminder (RR-05)
  private drvReminderId: ReturnType<typeof setInterval> | null = null;
  private pendingDispatchIds = new Set<number>();

  notify(icon: string, title: string, detail: string, route?: string, severity: string = 'info') {
    const n: AppNotification = { id: ++_seq, icon, title, detail, time: new Date(), read: false, route, severity };
    this._notifications.update(list => [n, ...list].slice(0, 50));
    this._latestToast.set(n);

    // Browser native notification when tab is hidden
    if (typeof document !== 'undefined' && document.hidden && typeof Notification !== 'undefined' && Notification.permission === 'granted') {
      new Notification(title, { body: detail, icon: '/favicon.ico' });
    }
  }

  markAllRead() {
    this._notifications.update(list => list.map(n => ({ ...n, read: true })));
  }

  navigateTo(n: AppNotification) {
    this.markRead(n.id);
    if (!n.route) return;
    const [path, qs] = n.route.split('?');
    if (qs) {
      const params: Record<string, string> = {};
      qs.split('&').forEach(p => { const [k, v] = p.split('='); if (k) params[k] = decodeURIComponent(v ?? ''); });
      this.router.navigate([path], { queryParams: params });
    } else {
      this.router.navigate([path]);
    }
  }

  markRead(id: number) {
    this._notifications.update(list => list.map(n => n.id === id ? { ...n, read: true } : n));
  }

  async requestPermission(): Promise<void> {
    if ('Notification' in window && Notification.permission === 'default') {
      await Notification.requestPermission();
    }
  }

  // Called by rutas component when DRV has pending dispatches
  startDrvReminder(dispatchIds: number[], driverName: string) {
    const newIds = new Set(dispatchIds);
    // Notify for new pending dispatches not seen before
    for (const id of newIds) {
      if (!this.pendingDispatchIds.has(id)) {
        this.notify('pi pi-send', 'Despacho asignado', `Tienes un despacho pendiente de aceptar`, '/rutas', 'warn');
      }
    }
    this.pendingDispatchIds = newIds;

    // 5-min reminder interval (RR-05)
    if (this.drvReminderId) clearInterval(this.drvReminderId);
    if (dispatchIds.length > 0) {
      this.drvReminderId = setInterval(() => {
        if (this.pendingDispatchIds.size > 0) {
          this.notify('pi pi-bell', 'Recordatorio', `Tienes ${this.pendingDispatchIds.size} despacho(s) pendiente(s) de aceptar`, '/rutas', 'warn');
        }
      }, 5 * 60 * 1000);
    }
  }

  /** Stop reminder + clear state — call when dispatch accepted/rejected */
  stopDrvReminder() {
    if (this.drvReminderId) { clearInterval(this.drvReminderId); this.drvReminderId = null; }
    this.pendingDispatchIds.clear();
  }

  /** Stop interval only, keep seen IDs — call from ngOnDestroy to avoid re-notifying on re-mount */
  pauseDrvReminder() {
    if (this.drvReminderId) { clearInterval(this.drvReminderId); this.drvReminderId = null; }
  }
}
