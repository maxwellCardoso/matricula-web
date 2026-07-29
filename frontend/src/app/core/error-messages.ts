import { ErroResponse } from './models/erro-response.model';

export const ERROR_MESSAGES: Record<string, string> = {
  VALIDACAO_FALHOU: 'Dados de entrada inválidos.',
  ORDENACAO_INVALIDA: 'Ordenação inválida. Tente outra coluna.',
  ENTIDADE_NAO_ENCONTRADA: 'Registro não encontrado.',
  EMAIL_DUPLICADO: 'Já existe um aluno com este e-mail.',
  CPF_DUPLICADO: 'Já existe um aluno com este CPF.',
  COD_CURSO_DUPLICADO: 'Já existe um curso com este código.',
  COD_DISCIPLINA_DUPLICADO: 'Já existe uma disciplina com este código.',
  COD_TURMA_DUPLICADO: 'Já existe uma turma com este código.',
  TURMA_FECHADA: 'Esta turma está fechada para matrículas.',
  VAGA_INDISPONIVEL: 'Não há vagas disponíveis nesta turma.',
  MATRICULA_DUPLICADA: 'Este aluno já possui matrícula ativa nesta turma.',
  MATRICULA_JA_CANCELADA: 'Esta matrícula já foi cancelada.',
  MATRICULA_STATUS_INVALIDO: 'Somente matrículas pendentes podem ser confirmadas.',
  INCONSISTENCIA_VAGAS: 'Erro ao liberar vaga. Tente novamente.',
};

export function mapErrorMessage(erro: ErroResponse): string {
  if (erro.codigo === 'EXCLUSAO_BLOQUEADA_VINCULO_ATIVO') {
    return erro.mensagem;
  }

  return ERROR_MESSAGES[erro.codigo] ?? erro.mensagem;
}
