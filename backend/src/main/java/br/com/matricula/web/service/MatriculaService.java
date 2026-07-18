package br.com.matricula.web.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.matricula.web.domain.Aluno;
import br.com.matricula.web.domain.Matricula;
import br.com.matricula.web.domain.Turma;
import br.com.matricula.web.dto.response.MatriculaResponseDTO;
import br.com.matricula.web.exception.EntidadeNaoEncontradaException;
import br.com.matricula.web.repository.AlunoRepository;
import br.com.matricula.web.repository.MatriculaRepository;
import br.com.matricula.web.repository.TurmaRepository;

@Service
public class MatriculaService {

	private final MatriculaRepository matriculaRepository;
	private final AlunoRepository alunoRepository;
	private final TurmaRepository turmaRepository;

	public MatriculaService(
			MatriculaRepository matriculaRepository,
			AlunoRepository alunoRepository,
			TurmaRepository turmaRepository) {
		this.matriculaRepository = matriculaRepository;
		this.alunoRepository = alunoRepository;
		this.turmaRepository = turmaRepository;
	}

	@Transactional(readOnly = true)
	public List<MatriculaResponseDTO> listar(Long alunoId, Long turmaId) {
		List<Matricula> matriculas = buscarMatriculas(alunoId, turmaId);
		Map<Long, Aluno> alunosPorId = carregarAlunos(matriculas);
		Map<Long, Turma> turmasPorId = carregarTurmas(matriculas);

		return matriculas.stream()
				.map(matricula -> {
					Aluno aluno = alunosPorId.get(matricula.getAlunoId());
					if (aluno == null) {
						throw new EntidadeNaoEncontradaException(
								"Aluno não encontrado com id: " + matricula.getAlunoId());
					}
					Turma turma = turmasPorId.get(matricula.getTurmaId());
					if (turma == null) {
						throw new EntidadeNaoEncontradaException(
								"Turma não encontrada com id: " + matricula.getTurmaId());
					}
					return mapToResponseDTO(matricula, aluno, turma);
				})
				.toList();
	}

	private List<Matricula> buscarMatriculas(Long alunoId, Long turmaId) {
		if (alunoId != null && turmaId != null) {
			return matriculaRepository.findByAlunoIdAndTurmaId(alunoId, turmaId);
		}
		if (alunoId != null) {
			return matriculaRepository.findByAlunoId(alunoId);
		}
		if (turmaId != null) {
			return matriculaRepository.findByTurmaId(turmaId);
		}
		return matriculaRepository.findAll();
	}

	private Map<Long, Aluno> carregarAlunos(List<Matricula> matriculas) {
		Set<Long> alunoIds = matriculas.stream()
				.map(Matricula::getAlunoId)
				.collect(Collectors.toSet());

		return alunoRepository.findAllById(alunoIds).stream()
				.collect(Collectors.toMap(Aluno::getId, Function.identity()));
	}

	private Map<Long, Turma> carregarTurmas(List<Matricula> matriculas) {
		Set<Long> turmaIds = matriculas.stream()
				.map(Matricula::getTurmaId)
				.collect(Collectors.toSet());

		return turmaRepository.findAllById(turmaIds).stream()
				.collect(Collectors.toMap(Turma::getId, Function.identity()));
	}

	private MatriculaResponseDTO mapToResponseDTO(Matricula matricula, Aluno aluno, Turma turma) {
		return new MatriculaResponseDTO(
				matricula.getId(),
				matricula.getAlunoId(),
				aluno.getNome(),
				aluno.getEmail(),
				aluno.getCpf(),
				matricula.getTurmaId(),
				turma.getCodTurma(),
				matricula.getStatus(),
				matricula.getDataCriacao(),
				matricula.getDataAtualizacao());
	}
}
