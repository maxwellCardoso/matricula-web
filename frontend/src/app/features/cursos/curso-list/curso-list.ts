import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { ApiError } from '../../../core/models/api-error.model';
import { CursoService } from '../../../core/services/curso.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { LoadingComponent } from '../../../shared/components/loading/loading';
import { PaginationComponent } from '../../../shared/components/pagination/pagination';
import { Curso } from '../curso.model';

@Component({
  selector: 'app-curso-list',
  imports: [RouterLink, LoadingComponent, PaginationComponent, ConfirmDialogComponent],
  templateUrl: './curso-list.html',
  styleUrl: './curso-list.scss',
})
export class CursoList implements OnInit {
  private readonly cursoService = inject(CursoService);
  private readonly notificationService = inject(NotificationService);

  readonly cursos = signal<Curso[]>([]);
  readonly loading = signal(false);
  readonly page = signal(0);
  readonly size = signal(10);
  readonly totalPages = signal(0);
  readonly first = signal(true);
  readonly last = signal(true);

  readonly confirmOpen = signal(false);
  readonly cursoParaExcluir = signal<Curso | null>(null);
  readonly excluindo = signal(false);

  readonly confirmMessage = computed(() => {
    const curso = this.cursoParaExcluir();
    return curso ? `Deseja excluir o curso ${curso.nome}?` : 'Deseja excluir este curso?';
  });

  ngOnInit(): void {
    this.carregar();
  }

  onPageChange(page: number): void {
    this.page.set(page);
    this.carregar();
  }

  solicitarExclusao(curso: Curso): void {
    this.cursoParaExcluir.set(curso);
    this.confirmOpen.set(true);
  }

  cancelarExclusao(): void {
    if (this.excluindo()) {
      return;
    }

    this.confirmOpen.set(false);
    this.cursoParaExcluir.set(null);
  }

  confirmarExclusao(): void {
    const curso = this.cursoParaExcluir();
    if (!curso || this.excluindo()) {
      return;
    }

    this.excluindo.set(true);

    this.cursoService
      .excluir(curso.id)
      .pipe(finalize(() => this.excluindo.set(false)))
      .subscribe({
        next: () => {
          this.fecharDialogo();
          this.notificationService.showSuccess('Curso excluído com sucesso.');
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
    this.cursoParaExcluir.set(null);
  }

  private recarregarAposExclusao(): void {
    const unicoNaPagina = this.cursos().length === 1;
    const paginaAtual = this.page();

    if (unicoNaPagina && paginaAtual > 0) {
      this.page.set(paginaAtual - 1);
    }

    this.carregar();
  }

  private carregar(): void {
    this.loading.set(true);

    this.cursoService
      .listar({
        page: this.page(),
        size: this.size(),
        sort: 'nome,asc',
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (response) => {
          this.cursos.set(response.content);
          this.page.set(response.page);
          this.size.set(response.size);
          this.totalPages.set(response.totalPages);
          this.first.set(response.first);
          this.last.set(response.last);
        },
      });
  }
}
