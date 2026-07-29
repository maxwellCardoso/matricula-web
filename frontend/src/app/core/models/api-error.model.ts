import { HttpErrorResponse } from '@angular/common/http';

import { ErroResponse } from './erro-response.model';

export interface ApiError extends HttpErrorResponse {
  apiError?: ErroResponse & { mensagemAmigavel: string };
}
