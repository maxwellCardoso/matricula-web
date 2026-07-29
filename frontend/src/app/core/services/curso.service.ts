import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Curso, CursoRequest } from '../../features/cursos/curso.model';
import { PageParams, PageResponse } from '../models/page-response.model';
import { buildPageParams } from '../utils/http-params.util';

@Injectable({ providedIn: 'root' })
export class CursoService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/cursos`;

  listar(params: PageParams = {}): Observable<PageResponse<Curso>> {
    return this.http.get<PageResponse<Curso>>(this.baseUrl, {
      params: buildPageParams({
        page: params.page ?? 0,
        size: params.size ?? 10,
        sort: params.sort ?? 'nome,asc',
      }),
    });
  }

  buscarPorId(id: number): Observable<Curso> {
    return this.http.get<Curso>(`${this.baseUrl}/${id}`);
  }

  criar(request: CursoRequest): Observable<Curso> {
    return this.http.post<Curso>(this.baseUrl, request);
  }

  atualizar(id: number, request: CursoRequest): Observable<Curso> {
    return this.http.put<Curso>(`${this.baseUrl}/${id}`, request);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
