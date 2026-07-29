import { FormGroup } from '@angular/forms';

import { ErroResponse } from '../models/erro-response.model';

export function isErroResponse(body: unknown): body is ErroResponse {
  return (
    typeof body === 'object' &&
    body !== null &&
    'codigo' in body &&
    'mensagem' in body &&
    'detalhes' in body
  );
}

export function applyServerValidationErrors(form: FormGroup, detalhes: string[]): void {
  for (const detalhe of detalhes) {
    const colonIndex = detalhe.indexOf(':');
    if (colonIndex === -1) {
      continue;
    }

    const field = detalhe.slice(0, colonIndex).trim();
    const message = detalhe.slice(colonIndex + 1).trim();
    const control = form.get(field);

    if (control) {
      control.setErrors({ server: message });
      control.markAsTouched();
    }
  }
}
