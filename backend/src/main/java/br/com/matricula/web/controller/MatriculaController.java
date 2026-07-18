package br.com.matricula.web.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.matricula.web.dto.response.MatriculaResponseDTO;
import br.com.matricula.web.service.MatriculaService;

@RestController
@RequestMapping("/matriculas")
public class MatriculaController {

	private final MatriculaService matriculaService;

	public MatriculaController(MatriculaService matriculaService) {
		this.matriculaService = matriculaService;
	}

	@GetMapping
	public ResponseEntity<List<MatriculaResponseDTO>> listar(
			@RequestParam(required = false) Long alunoId,
			@RequestParam(required = false) Long turmaId) {
		return ResponseEntity.ok(matriculaService.listar(alunoId, turmaId));
	}
}
