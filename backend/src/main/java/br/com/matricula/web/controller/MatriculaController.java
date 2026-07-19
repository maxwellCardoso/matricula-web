package br.com.matricula.web.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.matricula.web.dto.request.MatriculaRequestDTO;
import br.com.matricula.web.dto.response.MatriculaResponseDTO;
import br.com.matricula.web.dto.response.PageResponseDTO;
import br.com.matricula.web.service.MatriculaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/matriculas")
public class MatriculaController {

	private final MatriculaService matriculaService;

	public MatriculaController(MatriculaService matriculaService) {
		this.matriculaService = matriculaService;
	}

	@PostMapping
	public ResponseEntity<MatriculaResponseDTO> matricular(
			@Valid @RequestBody MatriculaRequestDTO matriculaRequestDTO) {
		MatriculaResponseDTO matriculaNova = matriculaService.matricular(matriculaRequestDTO);
		return ResponseEntity.status(HttpStatus.CREATED).body(matriculaNova);
	}

	@PatchMapping("/{id}/confirmar")
	public ResponseEntity<MatriculaResponseDTO> confirmar(@PathVariable Long id) {
		return ResponseEntity.ok(matriculaService.confirmar(id));
	}

	@PatchMapping("/{id}/cancelar")
	public ResponseEntity<MatriculaResponseDTO> cancelar(@PathVariable Long id) {
		return ResponseEntity.ok(matriculaService.cancelar(id));
	}

	@GetMapping
	public ResponseEntity<PageResponseDTO<MatriculaResponseDTO>> listar(
			@RequestParam(required = false) Long alunoId,
			@RequestParam(required = false) Long turmaId,
			@ParameterObject
			@PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
		return ResponseEntity.ok(matriculaService.listar(alunoId, turmaId, pageable));
	}
}
