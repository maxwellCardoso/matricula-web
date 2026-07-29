import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { ApiError } from '../../../core/models/api-error.model';
import { AlunoService } from '../../../core/services/aluno.service';
import { NotificationService } from '../../../core/services/notification.service';
import { applyServerValidationErrors } from '../../../core/utils/form-error.util';
import { LoadingComponent } from '../../../shared/components/loading/loading';
import { AlunoRequest } from '../aluno.model';

@Component({
  selector: 'app-aluno-form',
  imports: [ReactiveFormsModule, RouterLink, LoadingComponent],
  templateUrl: './aluno-form.html',
  styleUrl: './aluno-form.scss',
})
export class AlunoForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly alunoService = inject(AlunoService);
  private readonly notificationService = inject(NotificationService);

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly isEdit = signal(false);

  private alunoId: number | null = null;

  readonly form = this.fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(255)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    cpf: ['', [Validators.required, Validators.pattern(/^\d{11}$/)]],
    endereco: ['', [Validators.maxLength(255)]],
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) {
      return;
    }

    const id = Number(idParam);
    if (Number.isNaN(id)) {
      this.notificationService.showError('Registro não encontrado.');
      void this.router.navigate(['/alunos']);
      return;
    }

    this.isEdit.set(true);
    this.alunoId = id;
    this.carregar(id);
  }

  onSubmit(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    const alunoRequest = this.mapToAlunoRequest();
    const request$ =
      this.isEdit() && this.alunoId !== null
        ? this.alunoService.atualizar(this.alunoId, alunoRequest)
        : this.alunoService.criar(alunoRequest);

    request$.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.notificationService.showSuccess(
          this.isEdit() ? 'Aluno atualizado com sucesso.' : 'Aluno cadastrado com sucesso.',
        );
        void this.router.navigate(['/alunos']);
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
    if (control.errors['email']) {
      return 'Informe um e-mail válido.';
    }
    if (control.errors['pattern']) {
      return 'CPF deve conter exatamente 11 dígitos numéricos.';
    }
    if (control.errors['maxlength']) {
      return 'Máximo de 255 caracteres.';
    }

    return null;
  }

  private carregar(id: number): void {
    this.loading.set(true);

    this.alunoService
      .buscarPorId(id)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (aluno) => {
          this.form.patchValue({
            nome: aluno.nome,
            email: aluno.email,
            cpf: aluno.cpf,
            endereco: aluno.endereco ?? '',
          });
        },
        error: () => {
          void this.router.navigate(['/alunos']);
        },
      });
  }

  private mapToAlunoRequest(): AlunoRequest {
    const { nome, email, cpf, endereco } = this.form.getRawValue();
    const trimmedEndereco = endereco.trim();

    return {
      nome: nome.trim(),
      email: email.trim(),
      cpf: cpf.trim(),
      ...(trimmedEndereco ? { endereco: trimmedEndereco } : {}),
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

    if (apiError.codigo === 'EMAIL_DUPLICADO') {
      this.form.controls.email.setErrors({ server: apiError.mensagemAmigavel });
      this.form.controls.email.markAsTouched();
      return;
    }

    if (apiError.codigo === 'CPF_DUPLICADO') {
      this.form.controls.cpf.setErrors({ server: apiError.mensagemAmigavel });
      this.form.controls.cpf.markAsTouched();
    }
  }
}
