package br.com.matricula.web.dto.response;

public record AlunoResponseDTO(
		Long id,
		String nome,
		String email,
		String cpf,
		String endereco
) {
}
