import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { ApiError } from '../../../core/models/api-error.model';
import { AlunoService } from '../../../core/services/aluno.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { LoadingComponent } from '../../../shared/components/loading/loading';
import { PaginationComponent } from '../../../shared/components/pagination/pagination';
import { Aluno } from '../aluno.model';

@Component({
  selector: 'app-aluno-list',
  imports: [RouterLink, LoadingComponent, PaginationComponent, ConfirmDialogComponent],
  templateUrl: './aluno-list.html',
  styleUrl: './aluno-list.scss',
})
export class AlunoList implements OnInit {
  private readonly alunoService = inject(AlunoService);
  private readonly notificationService = inject(NotificationService);

  readonly alunos = signal<Aluno[]>([]);
  readonly loading = signal(false);
  readonly page = signal(0);
  readonly size = signal(10);
  readonly totalPages = signal(0);
  readonly first = signal(true);
  readonly last = signal(true);

  readonly confirmOpen = signal(false);
  readonly alunoParaExcluir = signal<Aluno | null>(null);
  readonly excluindo = signal(false);

  readonly confirmMessage = computed(() => {
    const aluno = this.alunoParaExcluir();
    return aluno
      ? `Deseja excluir o aluno ${aluno.nome}?`
      : 'Deseja excluir este aluno?';
  });

  ngOnInit(): void {
    this.carregar();
  }

  onPageChange(page: number): void {
    this.page.set(page);
    this.carregar();
  }

  solicitarExclusao(aluno: Aluno): void {
    this.alunoParaExcluir.set(aluno);
    this.confirmOpen.set(true);
  }

  cancelarExclusao(): void {
    if (this.excluindo()) {
      return;
    }

    this.confirmOpen.set(false);
    this.alunoParaExcluir.set(null);
  }

  confirmarExclusao(): void {
    const aluno = this.alunoParaExcluir();
    if (!aluno || this.excluindo()) {
      return;
    }

    this.excluindo.set(true);

    this.alunoService
      .excluir(aluno.id)
      .pipe(finalize(() => this.excluindo.set(false)))
      .subscribe({
        next: () => {
          this.fecharDialogo();
          this.notificationService.showSuccess('Aluno excluído com sucesso.');
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
    this.alunoParaExcluir.set(null);
  }

  private recarregarAposExclusao(): void {
    const unicaNaPagina = this.alunos().length === 1;
    const paginaAtual = this.page();

    if (unicaNaPagina && paginaAtual > 0) {
      this.page.set(paginaAtual - 1);
    }

    this.carregar();
  }

  private carregar(): void {
    this.loading.set(true);

    this.alunoService
      .listar({
        page: this.page(),
        size: this.size(),
        sort: 'nome,asc',
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (response) => {
          this.alunos.set(response.content);
          this.page.set(response.page);
          this.size.set(response.size);
          this.totalPages.set(response.totalPages);
          this.first.set(response.first);
          this.last.set(response.last);
        },
      });
  }
}
