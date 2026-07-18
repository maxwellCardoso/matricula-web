package br.com.matricula.web.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.matricula.web.domain.Curso;
import br.com.matricula.web.dto.request.CursoRequestDTO;
import br.com.matricula.web.dto.response.CursoResponseDTO;
import br.com.matricula.web.dto.response.PageResponseDTO;
import br.com.matricula.web.exception.EntidadeNaoEncontradaException;
import br.com.matricula.web.exception.ExclusaoBloqueadaVinculoAtivoException;
import br.com.matricula.web.exception.RecursoDuplicadoException;
import br.com.matricula.web.repository.CursoRepository;
import br.com.matricula.web.repository.DisciplinaRepository;

@Service
public class CursoService {

	private final CursoRepository cursoRepository;
	private final DisciplinaRepository disciplinaRepository;

	public CursoService(CursoRepository cursoRepository, DisciplinaRepository disciplinaRepository) {
		this.cursoRepository = cursoRepository;
		this.disciplinaRepository = disciplinaRepository;
	}

	@Transactional
	public CursoResponseDTO criar(CursoRequestDTO cursoRequestDTO) {
		validarUnicidadeCodigo(cursoRequestDTO.codCurso(), null);
		Curso curso = new Curso();
		aplicarDados(curso, cursoRequestDTO);
		return mapToResponseDTO(cursoRepository.save(curso));
	}

	@Transactional(readOnly = true)
	public PageResponseDTO<CursoResponseDTO> listar(Pageable pageable) {
		Page<CursoResponseDTO> page = cursoRepository.findAll(pageable)
				.map(this::mapToResponseDTO);
		return PageResponseDTO.from(page);
	}

	@Transactional(readOnly = true)
	public CursoResponseDTO buscarPorId(Long id) {
		return mapToResponseDTO(buscarCurso(id));
	}

	@Transactional
	public CursoResponseDTO atualizar(Long id, CursoRequestDTO cursoRequestDTO) {
		Curso curso = buscarCurso(id);
		validarUnicidadeCodigo(cursoRequestDTO.codCurso(), id);
		aplicarDados(curso, cursoRequestDTO);
		return mapToResponseDTO(cursoRepository.save(curso));
	}

	@Transactional
	public void remover(Long id) {
		Curso curso = buscarCurso(id);

		if (disciplinaRepository.existsByCursoId(id)) {
			throw new ExclusaoBloqueadaVinculoAtivoException(
					"Não é possível excluir o curso pois existem disciplinas vinculadas.");
		}

		cursoRepository.delete(curso);
	}

	private Curso buscarCurso(Long id) {
		return cursoRepository.findById(id)
				.orElseThrow(() -> new EntidadeNaoEncontradaException(
						"Curso não encontrado com id: " + id));
	}

	private void validarUnicidadeCodigo(String codCurso, Long idAtual) {
		boolean duplicado = idAtual == null
				? cursoRepository.existsByCodCurso(codCurso)
				: cursoRepository.existsByCodCursoAndIdNot(codCurso, idAtual);

		if (duplicado) {
			throw new RecursoDuplicadoException(
					"COD_CURSO_DUPLICADO",
					"Já existe um curso cadastrado com o código informado.");
		}
	}

	private void aplicarDados(Curso curso, CursoRequestDTO cursoRequestDTO) {
		curso.setCodCurso(cursoRequestDTO.codCurso());
		curso.setNome(cursoRequestDTO.nome());
		curso.setDescricao(cursoRequestDTO.descricao());
	}

	private CursoResponseDTO mapToResponseDTO(Curso curso) {
		return new CursoResponseDTO(
				curso.getId(),
				curso.getCodCurso(),
				curso.getNome(),
				curso.getDescricao());
	}
}
