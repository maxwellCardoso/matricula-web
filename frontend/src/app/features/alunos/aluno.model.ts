export interface Aluno {
  id: number;
  nome: string;
  email: string;
  cpf: string;
  endereco: string | null;
}

export interface AlunoRequest {
  nome: string;
  email: string;
  cpf: string;
  endereco?: string;
}
