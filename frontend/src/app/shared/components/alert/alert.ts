import { Component, inject } from '@angular/core';

import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-alert',
  template: `
    @if (notificationService.alert(); as alert) {
      <div class="alert" [class]="'alert--' + alert.type" role="alert">
        <span>{{ alert.message }}</span>
        <button type="button" class="alert__close" (click)="notificationService.clear()" aria-label="Fechar">
          ×
        </button>
      </div>
    }
  `,
  styles: `
    .alert {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      padding: 0.75rem 1rem;
      border-radius: 0.375rem;
      border: 1px solid transparent;
    }

    .alert--error {
      background: #fef2f2;
      border-color: #fecaca;
      color: #991b1b;
    }

    .alert--success {
      background: #f0fdf4;
      border-color: #bbf7d0;
      color: #166534;
    }

    .alert--info {
      background: #eff6ff;
      border-color: #bfdbfe;
      color: #1e40af;
    }

    .alert__close {
      border: none;
      background: transparent;
      font-size: 1.25rem;
      line-height: 1;
      cursor: pointer;
      color: inherit;
    }
  `,
})
export class AlertComponent {
  protected readonly notificationService = inject(NotificationService);
}
