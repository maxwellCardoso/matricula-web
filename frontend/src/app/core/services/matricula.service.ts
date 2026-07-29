import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Matricula, MatriculaRequest } from '../../features/matriculas/matricula.model';
import { PageParams, PageResponse } from '../models/page-response.model';
import { buildPageParams } from '../utils/http-params.util';

export interface MatriculaListParams extends PageParams {
  alunoId?: number;
  turmaId?: number;
}

@Injectable({ providedIn: 'root' })
export class MatriculaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/matriculas`;

  listar(params: MatriculaListParams = {}): Observable<PageResponse<Matricula>> {
    let httpParams = buildPageParams({
      page: params.page ?? 0,
      size: params.size ?? 10,
      sort: params.sort ?? 'id,asc',
    });

    if (params.alunoId !== undefined) {
      httpParams = httpParams.set('alunoId', params.alunoId);
    }

    if (params.turmaId !== undefined) {
      httpParams = httpParams.set('turmaId', params.turmaId);
    }

    return this.http.get<PageResponse<Matricula>>(this.baseUrl, { params: httpParams });
  }

  matricular(request: MatriculaRequest): Observable<Matricula> {
    return this.http.post<Matricula>(this.baseUrl, request);
  }

  confirmar(id: number): Observable<Matricula> {
    return this.http.patch<Matricula>(`${this.baseUrl}/${id}/confirmar`, null);
  }

  cancelar(id: number): Observable<Matricula> {
    return this.http.patch<Matricula>(`${this.baseUrl}/${id}/cancelar`, null);
  }
}
