import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Turma, TurmaRequest } from '../../features/turmas/turma.model';
import { PageParams, PageResponse } from '../models/page-response.model';
import { buildPageParams } from '../utils/http-params.util';

@Injectable({ providedIn: 'root' })
export class TurmaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/turmas`;

  listar(params: PageParams = {}): Observable<PageResponse<Turma>> {
    return this.http.get<PageResponse<Turma>>(this.baseUrl, {
      params: buildPageParams({
        page: params.page ?? 0,
        size: params.size ?? 10,
        sort: params.sort ?? 'id,asc',
      }),
    });
  }

  buscarPorId(id: number): Observable<Turma> {
    return this.http.get<Turma>(`${this.baseUrl}/${id}`);
  }

  criar(request: TurmaRequest): Observable<Turma> {
    return this.http.post<Turma>(this.baseUrl, request);
  }

  atualizar(id: number, request: TurmaRequest): Observable<Turma> {
    return this.http.put<Turma>(`${this.baseUrl}/${id}`, request);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
