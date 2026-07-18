package br.com.matricula.web.dto.response;

public record DisciplinaResponseDTO(
		Long id,
		String codDisciplina,
		String nome,
		Long cursoId,
		String codCurso,
		String nomeCurso,
		Integer ano,
		Integer periodo
) {
}
