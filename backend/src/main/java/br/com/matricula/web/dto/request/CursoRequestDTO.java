package br.com.matricula.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CursoRequestDTO(
		@NotBlank(message = "é obrigatório")
		@Size(max = 50, message = "deve ter no máximo 50 caracteres")
		String codCurso,

		@NotBlank(message = "é obrigatório")
		@Size(max = 255, message = "deve ter no máximo 255 caracteres")
		String nome,

		@Size(max = 255, message = "deve ter no máximo 255 caracteres")
		String descricao
) {
}
