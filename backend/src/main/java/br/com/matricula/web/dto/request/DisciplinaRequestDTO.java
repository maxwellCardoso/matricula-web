package br.com.matricula.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DisciplinaRequestDTO(
		@NotBlank(message = "é obrigatório")
		@Size(max = 50, message = "deve ter no máximo 50 caracteres")
		String codDisciplina,

		@NotBlank(message = "é obrigatório")
		@Size(max = 255, message = "deve ter no máximo 255 caracteres")
		String nome,

		@NotNull(message = "é obrigatório")
		Long cursoId,

		@NotNull(message = "é obrigatório")
		Integer ano,

		@NotNull(message = "é obrigatório")
		Integer periodo
) {
}
