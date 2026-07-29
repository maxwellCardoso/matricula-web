import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Disciplina, DisciplinaRequest } from '../../features/disciplinas/disciplina.model';
import { PageParams, PageResponse } from '../models/page-response.model';
import { buildPageParams } from '../utils/http-params.util';

@Injectable({ providedIn: 'root' })
export class DisciplinaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/disciplinas`;

  listar(params: PageParams = {}): Observable<PageResponse<Disciplina>> {
    return this.http.get<PageResponse<Disciplina>>(this.baseUrl, {
      params: buildPageParams({
        page: params.page ?? 0,
        size: params.size ?? 10,
        sort: params.sort ?? 'nome,asc',
      }),
    });
  }

  buscarPorId(id: number): Observable<Disciplina> {
    return this.http.get<Disciplina>(`${this.baseUrl}/${id}`);
  }

  criar(request: DisciplinaRequest): Observable<Disciplina> {
    return this.http.post<Disciplina>(this.baseUrl, request);
  }

  atualizar(id: number, request: DisciplinaRequest): Observable<Disciplina> {
    return this.http.put<Disciplina>(`${this.baseUrl}/${id}`, request);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
