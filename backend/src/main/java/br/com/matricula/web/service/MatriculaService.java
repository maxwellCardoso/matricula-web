package br.com.matricula.web.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.matricula.web.domain.Aluno;
import br.com.matricula.web.domain.Matricula;
import br.com.matricula.web.domain.StatusMatricula;
import br.com.matricula.web.domain.StatusTurma;
import br.com.matricula.web.domain.Turma;
import br.com.matricula.web.dto.request.MatriculaRequestDTO;
import br.com.matricula.web.dto.response.MatriculaResponseDTO;
import br.com.matricula.web.dto.response.PageResponseDTO;
import br.com.matricula.web.exception.EntidadeNaoEncontradaException;
import br.com.matricula.web.exception.MatriculaDuplicadaException;
import br.com.matricula.web.exception.MatriculaJaCanceladaException;
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

		Matricula matricula = matriculaRepository
				.findByAlunoIdAndTurmaId(matriculaRequestDTO.alunoId(), matriculaRequestDTO.turmaId())
				.orElseGet(() -> {
					Matricula nova = new Matricula();
					nova.setAlunoId(aluno.getId());
					nova.setTurmaId(turma.getId());
					return nova;
				});

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

	@Transactional
	public MatriculaResponseDTO cancelar(Long id) {
		Matricula matricula = buscarMatricula(id);
		validarMatriculaNaoCancelada(matricula);

		if (matricula.getStatus() == StatusMatricula.CONFIRMADA) {
			int linhasAfetadas = turmaRepository.decrementarVagasOcupadas(matricula.getTurmaId());
			if (linhasAfetadas == 0) {
				throw new RecursoDuplicadoException(
						"INCONSISTENCIA_VAGAS",
						"Não foi possível liberar a vaga desta matrícula.");
			}
		}

		matricula.setStatus(StatusMatricula.CANCELADA);
		matriculaRepository.save(matricula);

		return matriculaRepository.findDetalhadaById(id)
				.orElseThrow(() -> new EntidadeNaoEncontradaException(
						"Matrícula não encontrada com id: " + id));
	}

	@Transactional(readOnly = true)
	public PageResponseDTO<MatriculaResponseDTO> listar(Long alunoId, Long turmaId, Pageable pageable) {
		return PageResponseDTO.from(matriculaRepository.findDetalhadas(alunoId, turmaId, pageable));
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

	private void validarMatriculaNaoCancelada(Matricula matricula) {
		if (matricula.getStatus() == StatusMatricula.CANCELADA) {
			throw new MatriculaJaCanceladaException(
					"A matrícula já está cancelada.");
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
