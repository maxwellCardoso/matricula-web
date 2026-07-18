package br.com.matricula.web.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.matricula.web.dto.request.TurmaRequestDTO;
import br.com.matricula.web.dto.response.PageResponseDTO;
import br.com.matricula.web.dto.response.TurmaResponseDTO;
import br.com.matricula.web.service.TurmaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/turmas")
public class TurmaController {

	private final TurmaService turmaService;

	public TurmaController(TurmaService turmaService) {
		this.turmaService = turmaService;
	}

	@PostMapping
	public ResponseEntity<TurmaResponseDTO> criar(@Valid @RequestBody TurmaRequestDTO turmaRequestDTO) {
		TurmaResponseDTO turmaNova = turmaService.criar(turmaRequestDTO);
		return ResponseEntity.status(HttpStatus.CREATED).body(turmaNova);
	}

	@GetMapping
	public ResponseEntity<PageResponseDTO<TurmaResponseDTO>> listar(
			@ParameterObject
			@PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
		return ResponseEntity.ok(turmaService.listar(pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<TurmaResponseDTO> buscarPorId(@PathVariable Long id) {
		return ResponseEntity.ok(turmaService.buscarPorId(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<TurmaResponseDTO> atualizar(
			@PathVariable Long id,
			@Valid @RequestBody TurmaRequestDTO turmaRequestDTO) {
		return ResponseEntity.ok(turmaService.atualizar(id, turmaRequestDTO));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> remover(@PathVariable Long id) {
		turmaService.remover(id);
		return ResponseEntity.noContent().build();
	}
}
