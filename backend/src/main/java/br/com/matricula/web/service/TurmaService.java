package br.com.matricula.web.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.matricula.web.domain.Disciplina;
import br.com.matricula.web.domain.StatusTurma;
import br.com.matricula.web.domain.Turma;
import br.com.matricula.web.dto.request.TurmaRequestDTO;
import br.com.matricula.web.dto.response.PageResponseDTO;
import br.com.matricula.web.dto.response.TurmaResponseDTO;
import br.com.matricula.web.exception.EntidadeNaoEncontradaException;
import br.com.matricula.web.exception.ExclusaoBloqueadaVinculoAtivoException;
import br.com.matricula.web.exception.RecursoDuplicadoException;
import br.com.matricula.web.repository.DisciplinaRepository;
import br.com.matricula.web.repository.MatriculaRepository;
import br.com.matricula.web.repository.TurmaRepository;

@Service
public class TurmaService {

	private final TurmaRepository turmaRepository;
	private final DisciplinaRepository disciplinaRepository;
	private final MatriculaRepository matriculaRepository;

	public TurmaService(
			TurmaRepository turmaRepository,
			DisciplinaRepository disciplinaRepository,
			MatriculaRepository matriculaRepository) {
		this.turmaRepository = turmaRepository;
		this.disciplinaRepository = disciplinaRepository;
		this.matriculaRepository = matriculaRepository;
	}

	@Transactional
	public TurmaResponseDTO criar(TurmaRequestDTO turmaRequestDTO) {
		Disciplina disciplina = buscarDisciplina(turmaRequestDTO.disciplinaId());
		validarUnicidadeCodigo(turmaRequestDTO.codTurma(), null);

		Turma turma = new Turma();
		turma.setCodTurma(turmaRequestDTO.codTurma());
		turma.setDisciplinaId(turmaRequestDTO.disciplinaId());
		turma.setVagasTotais(turmaRequestDTO.vagasTotais());
		turma.setVagasOcupadas(0);
		turma.setStatus(StatusTurma.ABERTA);

		return mapToResponseDTO(turmaRepository.save(turma), disciplina);
	}

	@Transactional(readOnly = true)
	public PageResponseDTO<TurmaResponseDTO> listar(Pageable pageable) {
		return PageResponseDTO.from(turmaRepository.findDetalhadas(pageable));
	}

	@Transactional(readOnly = true)
	public TurmaResponseDTO buscarPorId(Long id) {
		return turmaRepository.findDetalhadaById(id)
				.orElseThrow(() -> new EntidadeNaoEncontradaException(
						"Turma não encontrada com id: " + id));
	}

	@Transactional
	public TurmaResponseDTO atualizar(Long id, TurmaRequestDTO turmaRequestDTO) {
		Turma turma = buscarTurma(id);
		Disciplina disciplina = buscarDisciplina(turmaRequestDTO.disciplinaId());
		validarUnicidadeCodigo(turmaRequestDTO.codTurma(), id);

		turma.setCodTurma(turmaRequestDTO.codTurma());
		turma.setDisciplinaId(turmaRequestDTO.disciplinaId());
		turma.setVagasTotais(turmaRequestDTO.vagasTotais());

		return mapToResponseDTO(turmaRepository.save(turma), disciplina);
	}

	@Transactional
	public void remover(Long id) {
		Turma turma = buscarTurma(id);

		if (matriculaRepository.existsByTurmaId(id)) {
			throw new ExclusaoBloqueadaVinculoAtivoException(
					"Não é possível excluir a turma pois existem matrículas vinculadas.");
		}

		turmaRepository.delete(turma);
	}

	private Turma buscarTurma(Long id) {
		return turmaRepository.findById(id)
				.orElseThrow(() -> new EntidadeNaoEncontradaException(
						"Turma não encontrada com id: " + id));
	}

	private Disciplina buscarDisciplina(Long disciplinaId) {
		return disciplinaRepository.findById(disciplinaId)
				.orElseThrow(() -> new EntidadeNaoEncontradaException(
						"Disciplina não encontrada com id: " + disciplinaId));
	}

	private void validarUnicidadeCodigo(String codTurma, Long idAtual) {
		boolean duplicado = idAtual == null
				? turmaRepository.existsByCodTurma(codTurma)
				: turmaRepository.existsByCodTurmaAndIdNot(codTurma, idAtual);

		if (duplicado) {
			throw new RecursoDuplicadoException(
					"COD_TURMA_DUPLICADO",
					"Já existe uma turma cadastrada com o código informado.");
		}
	}

	private TurmaResponseDTO mapToResponseDTO(Turma turma, Disciplina disciplina) {
		return new TurmaResponseDTO(
				turma.getId(),
				turma.getCodTurma(),
				turma.getDisciplinaId(),
				disciplina.getCodDisciplina(),
				disciplina.getNome(),
				turma.getVagasTotais(),
				turma.getVagasOcupadas(),
				turma.getStatus());
	}
}
