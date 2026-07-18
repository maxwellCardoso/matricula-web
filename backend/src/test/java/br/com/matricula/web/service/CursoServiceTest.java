package br.com.matricula.web.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

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
import br.com.matricula.web.dto.request.CursoRequestDTO;
import br.com.matricula.web.dto.response.CursoResponseDTO;
import br.com.matricula.web.dto.response.PageResponseDTO;
import br.com.matricula.web.exception.EntidadeNaoEncontradaException;
import br.com.matricula.web.exception.ExclusaoBloqueadaVinculoAtivoException;
import br.com.matricula.web.repository.CursoRepository;
import br.com.matricula.web.repository.DisciplinaRepository;

@ExtendWith(MockitoExtension.class)
class CursoServiceTest {

	@Mock
	private CursoRepository cursoRepository;

	@Mock
	private DisciplinaRepository disciplinaRepository;

	@InjectMocks
	private CursoService cursoService;

	@Test
	void deveCriarCurso_quandoDadosValidos() {
		CursoRequestDTO request = requestValido();
		when(cursoRepository.existsByCodCurso(request.codCurso())).thenReturn(false);
		when(cursoRepository.save(any(Curso.class))).thenAnswer(invocation -> {
			Curso curso = invocation.getArgument(0);
			curso.setId(1L);
			return curso;
		});

		CursoResponseDTO response = cursoService.criar(request);

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.codCurso()).isEqualTo(request.codCurso());
		assertThat(response.nome()).isEqualTo(request.nome());
		assertThat(response.descricao()).isEqualTo(request.descricao());

		ArgumentCaptor<Curso> captor = ArgumentCaptor.forClass(Curso.class);
		verify(cursoRepository).save(captor.capture());
		assertThat(captor.getValue().getCodCurso()).isEqualTo(request.codCurso());
		assertThat(captor.getValue().getNome()).isEqualTo(request.nome());
	}

	@Test
	void deveListarCursosComPaginacao() {
		Curso curso = cursoExistente(1L);
		Pageable pageable = PageRequest.of(0, 10);
		Page<Curso> page = new PageImpl<>(List.of(curso), pageable, 1);
		when(cursoRepository.findAll(pageable)).thenReturn(page);

		PageResponseDTO<CursoResponseDTO> response = cursoService.listar(pageable);

		assertThat(response.content()).hasSize(1);
		assertThat(response.content().get(0).id()).isEqualTo(curso.getId());
		assertThat(response.content().get(0).codCurso()).isEqualTo(curso.getCodCurso());
		assertThat(response.content().get(0).nome()).isEqualTo(curso.getNome());
		assertThat(response.page()).isZero();
		assertThat(response.size()).isEqualTo(10);
		assertThat(response.totalElements()).isEqualTo(1);
		assertThat(response.first()).isTrue();
		assertThat(response.last()).isTrue();
	}

	@Test
	void deveBuscarCursoPorId_quandoExiste() {
		Long id = 1L;
		Curso curso = cursoExistente(id);
		when(cursoRepository.findById(id)).thenReturn(Optional.of(curso));

		CursoResponseDTO response = cursoService.buscarPorId(id);

		assertThat(response.id()).isEqualTo(id);
		assertThat(response.codCurso()).isEqualTo(curso.getCodCurso());
		assertThat(response.nome()).isEqualTo(curso.getNome());
	}

	@Test
	void deveLancarEntidadeNaoEncontrada_quandoBuscarIdInexistente() {
		Long id = 99L;
		when(cursoRepository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> cursoService.buscarPorId(id))
				.isInstanceOf(EntidadeNaoEncontradaException.class)
				.hasMessageContaining(id.toString());
	}

	@Test
	void deveAtualizarCurso_quandoDadosValidos() {
		Long id = 1L;
		Curso curso = cursoExistente(id);
		CursoRequestDTO request = new CursoRequestDTO(
				"CC",
				"Ciência da Computação",
				"Curso atualizado");

		when(cursoRepository.findById(id)).thenReturn(Optional.of(curso));
		when(cursoRepository.existsByCodCursoAndIdNot(request.codCurso(), id)).thenReturn(false);
		when(cursoRepository.save(curso)).thenReturn(curso);

		CursoResponseDTO response = cursoService.atualizar(id, request);

		assertThat(response.codCurso()).isEqualTo("CC");
		assertThat(response.nome()).isEqualTo("Ciência da Computação");
		assertThat(response.descricao()).isEqualTo("Curso atualizado");
	}

	@Test
	void deveRemoverCurso_quandoNaoHaDisciplinasVinculadas() {
		Long id = 1L;
		Curso curso = cursoExistente(id);
		when(cursoRepository.findById(id)).thenReturn(Optional.of(curso));
		when(disciplinaRepository.existsByCursoId(id)).thenReturn(false);

		cursoService.remover(id);

		verify(cursoRepository).delete(curso);
	}

	@Test
	void deveLancarExclusaoBloqueada_quandoCursoTemDisciplinasVinculadas() {
		Long id = 1L;
		Curso curso = cursoExistente(id);
		when(cursoRepository.findById(id)).thenReturn(Optional.of(curso));
		when(disciplinaRepository.existsByCursoId(id)).thenReturn(true);

		assertThatThrownBy(() -> cursoService.remover(id))
				.isInstanceOf(ExclusaoBloqueadaVinculoAtivoException.class)
				.extracting(ex -> ((ExclusaoBloqueadaVinculoAtivoException) ex).getCodigo())
				.isEqualTo("EXCLUSAO_BLOQUEADA_VINCULO_ATIVO");

		verify(cursoRepository, never()).delete(any());
	}

	@Test
	void deveLancarEntidadeNaoEncontrada_quandoRemoverIdInexistente() {
		Long id = 99L;
		when(cursoRepository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> cursoService.remover(id))
				.isInstanceOf(EntidadeNaoEncontradaException.class);

		verify(cursoRepository, never()).delete(any());
	}

	private CursoRequestDTO requestValido() {
		return new CursoRequestDTO("ES", "Engenharia de Software", "Formação em desenvolvimento de sistemas");
	}

	private Curso cursoExistente(Long id) {
		Curso curso = new Curso();
		curso.setId(id);
		curso.setCodCurso("ES");
		curso.setNome("Engenharia de Software");
		curso.setDescricao("Formação em desenvolvimento de sistemas");
		return curso;
	}
}
