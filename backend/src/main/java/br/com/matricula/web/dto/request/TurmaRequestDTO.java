package br.com.matricula.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TurmaRequestDTO(
		@NotBlank(message = "é obrigatório")
		@Size(max = 50, message = "deve ter no máximo 50 caracteres")
		String codTurma,

		@NotNull(message = "é obrigatório")
		Long disciplinaId,

		@NotNull(message = "é obrigatório")
		@Positive(message = "deve ser maior que zero")
		Integer vagasTotais
) {
}
