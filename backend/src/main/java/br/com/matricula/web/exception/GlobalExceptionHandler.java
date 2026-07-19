package br.com.matricula.web.exception;

import java.time.Instant;
import java.util.List;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.matricula.web.dto.response.ErroResponseDTO;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(EntidadeNaoEncontradaException.class)
	public ResponseEntity<ErroResponseDTO> handleEntidadeNaoEncontrada(EntidadeNaoEncontradaException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(erro(HttpStatus.NOT_FOUND, ex.getCodigo(), ex.getMessage(), List.of()));
	}

	@ExceptionHandler(RecursoDuplicadoException.class)
	public ResponseEntity<ErroResponseDTO> handleRecursoDuplicado(RecursoDuplicadoException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(erro(HttpStatus.CONFLICT, ex.getCodigo(), ex.getMessage(), List.of()));
	}

	@ExceptionHandler(ExclusaoBloqueadaVinculoAtivoException.class)
	public ResponseEntity<ErroResponseDTO> handleExclusaoBloqueada(ExclusaoBloqueadaVinculoAtivoException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(erro(HttpStatus.CONFLICT, ex.getCodigo(), ex.getMessage(), List.of()));
	}

	@ExceptionHandler(TurmaFechadaException.class)
	public ResponseEntity<ErroResponseDTO> handleTurmaFechada(TurmaFechadaException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(erro(HttpStatus.CONFLICT, ex.getCodigo(), ex.getMessage(), List.of()));
	}

	@ExceptionHandler(MatriculaDuplicadaException.class)
	public ResponseEntity<ErroResponseDTO> handleMatriculaDuplicada(MatriculaDuplicadaException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(erro(HttpStatus.CONFLICT, ex.getCodigo(), ex.getMessage(), List.of()));
	}

	@ExceptionHandler(VagaIndisponivelException.class)
	public ResponseEntity<ErroResponseDTO> handleVagaIndisponivel(VagaIndisponivelException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(erro(HttpStatus.CONFLICT, ex.getCodigo(), ex.getMessage(), List.of()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErroResponseDTO> handleValidacao(MethodArgumentNotValidException ex) {
		List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.toList();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(erro(HttpStatus.BAD_REQUEST, "VALIDACAO_FALHOU", "Dados de entrada inválidos.", detalhes));
	}

	@ExceptionHandler(InvalidDataAccessApiUsageException.class)
	public ResponseEntity<ErroResponseDTO> handleSortInvalido(InvalidDataAccessApiUsageException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(erro(
						HttpStatus.BAD_REQUEST,
						"ORDENACAO_INVALIDA",
						"Parâmetro de ordenação inválido. Use um campo da entidade, ex: sort=nome ou sort=nome,asc.",
						List.of(ex.getMostSpecificCause().getMessage())));
	}

	private ErroResponseDTO erro(HttpStatus status, String codigo, String mensagem, List<String> detalhes) {
		return new ErroResponseDTO(Instant.now(), status.value(), codigo, mensagem, detalhes);
	}
}
