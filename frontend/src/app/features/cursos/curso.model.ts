export interface Curso {
  id: number;
  codCurso: string;
  nome: string;
  descricao: string | null;
}

export interface CursoRequest {
  codCurso: string;
  nome: string;
  descricao?: string;
}
