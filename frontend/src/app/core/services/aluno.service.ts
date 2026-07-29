import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Aluno, AlunoRequest } from '../../features/alunos/aluno.model';
import { PageParams, PageResponse } from '../models/page-response.model';
import { buildPageParams } from '../utils/http-params.util';

@Injectable({ providedIn: 'root' })
export class AlunoService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/alunos`;

  listar(params: PageParams = {}): Observable<PageResponse<Aluno>> {
    return this.http.get<PageResponse<Aluno>>(this.baseUrl, {
      params: buildPageParams({
        page: params.page ?? 0,
        size: params.size ?? 10,
        sort: params.sort ?? 'nome,asc',
      }),
    });
  }

  buscarPorId(id: number): Observable<Aluno> {
    return this.http.get<Aluno>(`${this.baseUrl}/${id}`);
  }

  criar(request: AlunoRequest): Observable<Aluno> {
    return this.http.post<Aluno>(this.baseUrl, request);
  }

  atualizar(id: number, request: AlunoRequest): Observable<Aluno> {
    return this.http.put<Aluno>(`${this.baseUrl}/${id}`, request);
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
