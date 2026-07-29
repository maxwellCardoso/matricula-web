import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { ApiError } from '../../../core/models/api-error.model';
import { CursoService } from '../../../core/services/curso.service';
import { NotificationService } from '../../../core/services/notification.service';
import { applyServerValidationErrors } from '../../../core/utils/form-error.util';
import { LoadingComponent } from '../../../shared/components/loading/loading';
import { CursoRequest } from '../curso.model';

@Component({
  selector: 'app-curso-form',
  imports: [ReactiveFormsModule, RouterLink, LoadingComponent],
  templateUrl: './curso-form.html',
  styleUrl: './curso-form.scss',
})
export class CursoForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly cursoService = inject(CursoService);
  private readonly notificationService = inject(NotificationService);

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly isEdit = signal(false);

  private cursoId: number | null = null;

  readonly form = this.fb.nonNullable.group({
    codCurso: ['', [Validators.required, Validators.maxLength(50)]],
    nome: ['', [Validators.required, Validators.maxLength(255)]],
    descricao: ['', [Validators.maxLength(255)]],
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) {
      return;
    }

    const id = Number(idParam);
    if (Number.isNaN(id)) {
      this.notificationService.showError('Registro não encontrado.');
      void this.router.navigate(['/cursos']);
      return;
    }

    this.isEdit.set(true);
    this.cursoId = id;
    this.carregar(id);
  }

  onSubmit(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    const cursoRequest = this.mapToCursoRequest();
    const request$ =
      this.isEdit() && this.cursoId !== null
        ? this.cursoService.atualizar(this.cursoId, cursoRequest)
        : this.cursoService.criar(cursoRequest);

    request$.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.notificationService.showSuccess(
          this.isEdit() ? 'Curso atualizado com sucesso.' : 'Curso cadastrado com sucesso.',
        );
        void this.router.navigate(['/cursos']);
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
    if (control.errors['maxlength']) {
      const maxLength = control.errors['maxlength'].requiredLength;
      return `Máximo de ${maxLength} caracteres.`;
    }

    return null;
  }

  private carregar(id: number): void {
    this.loading.set(true);

    this.cursoService
      .buscarPorId(id)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (curso) => {
          this.form.patchValue({
            codCurso: curso.codCurso,
            nome: curso.nome,
            descricao: curso.descricao ?? '',
          });
        },
        error: () => {
          void this.router.navigate(['/cursos']);
        },
      });
  }

  private mapToCursoRequest(): CursoRequest {
    const { codCurso, nome, descricao } = this.form.getRawValue();
    const trimmedDescricao = descricao.trim();

    return {
      codCurso: codCurso.trim(),
      nome: nome.trim(),
      ...(trimmedDescricao ? { descricao: trimmedDescricao } : {}),
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

    if (apiError.codigo === 'COD_CURSO_DUPLICADO') {
      this.form.controls.codCurso.setErrors({ server: apiError.mensagemAmigavel });
      this.form.controls.codCurso.markAsTouched();
    }
  }
}
