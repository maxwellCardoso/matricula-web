import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';

import { ApiError } from '../../../core/models/api-error.model';
import { CursoService } from '../../../core/services/curso.service';
import { DisciplinaService } from '../../../core/services/disciplina.service';
import { NotificationService } from '../../../core/services/notification.service';
import { applyServerValidationErrors } from '../../../core/utils/form-error.util';
import { LoadingComponent } from '../../../shared/components/loading/loading';
import { Curso } from '../../cursos/curso.model';
import { DisciplinaRequest } from '../disciplina.model';

@Component({
  selector: 'app-disciplina-form',
  imports: [ReactiveFormsModule, RouterLink, LoadingComponent],
  templateUrl: './disciplina-form.html',
  styleUrl: './disciplina-form.scss',
})
export class DisciplinaForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly disciplinaService = inject(DisciplinaService);
  private readonly cursoService = inject(CursoService);
  private readonly notificationService = inject(NotificationService);

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly isEdit = signal(false);
  readonly cursos = signal<Curso[]>([]);

  private disciplinaId: number | null = null;

  readonly form = this.fb.group({
    codDisciplina: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(50)]),
    nome: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(255)]),
    cursoId: this.fb.control<number | null>(null, Validators.required),
    ano: this.fb.control<number | null>(null, Validators.required),
    periodo: this.fb.control<number | null>(null, Validators.required),
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) {
      this.carregarCursos();
      return;
    }

    const id = Number(idParam);
    if (Number.isNaN(id)) {
      this.notificationService.showError('Registro não encontrado.');
      void this.router.navigate(['/disciplinas']);
      return;
    }

    this.isEdit.set(true);
    this.disciplinaId = id;
    this.carregarParaEdicao(id);
  }

  onSubmit(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    const disciplinaRequest = this.mapToDisciplinaRequest();
    const request$ =
      this.isEdit() && this.disciplinaId !== null
        ? this.disciplinaService.atualizar(this.disciplinaId, disciplinaRequest)
        : this.disciplinaService.criar(disciplinaRequest);

    request$.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.notificationService.showSuccess(
          this.isEdit()
            ? 'Disciplina atualizada com sucesso.'
            : 'Disciplina cadastrada com sucesso.',
        );
        void this.router.navigate(['/disciplinas']);
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

  private carregarCursos(): void {
    this.loading.set(true);

    this.cursoService
      .listar({ size: 100, sort: 'nome,asc' })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (response) => {
          this.cursos.set(response.content);
        },
      });
  }

  private carregarParaEdicao(id: number): void {
    this.loading.set(true);

    forkJoin({
      cursos: this.cursoService.listar({ size: 100, sort: 'nome,asc' }),
      disciplina: this.disciplinaService.buscarPorId(id),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ cursos, disciplina }) => {
          this.cursos.set(cursos.content);
          this.form.patchValue({
            codDisciplina: disciplina.codDisciplina,
            nome: disciplina.nome,
            cursoId: disciplina.cursoId,
            ano: disciplina.ano,
            periodo: disciplina.periodo,
          });
        },
        error: () => {
          void this.router.navigate(['/disciplinas']);
        },
      });
  }

  private mapToDisciplinaRequest(): DisciplinaRequest {
    const { codDisciplina, nome, cursoId, ano, periodo } = this.form.getRawValue();

    return {
      codDisciplina: codDisciplina.trim(),
      nome: nome.trim(),
      cursoId: cursoId!,
      ano: ano!,
      periodo: periodo!,
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

    if (apiError.codigo === 'COD_DISCIPLINA_DUPLICADO') {
      this.form.controls.codDisciplina.setErrors({ server: apiError.mensagemAmigavel });
      this.form.controls.codDisciplina.markAsTouched();
    }
  }
}
