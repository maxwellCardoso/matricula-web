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

import br.com.matricula.web.domain.Disciplina;
import br.com.matricula.web.domain.StatusTurma;
import br.com.matricula.web.domain.Turma;
import br.com.matricula.web.dto.request.TurmaRequestDTO;
import br.com.matricula.web.dto.response.PageResponseDTO;
import br.com.matricula.web.dto.response.TurmaResponseDTO;
import br.com.matricula.web.exception.EntidadeNaoEncontradaException;
import br.com.matricula.web.exception.ExclusaoBloqueadaVinculoAtivoException;
import br.com.matricula.web.repository.DisciplinaRepository;
import br.com.matricula.web.repository.MatriculaRepository;
import br.com.matricula.web.repository.TurmaRepository;

@ExtendWith(MockitoExtension.class)
class TurmaServiceTest {

	@Mock
	private TurmaRepository turmaRepository;

	@Mock
	private DisciplinaRepository disciplinaRepository;

	@Mock
	private MatriculaRepository matriculaRepository;

	@InjectMocks
	private TurmaService turmaService;

	@Test
	void deveCriarTurma_quandoDadosValidos() {
		TurmaRequestDTO request = requestValido();
		Disciplina disciplina = disciplinaExistente(request.disciplinaId());
		when(disciplinaRepository.findById(request.disciplinaId())).thenReturn(Optional.of(disciplina));
		when(turmaRepository.existsByCodTurma(request.codTurma())).thenReturn(false);
		when(turmaRepository.save(any(Turma.class))).thenAnswer(invocation -> {
			Turma turma = invocation.getArgument(0);
			turma.setId(1L);
			return turma;
		});

		TurmaResponseDTO response = turmaService.criar(request);

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.codTurma()).isEqualTo(request.codTurma());
		assertThat(response.disciplinaId()).isEqualTo(request.disciplinaId());
		assertThat(response.codDisciplina()).isEqualTo(disciplina.getCodDisciplina());
		assertThat(response.nomeDisciplina()).isEqualTo(disciplina.getNome());
		assertThat(response.vagasTotais()).isEqualTo(request.vagasTotais());
		assertThat(response.vagasOcupadas()).isZero();
		assertThat(response.status()).isEqualTo(StatusTurma.ABERTA);

