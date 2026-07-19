package br.com.matricula.web.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.matricula.web.domain.Aluno;
import br.com.matricula.web.domain.Matricula;
import br.com.matricula.web.domain.StatusMatricula;
import br.com.matricula.web.domain.StatusTurma;
import br.com.matricula.web.domain.Turma;
import br.com.matricula.web.dto.request.MatriculaRequestDTO;
import br.com.matricula.web.dto.response.MatriculaResponseDTO;
import br.com.matricula.web.exception.EntidadeNaoEncontradaException;
import br.com.matricula.web.exception.MatriculaDuplicadaException;
import br.com.matricula.web.exception.RecursoDuplicadoException;
import br.com.matricula.web.exception.TurmaFechadaException;
import br.com.matricula.web.exception.VagaIndisponivelException;
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

	@Transactional
	public MatriculaResponseDTO matricular(MatriculaRequestDTO matriculaRequestDTO) {
		Aluno aluno = buscarAluno(matriculaRequestDTO.alunoId());
		Turma turma = buscarTurma(matriculaRequestDTO.turmaId());

		validarTurmaAberta(turma);
		validarVagaDisponivel(turma);
		validarMatriculaDuplicada(matriculaRequestDTO.alunoId(), matriculaRequestDTO.turmaId());

		Matricula matricula = new Matricula();
		matricula.setAlunoId(aluno.getId());
		matricula.setTurmaId(turma.getId());
		matricula.setStatus(StatusMatricula.PENDENTE);

		return mapToResponseDTO(matriculaRepository.save(matricula), aluno, turma);
	}

	@Transactional
	public MatriculaResponseDTO confirmar(Long id) {
		Matricula matricula = buscarMatricula(id);
		validarMatriculaPendente(matricula);

		int linhasAfetadas = turmaRepository.incrementarVagasOcupadasSeDisponivel(matricula.getTurmaId());
		if (linhasAfetadas == 0) {
			throw new VagaIndisponivelException(
					"Não há vagas disponíveis nesta turma.");
		}

		matricula.setStatus(StatusMatricula.CONFIRMADA);
		matriculaRepository.save(matricula);

		return matriculaRepository.findDetalhadaById(id)
				.orElseThrow(() -> new EntidadeNaoEncontradaException(
						"Matrícula não encontrada com id: " + id));
	}

	@Transactional(readOnly = true)
	public List<MatriculaResponseDTO> listar(Long alunoId, Long turmaId) {
		return matriculaRepository.findDetalhadas(alunoId, turmaId);
	}

	private Matricula buscarMatricula(Long id) {
		return matriculaRepository.findById(id)
				.orElseThrow(() -> new EntidadeNaoEncontradaException(
						"Matrícula não encontrada com id: " + id));
	}

	private Aluno buscarAluno(Long alunoId) {
		return alunoRepository.findById(alunoId)
				.orElseThrow(() -> new EntidadeNaoEncontradaException(
						"Aluno não encontrado com id: " + alunoId));
	}

	private Turma buscarTurma(Long turmaId) {
		return turmaRepository.findById(turmaId)
				.orElseThrow(() -> new EntidadeNaoEncontradaException(
						"Turma não encontrada com id: " + turmaId));
	}

	private void validarTurmaAberta(Turma turma) {
		if (turma.getStatus() == StatusTurma.FECHADA) {
			throw new TurmaFechadaException(
					"Não é possível matricular em turma com status FECHADA.");
		}
	}

	private void validarVagaDisponivel(Turma turma) {
		if (turma.getVagasOcupadas() >= turma.getVagasTotais()) {
			throw new VagaIndisponivelException(
					"Não há vagas disponíveis nesta turma.");
		}
	}

	private void validarMatriculaDuplicada(Long alunoId, Long turmaId) {
		if (matriculaRepository.existsByAlunoIdAndTurmaIdAndStatusNot(
				alunoId, turmaId, StatusMatricula.CANCELADA)) {
			throw new MatriculaDuplicadaException(
					"O aluno já possui matrícula ativa nesta turma.");
		}
	}

	private void validarMatriculaPendente(Matricula matricula) {
		if (matricula.getStatus() != StatusMatricula.PENDENTE) {
			throw new RecursoDuplicadoException(
					"MATRICULA_STATUS_INVALIDO",
					"Somente matrículas com status PENDENTE podem ser confirmadas.");
		}
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
