import { Routes } from '@angular/router';

import { AlunoForm } from './features/alunos/aluno-form/aluno-form';
import { AlunoList } from './features/alunos/aluno-list/aluno-list';
import { CursoForm } from './features/cursos/curso-form/curso-form';
import { CursoList } from './features/cursos/curso-list/curso-list';
import { DisciplinaForm } from './features/disciplinas/disciplina-form/disciplina-form';
import { DisciplinaList } from './features/disciplinas/disciplina-list/disciplina-list';
import { MatriculaForm } from './features/matriculas/matricula-form/matricula-form';
import { MatriculaList } from './features/matriculas/matricula-list/matricula-list';
import { TurmaForm } from './features/turmas/turma-form/turma-form';
import { TurmaList } from './features/turmas/turma-list/turma-list';

export const routes: Routes = [
  { path: '', redirectTo: 'alunos', pathMatch: 'full' },
  { path: 'alunos', component: AlunoList },
  { path: 'alunos/novo', component: AlunoForm },
  { path: 'alunos/:id/editar', component: AlunoForm },
  { path: 'alunos/:id/matriculas', component: MatriculaList, data: { context: 'aluno' } },
  { path: 'cursos', component: CursoList },
  { path: 'cursos/novo', component: CursoForm },
  { path: 'cursos/:id/editar', component: CursoForm },
  { path: 'disciplinas', component: DisciplinaList },
  { path: 'disciplinas/novo', component: DisciplinaForm },
  { path: 'disciplinas/:id/editar', component: DisciplinaForm },
  { path: 'turmas', component: TurmaList },
  { path: 'turmas/novo', component: TurmaForm },
  { path: 'turmas/:id/editar', component: TurmaForm },
  { path: 'turmas/:id/matriculas', component: MatriculaList, data: { context: 'turma' } },
  { path: 'matriculas/novo', component: MatriculaForm },
  { path: 'matriculas', component: MatriculaList, data: { context: 'all' } },
];
