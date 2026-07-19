package br.com.matricula.web.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.matricula.web.domain.Curso;
import br.com.matricula.web.domain.Disciplina;
import br.com.matricula.web.dto.request.DisciplinaRequestDTO;
import br.com.matricula.web.dto.response.DisciplinaResponseDTO;
import br.com.matricula.web.dto.response.PageResponseDTO;
import br.com.matricula.web.exception.EntidadeNaoEncontradaException;
import br.com.matricula.web.exception.ExclusaoBloqueadaVinculoAtivoException;
import br.com.matricula.web.exception.RecursoDuplicadoException;
import br.com.matricula.web.repository.CursoRepository;
import br.com.matricula.web.repository.DisciplinaRepository;
import br.com.matricula.web.repository.TurmaRepository;

@Service
public class DisciplinaService {

	private final DisciplinaRepository disciplinaRepository;
	private final CursoRepository cursoRepository;
	private final TurmaRepository turmaRepository;

	public DisciplinaService(
			DisciplinaRepository disciplinaRepository,
			CursoRepository cursoRepository,
			TurmaRepository turmaRepository) {
		this.disciplinaRepository = disciplinaRepository;
		this.cursoRepository = cursoRepository;
		this.turmaRepository = turmaRepository;
	}

	@Transactional
	public DisciplinaResponseDTO criar(DisciplinaRequestDTO disciplinaRequestDTO) {
		Curso curso = buscarCurso(disciplinaRequestDTO.cursoId());
		validarUnicidadeCodigo(disciplinaRequestDTO.codDisciplina(), null);

		Disciplina disciplina = new Disciplina();
		aplicarDados(disciplina, disciplinaRequestDTO);
		return mapToResponseDTO(disciplinaRepository.save(disciplina), curso);
	}

	@Transactional(readOnly = true)
	public PageResponseDTO<DisciplinaResponseDTO> listar(Pageable pageable) {
		return PageResponseDTO.from(disciplinaRepository.findDetalhadas(pageable));
	}

	@Transactional(readOnly = true)
	public DisciplinaResponseDTO buscarPorId(Long id) {
		return disciplinaRepository.findDetalhadaById(id)
				.orElseThrow(() -> new EntidadeNaoEncontradaException(
						"Disciplina não encontrada com id: " + id));
	}

	@Transactional
	public DisciplinaResponseDTO atualizar(Long id, DisciplinaRequestDTO disciplinaRequestDTO) {
		Disciplina disciplina = buscarDisciplina(id);
		Curso curso = buscarCurso(disciplinaRequestDTO.cursoId());
		validarUnicidadeCodigo(disciplinaRequestDTO.codDisciplina(), id);

		aplicarDados(disciplina, disciplinaRequestDTO);
		return mapToResponseDTO(disciplinaRepository.save(disciplina), curso);
	}

	@Transactional
	public void remover(Long id) {
		Disciplina disciplina = buscarDisciplina(id);

		if (turmaRepository.existsByDisciplinaId(id)) {
			throw new ExclusaoBloqueadaVinculoAtivoException(
					"Não é possível excluir a disciplina pois existem turmas vinculadas.");
		}

		disciplinaRepository.delete(disciplina);
	}

	private Disciplina buscarDisciplina(Long id) {
		return disciplinaRepository.findById(id)
				.orElseThrow(() -> new EntidadeNaoEncontradaException(
						"Disciplina não encontrada com id: " + id));
	}

	private Curso buscarCurso(Long cursoId) {
		return cursoRepository.findById(cursoId)
				.orElseThrow(() -> new EntidadeNaoEncontradaException(
						"Curso não encontrado com id: " + cursoId));
	}

	private void validarUnicidadeCodigo(String codDisciplina, Long idAtual) {
		boolean duplicado = idAtual == null
				? disciplinaRepository.existsByCodDisciplina(codDisciplina)
				: disciplinaRepository.existsByCodDisciplinaAndIdNot(codDisciplina, idAtual);

		if (duplicado) {
			throw new RecursoDuplicadoException(
					"COD_DISCIPLINA_DUPLICADO",
					"Já existe uma disciplina cadastrada com o código informado.");
		}
	}

	private void aplicarDados(Disciplina disciplina, DisciplinaRequestDTO disciplinaRequestDTO) {
		disciplina.setCodDisciplina(disciplinaRequestDTO.codDisciplina());
		disciplina.setNome(disciplinaRequestDTO.nome());
		disciplina.setCursoId(disciplinaRequestDTO.cursoId());
		disciplina.setAno(disciplinaRequestDTO.ano());
		disciplina.setPeriodo(disciplinaRequestDTO.periodo());
	}

	private DisciplinaResponseDTO mapToResponseDTO(Disciplina disciplina, Curso curso) {
		return new DisciplinaResponseDTO(
				disciplina.getId(),
				disciplina.getCodDisciplina(),
				disciplina.getNome(),
				disciplina.getCursoId(),
				curso.getCodCurso(),
				curso.getNome(),
				disciplina.getAno(),
				disciplina.getPeriodo());
	}
}
