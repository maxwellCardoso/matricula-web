package br.com.matricula.web.dto.request;

import jakarta.validation.constraints.NotNull;

public record MatriculaRequestDTO(
		@NotNull(message = "é obrigatório")
		Long alunoId,

		@NotNull(message = "é obrigatório")
		Long turmaId
) {
}
