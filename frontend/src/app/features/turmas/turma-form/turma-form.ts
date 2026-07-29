import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';

import { ApiError } from '../../../core/models/api-error.model';
import { DisciplinaService } from '../../../core/services/disciplina.service';
import { NotificationService } from '../../../core/services/notification.service';
import { TurmaService } from '../../../core/services/turma.service';
import { applyServerValidationErrors } from '../../../core/utils/form-error.util';
import { LoadingComponent } from '../../../shared/components/loading/loading';
import { Disciplina } from '../../disciplinas/disciplina.model';
import { TurmaRequest } from '../turma.model';

@Component({
  selector: 'app-turma-form',
  imports: [ReactiveFormsModule, RouterLink, LoadingComponent],
  templateUrl: './turma-form.html',
  styleUrl: './turma-form.scss',
})
export class TurmaForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly turmaService = inject(TurmaService);
  private readonly disciplinaService = inject(DisciplinaService);
  private readonly notificationService = inject(NotificationService);

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly isEdit = signal(false);
  readonly disciplinas = signal<Disciplina[]>([]);

  private turmaId: number | null = null;

  readonly form = this.fb.group({
    codTurma: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(50)]),
    disciplinaId: this.fb.control<number | null>(null, Validators.required),
    vagasTotais: this.fb.control<number | null>(null, [Validators.required, Validators.min(1)]),
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) {
      this.carregarDisciplinas();
      return;
    }

    const id = Number(idParam);
    if (Number.isNaN(id)) {
      this.notificationService.showError('Registro não encontrado.');
      void this.router.navigate(['/turmas']);
      return;
    }

    this.isEdit.set(true);
    this.turmaId = id;
    this.carregarParaEdicao(id);
  }

  onSubmit(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    const turmaRequest = this.mapToTurmaRequest();
    const request$ =
      this.isEdit() && this.turmaId !== null
        ? this.turmaService.atualizar(this.turmaId, turmaRequest)
        : this.turmaService.criar(turmaRequest);

    request$.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.notificationService.showSuccess(
          this.isEdit() ? 'Turma atualizada com sucesso.' : 'Turma cadastrada com sucesso.',
        );
        void this.router.navigate(['/turmas']);
      },
      error: (error: ApiError) => this.handleSubmitError(error),
    });
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
    if (control.errors['min']) {
      return 'Deve ser maior que zero.';
    }
    if (control.errors['maxlength']) {
      const maxLength = control.errors['maxlength'].requiredLength;
      return `Máximo de ${maxLength} caracteres.`;
    }

    return null;
  }

  private carregarDisciplinas(): void {
    this.loading.set(true);

    this.disciplinaService
      .listar({ size: 100, sort: 'nome,asc' })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (response) => {
          this.disciplinas.set(response.content);
        },
      });
  }

  private carregarParaEdicao(id: number): void {
    this.loading.set(true);

    forkJoin({
      disciplinas: this.disciplinaService.listar({ size: 100, sort: 'nome,asc' }),
      turma: this.turmaService.buscarPorId(id),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ disciplinas, turma }) => {
          this.disciplinas.set(disciplinas.content);
          this.form.patchValue({
            codTurma: turma.codTurma,
            disciplinaId: turma.disciplinaId,
            vagasTotais: turma.vagasTotais,
          });
        },
        error: () => {
          void this.router.navigate(['/turmas']);
        },
      });
  }

  private mapToTurmaRequest(): TurmaRequest {
    const { codTurma, disciplinaId, vagasTotais } = this.form.getRawValue();

    return {
      codTurma: codTurma.trim(),
      disciplinaId: disciplinaId!,
      vagasTotais: vagasTotais!,
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

    if (apiError.codigo === 'COD_TURMA_DUPLICADO') {
      this.form.controls.codTurma.setErrors({ server: apiError.mensagemAmigavel });
      this.form.controls.codTurma.markAsTouched();
    }
  }
}
