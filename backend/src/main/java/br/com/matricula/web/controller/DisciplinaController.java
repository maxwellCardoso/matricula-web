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

import br.com.matricula.web.dto.request.DisciplinaRequestDTO;
import br.com.matricula.web.dto.response.DisciplinaResponseDTO;
import br.com.matricula.web.dto.response.PageResponseDTO;
import br.com.matricula.web.service.DisciplinaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/disciplinas")
public class DisciplinaController {

	private final DisciplinaService disciplinaService;

	public DisciplinaController(DisciplinaService disciplinaService) {
		this.disciplinaService = disciplinaService;
	}

	@PostMapping
	public ResponseEntity<DisciplinaResponseDTO> criar(
			@Valid @RequestBody DisciplinaRequestDTO disciplinaRequestDTO) {
		DisciplinaResponseDTO disciplinaNova = disciplinaService.criar(disciplinaRequestDTO);
		return ResponseEntity.status(HttpStatus.CREATED).body(disciplinaNova);
	}

	@GetMapping
	public ResponseEntity<PageResponseDTO<DisciplinaResponseDTO>> listar(
			@ParameterObject
			@PageableDefault(size = 10, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
		return ResponseEntity.ok(disciplinaService.listar(pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<DisciplinaResponseDTO> buscarPorId(@PathVariable Long id) {
		return ResponseEntity.ok(disciplinaService.buscarPorId(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<DisciplinaResponseDTO> atualizar(
			@PathVariable Long id,
			@Valid @RequestBody DisciplinaRequestDTO disciplinaRequestDTO) {
		return ResponseEntity.ok(disciplinaService.atualizar(id, disciplinaRequestDTO));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> remover(@PathVariable Long id) {
		disciplinaService.remover(id);
		return ResponseEntity.noContent().build();
	}
}
