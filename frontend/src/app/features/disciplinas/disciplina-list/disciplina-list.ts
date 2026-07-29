import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { ApiError } from '../../../core/models/api-error.model';
import { DisciplinaService } from '../../../core/services/disciplina.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { LoadingComponent } from '../../../shared/components/loading/loading';
import { PaginationComponent } from '../../../shared/components/pagination/pagination';
import { Disciplina } from '../disciplina.model';

@Component({
  selector: 'app-disciplina-list',
  imports: [RouterLink, LoadingComponent, PaginationComponent, ConfirmDialogComponent],
  templateUrl: './disciplina-list.html',
  styleUrl: './disciplina-list.scss',
})
export class DisciplinaList implements OnInit {
  private readonly disciplinaService = inject(DisciplinaService);
  private readonly notificationService = inject(NotificationService);

  readonly disciplinas = signal<Disciplina[]>([]);
  readonly loading = signal(false);
  readonly page = signal(0);
  readonly size = signal(10);
  readonly totalPages = signal(0);
  readonly first = signal(true);
  readonly last = signal(true);

  readonly confirmOpen = signal(false);
  readonly disciplinaParaExcluir = signal<Disciplina | null>(null);
  readonly excluindo = signal(false);

  readonly confirmMessage = computed(() => {
    const disciplina = this.disciplinaParaExcluir();
    return disciplina
      ? `Deseja excluir a disciplina ${disciplina.nome}?`
      : 'Deseja excluir esta disciplina?';
  });

  ngOnInit(): void {
    this.carregar();
  }

  onPageChange(page: number): void {
    this.page.set(page);
    this.carregar();
  }

  solicitarExclusao(disciplina: Disciplina): void {
    this.disciplinaParaExcluir.set(disciplina);
    this.confirmOpen.set(true);
  }

  cancelarExclusao(): void {
    if (this.excluindo()) {
      return;
    }

    this.confirmOpen.set(false);
    this.disciplinaParaExcluir.set(null);
  }

  confirmarExclusao(): void {
    const disciplina = this.disciplinaParaExcluir();
    if (!disciplina || this.excluindo()) {
      return;
    }

    this.excluindo.set(true);

    this.disciplinaService
      .excluir(disciplina.id)
      .pipe(finalize(() => this.excluindo.set(false)))
      .subscribe({
        next: () => {
          this.fecharDialogo();
          this.notificationService.showSuccess('Disciplina excluída com sucesso.');
          this.recarregarAposExclusao();
        },
        error: (error: ApiError) => {
          this.fecharDialogo();

          const apiError = error.apiError;
          if (apiError?.codigo === 'EXCLUSAO_BLOQUEADA_VINCULO_ATIVO') {
            this.notificationService.showError(apiError.mensagemAmigavel);
          }

          this.carregar();
        },
      });
  }

  private fecharDialogo(): void {
    this.confirmOpen.set(false);
    this.disciplinaParaExcluir.set(null);
  }

  private recarregarAposExclusao(): void {
    const unicaNaPagina = this.disciplinas().length === 1;
    const paginaAtual = this.page();

    if (unicaNaPagina && paginaAtual > 0) {
      this.page.set(paginaAtual - 1);
    }

    this.carregar();
  }

  private carregar(): void {
    this.loading.set(true);

    this.disciplinaService
      .listar({
        page: this.page(),
        size: this.size(),
        sort: 'nome,asc',
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (response) => {
          this.disciplinas.set(response.content);
          this.page.set(response.page);
          this.size.set(response.size);
          this.totalPages.set(response.totalPages);
          this.first.set(response.first);
          this.last.set(response.last);
        },
      });
  }
}
