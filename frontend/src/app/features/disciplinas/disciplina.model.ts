export interface Disciplina {
  id: number;
  codDisciplina: string;
  nome: string;
  cursoId: number;
  codCurso: string;
  nomeCurso: string;
  ano: number;
  periodo: number;
}

export interface DisciplinaRequest {
  codDisciplina: string;
  nome: string;
  cursoId: number;
  ano: number;
  periodo: number;
}
