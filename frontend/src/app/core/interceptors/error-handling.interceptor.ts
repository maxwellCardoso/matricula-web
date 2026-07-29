import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { mapErrorMessage } from '../error-messages';
import { ApiError } from '../models/api-error.model';
import { NotificationService } from '../services/notification.service';
import { isErroResponse } from '../utils/form-error.util';

export const errorHandlingInterceptor: HttpInterceptorFn = (req, next) => {
  const notificationService = inject(NotificationService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (!isErroResponse(error.error)) {
        notificationService.showError('Erro inesperado. Tente novamente.');
        return throwError(() => error);
      }

      const erro = error.error;
      const mensagemAmigavel = mapErrorMessage(erro);
      const apiError = error as ApiError;
      apiError.apiError = { ...erro, mensagemAmigavel };

      if (erro.codigo === 'VALIDACAO_FALHOU' && erro.detalhes.length > 0) {
        return throwError(() => apiError);
      }

      if (error.status === 409) {
        return throwError(() => apiError);
      }

      notificationService.showError(mensagemAmigavel);
      return throwError(() => apiError);
    }),
  );
};
