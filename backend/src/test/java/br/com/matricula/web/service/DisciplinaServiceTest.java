package br.com.matricula.web.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import br.com.matricula.web.domain.Curso;
import br.com.matricula.web.domain.Disciplina;
import br.com.matricula.web.dto.request.DisciplinaRequestDTO;
import br.com.matricula.web.dto.response.DisciplinaResponseDTO;
import br.com.matricula.web.dto.response.PageResponseDTO;
import br.com.matricula.web.exception.EntidadeNaoEncontradaException;
import br.com.matricula.web.exception.ExclusaoBloqueadaVinculoAtivoException;
import br.com.matricula.web.repository.CursoRepository;
import br.com.matricula.web.repository.DisciplinaRepository;
import br.com.matricula.web.repository.TurmaRepository;

@ExtendWith(MockitoExtension.class)
class DisciplinaServiceTest {

	@Mock
	private DisciplinaRepository disciplinaRepository;

	@Mock
	private CursoRepository cursoRepository;

	@Mock
	private TurmaRepository turmaRepository;

	@InjectMocks
	private DisciplinaService disciplinaService;

	@Test
	void deveCriarDisciplina_quandoDadosValidos() {
		DisciplinaRequestDTO request = requestValido();
		Curso curso = cursoExistente(request.cursoId());
		when(cursoRepository.findById(request.cursoId())).thenReturn(Optional.of(curso));
		when(disciplinaRepository.existsByCodDisciplina(request.codDisciplina())).thenReturn(false);
		when(disciplinaRepository.save(any(Disciplina.class))).thenAnswer(invocation -> {
			Disciplina disciplina = invocation.getArgument(0);
			disciplina.setId(1L);
			return disciplina;
		});

		DisciplinaResponseDTO response = disciplinaService.criar(request);

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.codDisciplina()).isEqualTo(request.codDisciplina());
		assertThat(response.nome()).isEqualTo(request.nome());
		assertThat(response.cursoId()).isEqualTo(request.cursoId());
		assertThat(response.codCurso()).isEqualTo(curso.getCodCurso());
		assertThat(response.nomeCurso()).isEqualTo(curso.getNome());
		assertThat(response.ano()).isEqualTo(request.ano());
		assertThat(response.periodo()).isEqualTo(request.periodo());

