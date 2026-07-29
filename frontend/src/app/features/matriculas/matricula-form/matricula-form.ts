import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';

import { ApiError } from '../../../core/models/api-error.model';
import { AlunoService } from '../../../core/services/aluno.service';
import { MatriculaService } from '../../../core/services/matricula.service';
import { NotificationService } from '../../../core/services/notification.service';
import { TurmaService } from '../../../core/services/turma.service';
import { applyServerValidationErrors } from '../../../core/utils/form-error.util';
import { LoadingComponent } from '../../../shared/components/loading/loading';
import { Aluno } from '../../alunos/aluno.model';
import { Turma } from '../../turmas/turma.model';
import { MatriculaRequest } from '../matricula.model';

const ERROS_NEGOCIO_MATRICULA = [
  'TURMA_FECHADA',
  'VAGA_INDISPONIVEL',
  'MATRICULA_DUPLICADA',
] as const;

@Component({
  selector: 'app-matricula-form',
  imports: [ReactiveFormsModule, RouterLink, LoadingComponent],
  templateUrl: './matricula-form.html',
  styleUrl: './matricula-form.scss',
})
export class MatriculaForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly matriculaService = inject(MatriculaService);
  private readonly alunoService = inject(AlunoService);
  private readonly turmaService = inject(TurmaService);
  private readonly notificationService = inject(NotificationService);

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly businessError = signal<string | null>(null);
  readonly alunos = signal<Aluno[]>([]);
  readonly turmas = signal<Turma[]>([]);

  readonly form = this.fb.group({
    alunoId: this.fb.control<number | null>(null, Validators.required),
    turmaId: this.fb.control<number | null>(null, Validators.required),
  });

  ngOnInit(): void {
    this.carregarOpcoes();
  }

  onSubmit(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    this.businessError.set(null);
    this.saving.set(true);

    this.matriculaService
      .matricular(this.mapToMatriculaRequest())
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => {
          this.notificationService.showSuccess('Matrícula solicitada com sucesso.');
          void this.router.navigate(['/matriculas']);
        },
        error: (error: ApiError) => this.handleSubmitError(error),
      });
  }

  turmaLabel(turma: Turma): string {
    const status = turma.status === 'ABERTA' ? 'Aberta' : 'Fechada';
    return `${turma.codTurma} — ${turma.nomeDisciplina} — ${turma.vagasOcupadas}/${turma.vagasTotais} — ${status}`;
  }

  fieldError(field: keyof typeof this.form.controls): string | null {
    const control = this.form.controls[field];
    if (!control.touched || !control.errors) {
      return null;
    }

    if (control.errors['server']) {
      return control.errors['server'] as string;
    }
    if (control.errors['required']) {
      return 'Campo obrigatório.';
    }

    return null;
  }

  private carregarOpcoes(): void {
    this.loading.set(true);

    forkJoin({
      alunos: this.alunoService.listar({ size: 100, sort: 'nome,asc' }),
      turmas: this.turmaService.listar({ size: 100, sort: 'id,asc' }),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ alunos, turmas }) => {
          this.alunos.set(alunos.content);
          this.turmas.set(turmas.content);
        },
      });
  }

  private mapToMatriculaRequest(): MatriculaRequest {
    const { alunoId, turmaId } = this.form.getRawValue();

    return {
      alunoId: alunoId!,
      turmaId: turmaId!,
    };
  }

  private handleSubmitError(error: ApiError): void {
    const apiError = error.apiError;
    if (!apiError) {
      return;
    }

    if (apiError.codigo === 'VALIDACAO_FALHOU' && apiError.detalhes.length > 0) {
      applyServerValidationErrors(this.form, apiError.detalhes);
      return;
    }

    if (ERROS_NEGOCIO_MATRICULA.includes(apiError.codigo as (typeof ERROS_NEGOCIO_MATRICULA)[number])) {
      this.businessError.set(apiError.mensagemAmigavel);
    }
  }
}
