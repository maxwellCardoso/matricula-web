import { Component, input } from '@angular/core';

@Component({
  selector: 'app-loading',
  template: `
    <div class="loading" role="status" aria-live="polite">
      <span class="loading__spinner" aria-hidden="true"></span>
      @if (message()) {
        <span class="loading__message">{{ message() }}</span>
      }
    </div>
  `,
  styles: `
    .loading {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 1rem;
      justify-content: center;
    }

    .loading__spinner {
      width: 1.5rem;
      height: 1.5rem;
      border: 2px solid #cbd5e1;
      border-top-color: #2563eb;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }

    @keyframes spin {
      to {
        transform: rotate(360deg);
      }
    }
  `,
})
export class LoadingComponent {
  message = input('Carregando...');
}
