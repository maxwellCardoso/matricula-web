export type StatusMatricula = 'PENDENTE' | 'CONFIRMADA' | 'CANCELADA';

export interface Matricula {
  id: number;
  alunoId: number;
  nomeAluno: string;
  emailAluno: string;
  cpfAluno: string;
  turmaId: number;
  codTurma: string;
  status: StatusMatricula;
  dataCriacao: string;
  dataAtualizacao: string;
}

export interface MatriculaRequest {
  alunoId: number;
  turmaId: number;
}
