package br.com.matricula.web.dto.response;

import br.com.matricula.web.domain.StatusTurma;

public record TurmaResponseDTO(
		Long id,
		String codTurma,
		Long disciplinaId,
		String codDisciplina,
		String nomeDisciplina,
		Integer vagasTotais,
		Integer vagasOcupadas,
		StatusTurma status
) {
}
