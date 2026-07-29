import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { ApiError } from '../../../core/models/api-error.model';
import { AlunoService } from '../../../core/services/aluno.service';
import { MatriculaService } from '../../../core/services/matricula.service';
import { NotificationService } from '../../../core/services/notification.service';
import { TurmaService } from '../../../core/services/turma.service';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { LoadingComponent } from '../../../shared/components/loading/loading';
import { PaginationComponent } from '../../../shared/components/pagination/pagination';
import { Aluno } from '../../alunos/aluno.model';
import { Turma } from '../../turmas/turma.model';
import { Matricula } from '../matricula.model';

type ListContext = 'all' | 'aluno' | 'turma';

@Component({
  selector: 'app-matricula-list',
  imports: [RouterLink, DatePipe, LoadingComponent, PaginationComponent, ConfirmDialogComponent],
  templateUrl: './matricula-list.html',
  styleUrl: './matricula-list.scss',
})
export class MatriculaList implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly matriculaService = inject(MatriculaService);
  private readonly alunoService = inject(AlunoService);
  private readonly turmaService = inject(TurmaService);
  private readonly notificationService = inject(NotificationService);

  readonly context = signal<ListContext>('all');
  readonly fixedAlunoId = signal<number | null>(null);
  readonly fixedTurmaId = signal<number | null>(null);

  readonly alunoHeader = signal<Aluno | null>(null);
  readonly turmaHeader = signal<Turma | null>(null);

  readonly filtroAlunoId = signal<number | null>(null);
  readonly filtroTurmaId = signal<number | null>(null);
  readonly alunosFiltro = signal<Aluno[]>([]);
  readonly turmasFiltro = signal<Turma[]>([]);

  readonly matriculas = signal<Matricula[]>([]);
  readonly loading = signal(false);
  readonly page = signal(0);
  readonly size = signal(10);
  readonly totalPages = signal(0);
  readonly first = signal(true);
  readonly last = signal(true);

  readonly confirmDialogOpen = signal(false);
  readonly cancelDialogOpen = signal(false);
  readonly matriculaAcao = signal<Matricula | null>(null);
  readonly processando = signal(false);

  readonly confirmMessage = computed(() => {
    const matricula = this.matriculaAcao();
    return matricula
      ? `Confirmar matrícula de ${matricula.nomeAluno} na turma ${matricula.codTurma}?`
      : '';
  });

  readonly cancelMessage = computed(() => {
    const matricula = this.matriculaAcao();
    return matricula
      ? `Cancelar matrícula de ${matricula.nomeAluno} na turma ${matricula.codTurma}?`
      : '';
  });

  ngOnInit(): void {
    const context = (this.route.snapshot.data['context'] as ListContext | undefined) ?? 'all';
    this.context.set(context);

    if (context === 'aluno') {
      this.inicializarContextoAluno();
      return;
    }

    if (context === 'turma') {
      this.inicializarContextoTurma();
      return;
    }

    this.carregarOpcoesFiltro();
    this.carregar();
  }

  onPageChange(page: number): void {
    this.page.set(page);
    this.carregar();
  }

  onFiltroAlunoChange(value: string): void {
    this.filtroAlunoId.set(value ? Number(value) : null);
    this.page.set(0);
    this.carregar();
  }

  onFiltroTurmaChange(value: string): void {
    this.filtroTurmaId.set(value ? Number(value) : null);
    this.page.set(0);
    this.carregar();
  }

  limparFiltros(): void {
    this.filtroAlunoId.set(null);
    this.filtroTurmaId.set(null);
    this.page.set(0);
    this.carregar();
  }

  podeConfirmar(matricula: Matricula): boolean {
    return matricula.status === 'PENDENTE';
  }

  podeCancelar(matricula: Matricula): boolean {
    return matricula.status === 'PENDENTE' || matricula.status === 'CONFIRMADA';
  }

  vagasDisponiveis(turma: Turma): number {
    return turma.vagasTotais - turma.vagasOcupadas;
  }

  solicitarConfirmacao(matricula: Matricula): void {
    this.matriculaAcao.set(matricula);
    this.confirmDialogOpen.set(true);
  }

  solicitarCancelamento(matricula: Matricula): void {
    this.matriculaAcao.set(matricula);
    this.cancelDialogOpen.set(true);
  }

  fecharConfirmacao(): void {
    this.confirmDialogOpen.set(false);
    this.matriculaAcao.set(null);
  }

  fecharCancelamento(): void {
    this.cancelDialogOpen.set(false);
    this.matriculaAcao.set(null);
  }

  confirmarMatricula(): void {
    const matricula = this.matriculaAcao();
    if (!matricula || this.processando()) {
      return;
    }

    this.processando.set(true);

    this.matriculaService
      .confirmar(matricula.id)
      .pipe(finalize(() => this.processando.set(false)))
      .subscribe({
        next: () => {
          this.fecharConfirmacao();
          this.notificationService.showSuccess('Matrícula confirmada com sucesso.');
          this.carregar();
        },
        error: (error: ApiError) => {
          this.fecharConfirmacao();
          this.exibirErroNegocio(error);
          this.carregar();
        },
      });
  }

  cancelarMatricula(): void {
    const matricula = this.matriculaAcao();
    if (!matricula || this.processando()) {
      return;
    }

    this.processando.set(true);

    this.matriculaService
      .cancelar(matricula.id)
      .pipe(finalize(() => this.processando.set(false)))
      .subscribe({
        next: () => {
          this.fecharCancelamento();
          this.notificationService.showSuccess('Matrícula cancelada com sucesso.');
          this.carregar();
        },
        error: (error: ApiError) => {
          this.fecharCancelamento();
          this.exibirErroNegocio(error);
          this.carregar();
        },
      });
  }

  private inicializarContextoAluno(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (Number.isNaN(id)) {
      this.notificationService.showError('Registro não encontrado.');
      void this.router.navigate(['/alunos']);
      return;
    }

    this.fixedAlunoId.set(id);
    this.loading.set(true);

    this.alunoService.buscarPorId(id).subscribe({
      next: (aluno) => {
        this.alunoHeader.set(aluno);
        this.carregar();
      },
      error: () => {
        this.loading.set(false);
        void this.router.navigate(['/alunos']);
      },
    });
  }

  private inicializarContextoTurma(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (Number.isNaN(id)) {
      this.notificationService.showError('Registro não encontrado.');
      void this.router.navigate(['/turmas']);
      return;
    }

    this.fixedTurmaId.set(id);
    this.loading.set(true);

    this.turmaService.buscarPorId(id).subscribe({
      next: (turma) => {
        this.turmaHeader.set(turma);
        this.carregar();
      },
      error: () => {
        this.loading.set(false);
        void this.router.navigate(['/turmas']);
      },
    });
  }

  private carregarOpcoesFiltro(): void {
    this.alunoService.listar({ size: 100, sort: 'nome,asc' }).subscribe({
      next: (response) => this.alunosFiltro.set(response.content),
    });

    this.turmaService.listar({ size: 100, sort: 'id,asc' }).subscribe({
      next: (response) => this.turmasFiltro.set(response.content),
    });
  }

  private carregar(): void {
    this.loading.set(true);

    const alunoId = this.fixedAlunoId() ?? this.filtroAlunoId();
    const turmaId = this.fixedTurmaId() ?? this.filtroTurmaId();

    this.matriculaService
      .listar({
        page: this.page(),
        size: this.size(),
        sort: 'id,asc',
        ...(alunoId !== null ? { alunoId } : {}),
        ...(turmaId !== null ? { turmaId } : {}),
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (response) => {
          this.matriculas.set(response.content);
          this.page.set(response.page);
          this.size.set(response.size);
          this.totalPages.set(response.totalPages);
          this.first.set(response.first);
          this.last.set(response.last);
        },
      });
  }

  private exibirErroNegocio(error: ApiError): void {
    const apiError = error.apiError;
    if (apiError) {
      this.notificationService.showError(apiError.mensagemAmigavel);
    }
  }
}