		ArgumentCaptor<Turma> captor = ArgumentCaptor.forClass(Turma.class);
		verify(turmaRepository).save(captor.capture());
		assertThat(captor.getValue().getCodTurma()).isEqualTo(request.codTurma());
		assertThat(captor.getValue().getVagasOcupadas()).isZero();
		assertThat(captor.getValue().getStatus()).isEqualTo(StatusTurma.ABERTA);
	}

	@Test
	void deveLancarEntidadeNaoEncontrada_quandoCriarComDisciplinaInexistente() {
		TurmaRequestDTO request = requestValido();
		when(disciplinaRepository.findById(request.disciplinaId())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> turmaService.criar(request))
				.isInstanceOf(EntidadeNaoEncontradaException.class)
				.hasMessageContaining(request.disciplinaId().toString());

		verify(turmaRepository, never()).save(any());
	}

	@Test
	void deveListarTurmasComPaginacaoEDadosDaDisciplina() {
		Pageable pageable = PageRequest.of(0, 10);
		TurmaResponseDTO dto = new TurmaResponseDTO(
				1L, "T01", 5L, "ALG", "Algoritmos", 30, 0, StatusTurma.ABERTA);
		Page<TurmaResponseDTO> page = new PageImpl<>(List.of(dto), pageable, 1);
		when(turmaRepository.findDetalhadas(pageable)).thenReturn(page);

		PageResponseDTO<TurmaResponseDTO> response = turmaService.listar(pageable);

		assertThat(response.content()).hasSize(1);
		assertThat(response.content().get(0).codTurma()).isEqualTo("T01");
		assertThat(response.content().get(0).codDisciplina()).isEqualTo("ALG");
		assertThat(response.content().get(0).nomeDisciplina()).isEqualTo("Algoritmos");
		assertThat(response.page()).isZero();
		assertThat(response.size()).isEqualTo(10);
		assertThat(response.totalElements()).isEqualTo(1);
		assertThat(response.first()).isTrue();
		assertThat(response.last()).isTrue();
	}

	@Test
	void deveBuscarTurmaPorId_quandoExiste() {
		Long id = 1L;
		TurmaResponseDTO dto = new TurmaResponseDTO(
				id, "T01", 5L, "ALG", "Algoritmos", 30, 0, StatusTurma.ABERTA);
		when(turmaRepository.findDetalhadaById(id)).thenReturn(Optional.of(dto));

		TurmaResponseDTO response = turmaService.buscarPorId(id);

		assertThat(response.id()).isEqualTo(id);
		assertThat(response.codTurma()).isEqualTo("T01");
		assertThat(response.disciplinaId()).isEqualTo(5L);
		assertThat(response.codDisciplina()).isEqualTo("ALG");
		assertThat(response.nomeDisciplina()).isEqualTo("Algoritmos");
		assertThat(response.status()).isEqualTo(StatusTurma.ABERTA);
	}

	@Test
	void deveLancarEntidadeNaoEncontrada_quandoBuscarIdInexistente() {
		Long id = 99L;
		when(turmaRepository.findDetalhadaById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> turmaService.buscarPorId(id))
				.isInstanceOf(EntidadeNaoEncontradaException.class)
				.hasMessageContaining(id.toString());
	}

	@Test
	void deveAtualizarTurma_quandoDadosValidos() {
		Long id = 1L;
		Turma turma = turmaExistente(id);
		TurmaRequestDTO request = new TurmaRequestDTO("T02", 20L, 40);
		Disciplina disciplina = disciplinaExistente(request.disciplinaId());
		disciplina.setCodDisciplina("ED");
		disciplina.setNome("Estruturas de Dados");

		when(turmaRepository.findById(id)).thenReturn(Optional.of(turma));
		when(disciplinaRepository.findById(request.disciplinaId())).thenReturn(Optional.of(disciplina));
		when(turmaRepository.existsByCodTurmaAndIdNot(request.codTurma(), id)).thenReturn(false);
		when(turmaRepository.save(turma)).thenReturn(turma);

		TurmaResponseDTO response = turmaService.atualizar(id, request);

		assertThat(response.codTurma()).isEqualTo("T02");
		assertThat(response.disciplinaId()).isEqualTo(20L);
		assertThat(response.codDisciplina()).isEqualTo("ED");
		assertThat(response.nomeDisciplina()).isEqualTo("Estruturas de Dados");
		assertThat(response.vagasTotais()).isEqualTo(40);
		assertThat(response.vagasOcupadas()).isZero();
		assertThat(response.status()).isEqualTo(StatusTurma.ABERTA);
	}

	@Test
	void deveLancarEntidadeNaoEncontrada_quandoAtualizarComDisciplinaInexistente() {
		Long id = 1L;
		Turma turma = turmaExistente(id);
		TurmaRequestDTO request = new TurmaRequestDTO("T02", 99L, 40);

		when(turmaRepository.findById(id)).thenReturn(Optional.of(turma));
		when(disciplinaRepository.findById(request.disciplinaId())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> turmaService.atualizar(id, request))
				.isInstanceOf(EntidadeNaoEncontradaException.class)
				.hasMessageContaining("99");

		verify(turmaRepository, never()).save(any());
	}

	@Test
	void deveRemoverTurma_quandoNaoHaMatriculasVinculadas() {
		Long id = 1L;
		Turma turma = turmaExistente(id);
		when(turmaRepository.findById(id)).thenReturn(Optional.of(turma));
		when(matriculaRepository.existsByTurmaId(id)).thenReturn(false);

		turmaService.remover(id);

		verify(turmaRepository).delete(turma);
	}

	@Test
	void deveLancarExclusaoBloqueada_quandoTurmaTemMatriculasVinculadas() {
		Long id = 1L;
		Turma turma = turmaExistente(id);
		when(turmaRepository.findById(id)).thenReturn(Optional.of(turma));
		when(matriculaRepository.existsByTurmaId(id)).thenReturn(true);

		assertThatThrownBy(() -> turmaService.remover(id))
				.isInstanceOf(ExclusaoBloqueadaVinculoAtivoException.class)
				.extracting(ex -> ((ExclusaoBloqueadaVinculoAtivoException) ex).getCodigo())
				.isEqualTo("EXCLUSAO_BLOQUEADA_VINCULO_ATIVO");

		verify(turmaRepository, never()).delete(any());
	}

	@Test
	void deveLancarEntidadeNaoEncontrada_quandoRemoverIdInexistente() {
		Long id = 99L;
		when(turmaRepository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> turmaService.remover(id))
				.isInstanceOf(EntidadeNaoEncontradaException.class);

		verify(turmaRepository, never()).delete(any());
	}

	private TurmaRequestDTO requestValido() {
		return new TurmaRequestDTO("T01", 5L, 30);
	}

	private Turma turmaExistente(Long id) {
		Turma turma = new Turma();
		turma.setId(id);
		turma.setCodTurma("T01");
		turma.setDisciplinaId(5L);
		turma.setVagasTotais(30);
		turma.setVagasOcupadas(0);
		turma.setStatus(StatusTurma.ABERTA);
		return turma;
	}

	private Disciplina disciplinaExistente(Long id) {
		Disciplina disciplina = new Disciplina();
		disciplina.setId(id);
		disciplina.setCodDisciplina("ALG");
		disciplina.setNome("Algoritmos");
		disciplina.setCursoId(1L);
		disciplina.setAno(2026);
		disciplina.setPeriodo(1);
		return disciplina;
	}
}
