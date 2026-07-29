export type StatusTurma = 'ABERTA' | 'FECHADA';

export interface Turma {
  id: number;
  codTurma: string;
  disciplinaId: number;
  codDisciplina: string;
  nomeDisciplina: string;
  vagasTotais: number;
  vagasOcupadas: number;
  status: StatusTurma;
}

export interface TurmaRequest {
  codTurma: string;
  disciplinaId: number;
  vagasTotais: number;
}
