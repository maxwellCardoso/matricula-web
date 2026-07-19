package br.com.matricula.web.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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
import br.com.matricula.web.exception.TurmaFechadaException;
import br.com.matricula.web.exception.VagaIndisponivelException;
import br.com.matricula.web.repository.AlunoRepository;
import br.com.matricula.web.repository.MatriculaRepository;
import br.com.matricula.web.repository.TurmaRepository;

@ExtendWith(MockitoExtension.class)
class MatriculaServiceTest {

	@Mock
	private MatriculaRepository matriculaRepository;

	@Mock
	private AlunoRepository alunoRepository;

	@Mock
	private TurmaRepository turmaRepository;

	@InjectMocks
	private MatriculaService matriculaService;

	@Test
	void deveCriarMatriculaPendente_quandoTurmaAbertaComVaga() {
		MatriculaRequestDTO request = new MatriculaRequestDTO(1L, 10L);
		Aluno aluno = alunoExistente(request.alunoId());
		Turma turma = turmaExistente(request.turmaId(), StatusTurma.ABERTA, 5, 30);

		when(alunoRepository.findById(request.alunoId())).thenReturn(Optional.of(aluno));
		when(turmaRepository.findById(request.turmaId())).thenReturn(Optional.of(turma));
		when(matriculaRepository.existsByAlunoIdAndTurmaIdAndStatusNot(
				request.alunoId(), request.turmaId(), StatusMatricula.CANCELADA)).thenReturn(false);
		when(matriculaRepository.save(any(Matricula.class))).thenAnswer(invocation -> {
			Matricula matricula = invocation.getArgument(0);
			matricula.setId(100L);
			return matricula;
		});

		MatriculaResponseDTO response = matriculaService.matricular(request);

		assertThat(response.id()).isEqualTo(100L);
		assertThat(response.status()).isEqualTo(StatusMatricula.PENDENTE);
		assertThat(response.codTurma()).isEqualTo(turma.getCodTurma());

		ArgumentCaptor<Matricula> captor = ArgumentCaptor.forClass(Matricula.class);
		verify(matriculaRepository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(StatusMatricula.PENDENTE);
		assertThat(turma.getVagasOcupadas()).isEqualTo(5);
	}

	@Test
	void deveLancarTurmaFechada_quandoMatricularEmTurmaFechada() {
		MatriculaRequestDTO request = new MatriculaRequestDTO(1L, 10L);
		Aluno aluno = alunoExistente(request.alunoId());
		Turma turma = turmaExistente(request.turmaId(), StatusTurma.FECHADA, 0, 30);

		when(alunoRepository.findById(request.alunoId())).thenReturn(Optional.of(aluno));
		when(turmaRepository.findById(request.turmaId())).thenReturn(Optional.of(turma));

		assertThatThrownBy(() -> matriculaService.matricular(request))
				.isInstanceOf(TurmaFechadaException.class)
				.extracting(ex -> ((TurmaFechadaException) ex).getCodigo())
				.isEqualTo("TURMA_FECHADA");

		verify(matriculaRepository, never()).save(any());
	}

	@Test
	void deveLancarVagaIndisponivel_quandoConfirmarSemVaga() {
		Long matriculaId = 100L;
		Matricula matricula = matriculaPendente(matriculaId, 1L, 10L);

		when(matriculaRepository.findById(matriculaId)).thenReturn(Optional.of(matricula));
		when(turmaRepository.incrementarVagasOcupadasSeDisponivel(matricula.getTurmaId())).thenReturn(0);

		assertThatThrownBy(() -> matriculaService.confirmar(matriculaId))
				.isInstanceOf(VagaIndisponivelException.class)
				.extracting(ex -> ((VagaIndisponivelException) ex).getCodigo())
				.isEqualTo("VAGA_INDISPONIVEL");

		assertThat(matricula.getStatus()).isEqualTo(StatusMatricula.PENDENTE);
		verify(matriculaRepository, never()).save(any());
	}

	@Test
	void deveConfirmarMatricula_quandoHaVagaDisponivel() {
		Long matriculaId = 100L;
		Matricula matricula = matriculaPendente(matriculaId, 1L, 10L);
		MatriculaResponseDTO detalhada = responseConfirmada(matriculaId, matricula.getAlunoId(), matricula.getTurmaId());

		when(matriculaRepository.findById(matriculaId)).thenReturn(Optional.of(matricula));
		when(turmaRepository.incrementarVagasOcupadasSeDisponivel(matricula.getTurmaId())).thenReturn(1);
		when(matriculaRepository.save(matricula)).thenReturn(matricula);
		when(matriculaRepository.findDetalhadaById(matriculaId)).thenReturn(Optional.of(detalhada));

		MatriculaResponseDTO response = matriculaService.confirmar(matriculaId);

		assertThat(response.status()).isEqualTo(StatusMatricula.CONFIRMADA);
		assertThat(matricula.getStatus()).isEqualTo(StatusMatricula.CONFIRMADA);
		verify(turmaRepository).incrementarVagasOcupadasSeDisponivel(matricula.getTurmaId());
	}

	@Test
	void deveLancarMatriculaDuplicada_quandoAlunoJaPossuiMatriculaAtiva() {
		MatriculaRequestDTO request = new MatriculaRequestDTO(1L, 10L);
		Aluno aluno = alunoExistente(request.alunoId());
		Turma turma = turmaExistente(request.turmaId(), StatusTurma.ABERTA, 5, 30);

		when(alunoRepository.findById(request.alunoId())).thenReturn(Optional.of(aluno));
		when(turmaRepository.findById(request.turmaId())).thenReturn(Optional.of(turma));
		when(matriculaRepository.existsByAlunoIdAndTurmaIdAndStatusNot(
				request.alunoId(), request.turmaId(), StatusMatricula.CANCELADA)).thenReturn(true);

		assertThatThrownBy(() -> matriculaService.matricular(request))
				.isInstanceOf(MatriculaDuplicadaException.class)
				.extracting(ex -> ((MatriculaDuplicadaException) ex).getCodigo())
				.isEqualTo("MATRICULA_DUPLICADA");

		verify(matriculaRepository, never()).save(any());
	}

	@Test
	void deveLancarEntidadeNaoEncontrada_quandoAlunoInexistente() {
		MatriculaRequestDTO request = new MatriculaRequestDTO(99L, 10L);
		when(alunoRepository.findById(request.alunoId())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> matriculaService.matricular(request))
				.isInstanceOf(EntidadeNaoEncontradaException.class)
				.hasMessageContaining("99");

		verify(matriculaRepository, never()).save(any());
	}

	@Test
	void deveLancarEntidadeNaoEncontrada_quandoTurmaInexistente() {
		MatriculaRequestDTO request = new MatriculaRequestDTO(1L, 99L);
		Aluno aluno = alunoExistente(request.alunoId());

		when(alunoRepository.findById(request.alunoId())).thenReturn(Optional.of(aluno));
		when(turmaRepository.findById(request.turmaId())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> matriculaService.matricular(request))
				.isInstanceOf(EntidadeNaoEncontradaException.class)
				.hasMessageContaining("99");

		verify(matriculaRepository, never()).save(any());
	}

	@Test
	void deveCancelarMatriculaConfirmada_eDecrementarVaga() {
		Long matriculaId = 100L;
		Matricula matricula = matriculaComStatus(matriculaId, 1L, 10L, StatusMatricula.CONFIRMADA);
		MatriculaResponseDTO detalhada = responseComStatus(
				matriculaId, matricula.getAlunoId(), matricula.getTurmaId(), StatusMatricula.CANCELADA);

		when(matriculaRepository.findById(matriculaId)).thenReturn(Optional.of(matricula));
		when(turmaRepository.decrementarVagasOcupadas(matricula.getTurmaId())).thenReturn(1);
		when(matriculaRepository.save(matricula)).thenReturn(matricula);
		when(matriculaRepository.findDetalhadaById(matriculaId)).thenReturn(Optional.of(detalhada));

		MatriculaResponseDTO response = matriculaService.cancelar(matriculaId);

		assertThat(response.status()).isEqualTo(StatusMatricula.CANCELADA);
		assertThat(matricula.getStatus()).isEqualTo(StatusMatricula.CANCELADA);
		verify(turmaRepository).decrementarVagasOcupadas(matricula.getTurmaId());
	}

	@Test
	void deveCancelarMatriculaPendente_semAlterarVagas() {
		Long matriculaId = 100L;
		Matricula matricula = matriculaPendente(matriculaId, 1L, 10L);
		MatriculaResponseDTO detalhada = responseComStatus(
				matriculaId, matricula.getAlunoId(), matricula.getTurmaId(), StatusMatricula.CANCELADA);

		when(matriculaRepository.findById(matriculaId)).thenReturn(Optional.of(matricula));
		when(matriculaRepository.save(matricula)).thenReturn(matricula);
		when(matriculaRepository.findDetalhadaById(matriculaId)).thenReturn(Optional.of(detalhada));

		MatriculaResponseDTO response = matriculaService.cancelar(matriculaId);

		assertThat(response.status()).isEqualTo(StatusMatricula.CANCELADA);
		assertThat(matricula.getStatus()).isEqualTo(StatusMatricula.CANCELADA);
		verify(turmaRepository, never()).decrementarVagasOcupadas(any());
	}

	@Test
	void deveLancarMatriculaJaCancelada_quandoCancelarNovamente() {
		Long matriculaId = 100L;
		Matricula matricula = matriculaComStatus(matriculaId, 1L, 10L, StatusMatricula.CANCELADA);

		when(matriculaRepository.findById(matriculaId)).thenReturn(Optional.of(matricula));

		assertThatThrownBy(() -> matriculaService.cancelar(matriculaId))
				.isInstanceOf(MatriculaJaCanceladaException.class)
				.extracting(ex -> ((MatriculaJaCanceladaException) ex).getCodigo())
				.isEqualTo("MATRICULA_JA_CANCELADA");

		verify(matriculaRepository, never()).save(any());
		verify(turmaRepository, never()).decrementarVagasOcupadas(any());
	}

	@Test
	void deveListarMatriculasPorAluno() {
		Long alunoId = 1L;
		Pageable pageable = PageRequest.of(0, 10);
		MatriculaResponseDTO detalhada = responseComStatus(100L, alunoId, 10L, StatusMatricula.PENDENTE);
		Page<MatriculaResponseDTO> page = new PageImpl<>(List.of(detalhada), pageable, 1);

		when(matriculaRepository.findDetalhadas(alunoId, null, pageable)).thenReturn(page);

		PageResponseDTO<MatriculaResponseDTO> response = matriculaService.listar(alunoId, null, pageable);

		assertThat(response.content()).hasSize(1);
		assertThat(response.content().get(0).alunoId()).isEqualTo(alunoId);
		assertThat(response.content().get(0).status()).isEqualTo(StatusMatricula.PENDENTE);
		assertThat(response.totalElements()).isEqualTo(1);
		verify(matriculaRepository).findDetalhadas(alunoId, null, pageable);
	}

	@Test
	void deveListarMatriculasPorTurma() {
		Long turmaId = 10L;
		Pageable pageable = PageRequest.of(0, 10);
		MatriculaResponseDTO detalhada = responseComStatus(100L, 1L, turmaId, StatusMatricula.CONFIRMADA);
		Page<MatriculaResponseDTO> page = new PageImpl<>(List.of(detalhada), pageable, 1);

		when(matriculaRepository.findDetalhadas(null, turmaId, pageable)).thenReturn(page);

		PageResponseDTO<MatriculaResponseDTO> response = matriculaService.listar(null, turmaId, pageable);

		assertThat(response.content()).hasSize(1);
		assertThat(response.content().get(0).turmaId()).isEqualTo(turmaId);
		assertThat(response.content().get(0).status()).isEqualTo(StatusMatricula.CONFIRMADA);
		assertThat(response.totalElements()).isEqualTo(1);
		verify(matriculaRepository).findDetalhadas(null, turmaId, pageable);
	}

	private Aluno alunoExistente(Long id) {
		Aluno aluno = new Aluno();
		aluno.setId(id);
		aluno.setNome("Ana Silva");
		aluno.setEmail("ana.silva@email.com");
		aluno.setCpf("12345678901");
		return aluno;
	}

	private Turma turmaExistente(Long id, StatusTurma status, int vagasOcupadas, int vagasTotais) {
		Turma turma = new Turma();
		turma.setId(id);
		turma.setCodTurma("T01");
		turma.setDisciplinaId(5L);
		turma.setVagasTotais(vagasTotais);
		turma.setVagasOcupadas(vagasOcupadas);
		turma.setStatus(status);
		return turma;
	}

	private Matricula matriculaPendente(Long id, Long alunoId, Long turmaId) {
		return matriculaComStatus(id, alunoId, turmaId, StatusMatricula.PENDENTE);
	}

	private Matricula matriculaComStatus(Long id, Long alunoId, Long turmaId, StatusMatricula status) {
		Matricula matricula = new Matricula();
		matricula.setId(id);
		matricula.setAlunoId(alunoId);
		matricula.setTurmaId(turmaId);
		matricula.setStatus(status);
		return matricula;
	}

	private MatriculaResponseDTO responseConfirmada(Long id, Long alunoId, Long turmaId) {
		return responseComStatus(id, alunoId, turmaId, StatusMatricula.CONFIRMADA);
	}

	private MatriculaResponseDTO responseComStatus(
			Long id, Long alunoId, Long turmaId, StatusMatricula status) {
		LocalDateTime agora = LocalDateTime.now();
		return new MatriculaResponseDTO(
				id,
				alunoId,
				"Ana Silva",
				"ana.silva@email.com",
				"12345678901",
				turmaId,
				"T01",
				status,
				agora,
				agora);
	}
}
