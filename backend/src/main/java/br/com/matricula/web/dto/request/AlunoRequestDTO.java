package br.com.matricula.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AlunoRequestDTO(
		@NotBlank(message = "é obrigatório")
		@Size(max = 255, message = "deve ter no máximo 255 caracteres")
		String nome,

		@NotBlank(message = "é obrigatório")
		@Email(message = "deve ser um e-mail válido")
		@Size(max = 255, message = "deve ter no máximo 255 caracteres")
		String email,

		@NotBlank(message = "é obrigatório")
		@Pattern(regexp = "\\d{11}", message = "deve conter exatamente 11 dígitos numéricos")
		String cpf,

		@Size(max = 255, message = "deve ter no máximo 255 caracteres")
		String endereco
) {
}