		ArgumentCaptor<Disciplina> captor = ArgumentCaptor.forClass(Disciplina.class);
		verify(disciplinaRepository).save(captor.capture());
		assertThat(captor.getValue().getCodDisciplina()).isEqualTo(request.codDisciplina());
		assertThat(captor.getValue().getCursoId()).isEqualTo(request.cursoId());
	}

	@Test
	void deveLancarEntidadeNaoEncontrada_quandoCriarComCursoInexistente() {
		DisciplinaRequestDTO request = requestValido();
		when(cursoRepository.findById(request.cursoId())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> disciplinaService.criar(request))
				.isInstanceOf(EntidadeNaoEncontradaException.class)
				.hasMessageContaining(request.cursoId().toString());

		verify(disciplinaRepository, never()).save(any());
	}

	@Test
	void deveListarDisciplinasComPaginacaoEDadosDoCurso() {
		Disciplina disciplina = disciplinaExistente(1L);
		Curso curso = cursoExistente(disciplina.getCursoId());
		Pageable pageable = PageRequest.of(0, 10);
		Page<Disciplina> page = new PageImpl<>(List.of(disciplina), pageable, 1);
		when(disciplinaRepository.findAll(pageable)).thenReturn(page);
		when(cursoRepository.findAllById(Set.of(disciplina.getCursoId()))).thenReturn(List.of(curso));

		PageResponseDTO<DisciplinaResponseDTO> response = disciplinaService.listar(pageable);

		assertThat(response.content()).hasSize(1);
		assertThat(response.content().get(0).codDisciplina()).isEqualTo("ALG");
		assertThat(response.content().get(0).codCurso()).isEqualTo("ES");
		assertThat(response.content().get(0).nomeCurso()).isEqualTo("Engenharia de Software");
		assertThat(response.page()).isZero();
		assertThat(response.size()).isEqualTo(10);
		assertThat(response.totalElements()).isEqualTo(1);
		assertThat(response.first()).isTrue();
		assertThat(response.last()).isTrue();
	}

	@Test
	void deveBuscarDisciplinaPorId_quandoExiste() {
		Long id = 1L;
		Disciplina disciplina = disciplinaExistente(id);
		Curso curso = cursoExistente(disciplina.getCursoId());
		when(disciplinaRepository.findById(id)).thenReturn(Optional.of(disciplina));
		when(cursoRepository.findById(disciplina.getCursoId())).thenReturn(Optional.of(curso));

		DisciplinaResponseDTO response = disciplinaService.buscarPorId(id);

		assertThat(response.id()).isEqualTo(id);
		assertThat(response.codDisciplina()).isEqualTo(disciplina.getCodDisciplina());
		assertThat(response.nome()).isEqualTo(disciplina.getNome());
		assertThat(response.codCurso()).isEqualTo(curso.getCodCurso());
		assertThat(response.nomeCurso()).isEqualTo(curso.getNome());
	}

	@Test
	void deveLancarEntidadeNaoEncontrada_quandoBuscarIdInexistente() {
		Long id = 99L;
		when(disciplinaRepository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> disciplinaService.buscarPorId(id))
				.isInstanceOf(EntidadeNaoEncontradaException.class)
				.hasMessageContaining(id.toString());
	}

	@Test
	void deveAtualizarDisciplina_quandoDadosValidos() {
		Long id = 1L;
		Disciplina disciplina = disciplinaExistente(id);
		DisciplinaRequestDTO request = new DisciplinaRequestDTO(
				"ED",
				"Estruturas de Dados",
				10L,
				2027,
				2);
		Curso curso = cursoExistente(request.cursoId());
		curso.setCodCurso("CC");
		curso.setNome("Ciência da Computação");

		when(disciplinaRepository.findById(id)).thenReturn(Optional.of(disciplina));
		when(cursoRepository.findById(request.cursoId())).thenReturn(Optional.of(curso));
		when(disciplinaRepository.existsByCodDisciplinaAndIdNot(request.codDisciplina(), id)).thenReturn(false);
		when(disciplinaRepository.save(disciplina)).thenReturn(disciplina);

		DisciplinaResponseDTO response = disciplinaService.atualizar(id, request);

		assertThat(response.codDisciplina()).isEqualTo("ED");
		assertThat(response.nome()).isEqualTo("Estruturas de Dados");
		assertThat(response.cursoId()).isEqualTo(10L);
		assertThat(response.codCurso()).isEqualTo("CC");
		assertThat(response.nomeCurso()).isEqualTo("Ciência da Computação");
		assertThat(response.ano()).isEqualTo(2027);
		assertThat(response.periodo()).isEqualTo(2);
	}

	@Test
	void deveLancarEntidadeNaoEncontrada_quandoAtualizarComCursoInexistente() {
		Long id = 1L;
		Disciplina disciplina = disciplinaExistente(id);
		DisciplinaRequestDTO request = new DisciplinaRequestDTO(
				"ED",
				"Estruturas de Dados",
				99L,
				2027,
				2);

		when(disciplinaRepository.findById(id)).thenReturn(Optional.of(disciplina));
		when(cursoRepository.findById(request.cursoId())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> disciplinaService.atualizar(id, request))
				.isInstanceOf(EntidadeNaoEncontradaException.class)
				.hasMessageContaining("99");

		verify(disciplinaRepository, never()).save(any());
	}

	@Test
	void deveRemoverDisciplina_quandoNaoHaTurmasVinculadas() {
		Long id = 1L;
		Disciplina disciplina = disciplinaExistente(id);
		when(disciplinaRepository.findById(id)).thenReturn(Optional.of(disciplina));
		when(turmaRepository.existsByDisciplinaId(id)).thenReturn(false);

		disciplinaService.remover(id);

		verify(disciplinaRepository).delete(disciplina);
	}

	@Test
	void deveLancarExclusaoBloqueada_quandoDisciplinaTemTurmasVinculadas() {
		Long id = 1L;
		Disciplina disciplina = disciplinaExistente(id);
		when(disciplinaRepository.findById(id)).thenReturn(Optional.of(disciplina));
		when(turmaRepository.existsByDisciplinaId(id)).thenReturn(true);

		assertThatThrownBy(() -> disciplinaService.remover(id))
				.isInstanceOf(ExclusaoBloqueadaVinculoAtivoException.class)
				.extracting(ex -> ((ExclusaoBloqueadaVinculoAtivoException) ex).getCodigo())
				.isEqualTo("EXCLUSAO_BLOQUEADA_VINCULO_ATIVO");

		verify(disciplinaRepository, never()).delete(any());
	}

	@Test
	void deveLancarEntidadeNaoEncontrada_quandoRemoverIdInexistente() {
		Long id = 99L;
		when(disciplinaRepository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> disciplinaService.remover(id))
				.isInstanceOf(EntidadeNaoEncontradaException.class);

		verify(disciplinaRepository, never()).delete(any());
	}

	private DisciplinaRequestDTO requestValido() {
		return new DisciplinaRequestDTO("ALG", "Algoritmos", 5L, 2026, 1);
	}

	private Disciplina disciplinaExistente(Long id) {
		Disciplina disciplina = new Disciplina();
		disciplina.setId(id);
		disciplina.setCodDisciplina("ALG");
		disciplina.setNome("Algoritmos");
		disciplina.setCursoId(5L);
		disciplina.setAno(2026);
		disciplina.setPeriodo(1);
		return disciplina;
	}

	private Curso cursoExistente(Long id) {
		Curso curso = new Curso();
		curso.setId(id);
		curso.setCodCurso("ES");
		curso.setNome("Engenharia de Software");
		curso.setDescricao("Descrição");
		return curso;
	}
}
