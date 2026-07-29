import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-confirm-dialog',
  template: `
    @if (open()) {
      <div class="confirm-dialog" role="dialog" aria-modal="true" [attr.aria-label]="title()">
        <div class="confirm-dialog__backdrop" (click)="onCancel()"></div>
        <div class="confirm-dialog__panel">
          <h2 class="confirm-dialog__title">{{ title() }}</h2>
          <p class="confirm-dialog__message">{{ message() }}</p>
          <div class="confirm-dialog__actions">
            <button
              type="button"
              class="confirm-dialog__button confirm-dialog__button--secondary"
              [disabled]="busy()"
              (click)="onCancel()"
            >
              {{ cancelLabel() }}
            </button>
            <button
              type="button"
              class="confirm-dialog__button confirm-dialog__button--primary"
              [disabled]="busy()"
              (click)="confirm.emit()"
            >
              {{ busy() ? busyLabel() : confirmLabel() }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styles: `
    .confirm-dialog {
      position: fixed;
      inset: 0;
      z-index: 1000;
      display: grid;
      place-items: center;
    }

    .confirm-dialog__backdrop {
      position: absolute;
      inset: 0;
      background: rgba(15, 23, 42, 0.45);
    }

    .confirm-dialog__panel {
      position: relative;
      width: min(100% - 2rem, 28rem);
      background: #fff;
      border-radius: 0.5rem;
      padding: 1.5rem;
      box-shadow: 0 10px 25px rgba(15, 23, 42, 0.15);
    }

    .confirm-dialog__title {
      margin: 0 0 0.75rem;
      font-size: 1.125rem;
    }

    .confirm-dialog__message {
      margin: 0 0 1.5rem;
      color: #475569;
    }

    .confirm-dialog__actions {
      display: flex;
      justify-content: flex-end;
      gap: 0.75rem;
    }

    .confirm-dialog__button {
      padding: 0.5rem 0.875rem;
      border-radius: 0.375rem;
      border: 1px solid transparent;
      cursor: pointer;
    }

    .confirm-dialog__button:disabled {
      opacity: 0.65;
      cursor: not-allowed;
    }

    .confirm-dialog__button--secondary {
      background: #fff;
      border-color: #cbd5e1;
    }

    .confirm-dialog__button--primary {
      background: #dc2626;
      color: #fff;
    }
  `,
})
export class ConfirmDialogComponent {
  open = input(false);
  title = input('Confirmar ação');
  message = input('Deseja continuar?');
  confirmLabel = input('Confirmar');
  cancelLabel = input('Cancelar');
  busy = input(false);
  busyLabel = input('Aguarde...');

  confirm = output<void>();
  cancel = output<void>();

  onCancel(): void {
    if (!this.busy()) {
      this.cancel.emit();
    }
  }
}
