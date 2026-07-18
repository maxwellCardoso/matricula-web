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

import br.com.matricula.web.dto.request.CursoRequestDTO;
import br.com.matricula.web.dto.response.CursoResponseDTO;
import br.com.matricula.web.dto.response.PageResponseDTO;
import br.com.matricula.web.service.CursoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/cursos")
public class CursoController {

	private final CursoService cursoService;

	public CursoController(CursoService cursoService) {
		this.cursoService = cursoService;
	}

	@PostMapping
	public ResponseEntity<CursoResponseDTO> criar(@Valid @RequestBody CursoRequestDTO cursoRequestDTO) {
		CursoResponseDTO cursoNovo = cursoService.criar(cursoRequestDTO);
		return ResponseEntity.status(HttpStatus.CREATED).body(cursoNovo);
	}

	@GetMapping
	public ResponseEntity<PageResponseDTO<CursoResponseDTO>> listar(
			@ParameterObject
			@PageableDefault(size = 10, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
		return ResponseEntity.ok(cursoService.listar(pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<CursoResponseDTO> buscarPorId(@PathVariable Long id) {
		return ResponseEntity.ok(cursoService.buscarPorId(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<CursoResponseDTO> atualizar(
			@PathVariable Long id,
			@Valid @RequestBody CursoRequestDTO cursoRequestDTO) {
		return ResponseEntity.ok(cursoService.atualizar(id, cursoRequestDTO));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> remover(@PathVariable Long id) {
		cursoService.remover(id);
		return ResponseEntity.noContent().build();
	}
}
