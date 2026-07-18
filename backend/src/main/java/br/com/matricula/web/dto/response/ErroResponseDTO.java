package br.com.matricula.web.dto.response;

import java.time.Instant;
import java.util.List;

public record ErroResponseDTO(
		Instant timestamp,
		int status,
		String codigo,
		String mensagem,
		List<String> detalhes
) {
}
