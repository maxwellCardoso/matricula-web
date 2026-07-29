import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { ApiError } from '../../../core/models/api-error.model';
import { NotificationService } from '../../../core/services/notification.service';
import { TurmaService } from '../../../core/services/turma.service';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { LoadingComponent } from '../../../shared/components/loading/loading';
import { PaginationComponent } from '../../../shared/components/pagination/pagination';
import { Turma } from '../turma.model';

@Component({
  selector: 'app-turma-list',
  imports: [RouterLink, LoadingComponent, PaginationComponent, ConfirmDialogComponent],
  templateUrl: './turma-list.html',
  styleUrl: './turma-list.scss',
})
export class TurmaList implements OnInit {
  private readonly turmaService = inject(TurmaService);
  private readonly notificationService = inject(NotificationService);

  readonly turmas = signal<Turma[]>([]);
  readonly loading = signal(false);
  readonly page = signal(0);
  readonly size = signal(10);
  readonly totalPages = signal(0);
  readonly first = signal(true);
  readonly last = signal(true);

  readonly confirmOpen = signal(false);
  readonly turmaParaExcluir = signal<Turma | null>(null);
  readonly excluindo = signal(false);

  readonly confirmMessage = computed(() => {
    const turma = this.turmaParaExcluir();
    return turma ? `Deseja excluir a turma ${turma.codTurma}?` : 'Deseja excluir esta turma?';
  });

  ngOnInit(): void {
    this.carregar();
  }

  onPageChange(page: number): void {
    this.page.set(page);
    this.carregar();
  }

  vagasDisponiveis(turma: Turma): number {
    return turma.vagasTotais - turma.vagasOcupadas;
  }

  solicitarExclusao(turma: Turma): void {
    this.turmaParaExcluir.set(turma);
    this.confirmOpen.set(true);
  }

  cancelarExclusao(): void {
    if (this.excluindo()) {
      return;
    }

    this.confirmOpen.set(false);
    this.turmaParaExcluir.set(null);
  }

  confirmarExclusao(): void {
    const turma = this.turmaParaExcluir();
    if (!turma || this.excluindo()) {
      return;
    }

    this.excluindo.set(true);

    this.turmaService
      .excluir(turma.id)
      .pipe(finalize(() => this.excluindo.set(false)))
      .subscribe({
        next: () => {
          this.fecharDialogo();
          this.notificationService.showSuccess('Turma excluída com sucesso.');
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
    this.turmaParaExcluir.set(null);
  }

  private recarregarAposExclusao(): void {
    const unicaNaPagina = this.turmas().length === 1;
    const paginaAtual = this.page();

    if (unicaNaPagina && paginaAtual > 0) {
      this.page.set(paginaAtual - 1);
    }

    this.carregar();
  }

  private carregar(): void {
    this.loading.set(true);

    this.turmaService
      .listar({
        page: this.page(),
        size: this.size(),
        sort: 'id,asc',
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (response) => {
          this.turmas.set(response.content);
          this.page.set(response.page);
          this.size.set(response.size);
          this.totalPages.set(response.totalPages);
          this.first.set(response.first);
          this.last.set(response.last);
        },
      });
  }
}
