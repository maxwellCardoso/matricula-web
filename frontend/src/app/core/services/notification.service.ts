import { Injectable, signal } from '@angular/core';

export type AlertType = 'success' | 'error' | 'info';

export interface AlertMessage {
  type: AlertType;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private static readonly ALERT_DISMISS_MS = 3000;

  private readonly alertSignal = signal<AlertMessage | null>(null);
  private dismissTimeout: ReturnType<typeof setTimeout> | null = null;

  readonly alert = this.alertSignal.asReadonly();

  showError(message: string): void {
    this.show({ type: 'error', message }, NotificationService.ALERT_DISMISS_MS);
  }

  showSuccess(message: string): void {
    this.show({ type: 'success', message }, NotificationService.ALERT_DISMISS_MS);
  }

  showInfo(message: string): void {
    this.show({ type: 'info', message });
  }

  clear(): void {
    this.clearDismissTimeout();
    this.alertSignal.set(null);
  }

  private show(alert: AlertMessage, autoDismissMs?: number): void {
    this.clearDismissTimeout();
    this.alertSignal.set(alert);

    if (autoDismissMs !== undefined) {
      this.dismissTimeout = setTimeout(() => this.clear(), autoDismissMs);
    }
  }

  private clearDismissTimeout(): void {
    if (this.dismissTimeout !== null) {
      clearTimeout(this.dismissTimeout);
      this.dismissTimeout = null;
    }
  }
}
