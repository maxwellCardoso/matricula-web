import { Component, computed, input, output } from '@angular/core';

@Component({
  selector: 'app-pagination',
  template: `
    <nav class="pagination" aria-label="Paginação">
      <button type="button" [disabled]="first()" (click)="pageChange.emit(page() - 1)">
        Anterior
      </button>
      <span class="pagination__info">Página {{ page() + 1 }} de {{ totalPages() || 1 }}</span>
      <button type="button" [disabled]="last()" (click)="pageChange.emit(page() + 1)">
        Próxima
      </button>
    </nav>
  `,
  styles: `
    .pagination {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 1rem 0;
    }

    .pagination button {
      padding: 0.5rem 0.75rem;
      border: 1px solid #cbd5e1;
      border-radius: 0.375rem;
      background: #fff;
      cursor: pointer;
    }

    .pagination button:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .pagination__info {
      color: #475569;
    }
  `,
})
export class PaginationComponent {
  page = input(0);
  totalPages = input(0);
  first = input(true);
  last = input(true);

  readonly currentPageLabel = computed(() => this.page() + 1);

  pageChange = output<number>();
}
