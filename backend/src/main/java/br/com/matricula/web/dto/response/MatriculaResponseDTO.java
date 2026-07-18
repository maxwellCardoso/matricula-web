package br.com.matricula.web.dto.response;

import java.time.LocalDateTime;

import br.com.matricula.web.domain.StatusMatricula;

public record MatriculaResponseDTO(
		Long id,
		Long alunoId,
		String nomeAluno,
		String emailAluno,
		String cpfAluno,
		Long turmaId,
		String codTurma,
		StatusMatricula status,
		LocalDateTime dataCriacao,
		LocalDateTime dataAtualizacao
) {
}
