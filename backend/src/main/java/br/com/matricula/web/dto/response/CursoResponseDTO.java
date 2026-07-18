package br.com.matricula.web.dto.response;

public record CursoResponseDTO(
		Long id,
		String codCurso,
		String nome,
		String descricao
) {
}
