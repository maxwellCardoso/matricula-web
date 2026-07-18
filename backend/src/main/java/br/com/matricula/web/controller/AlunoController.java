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

import br.com.matricula.web.dto.request.AlunoRequestDTO;
import br.com.matricula.web.dto.response.AlunoResponseDTO;
import br.com.matricula.web.dto.response.PageResponseDTO;
import br.com.matricula.web.service.AlunoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/alunos")
public class AlunoController {

	private final AlunoService alunoService;

	public AlunoController(AlunoService alunoService) {
		this.alunoService = alunoService;
	}

	@PostMapping
	public ResponseEntity<AlunoResponseDTO> criar(@Valid @RequestBody AlunoRequestDTO alunoRequestDTO) {
		AlunoResponseDTO alunoNovo = alunoService.criar(alunoRequestDTO);
		return ResponseEntity.status(HttpStatus.CREATED).body(alunoNovo);
	}

	@GetMapping
	public ResponseEntity<PageResponseDTO<AlunoResponseDTO>> listar(
			@ParameterObject
			@PageableDefault(size = 10, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
		return ResponseEntity.ok(alunoService.listar(pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<AlunoResponseDTO> buscarPorId(@PathVariable Long id) {
		return ResponseEntity.ok(alunoService.buscarPorId(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<AlunoResponseDTO> atualizar(
			@PathVariable Long id,
			@Valid @RequestBody AlunoRequestDTO alunoRequestDTO) {
		return ResponseEntity.ok(alunoService.atualizar(id, alunoRequestDTO));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> remover(@PathVariable Long id) {
		alunoService.remover(id);
		return ResponseEntity.noContent().build();
	}
}
