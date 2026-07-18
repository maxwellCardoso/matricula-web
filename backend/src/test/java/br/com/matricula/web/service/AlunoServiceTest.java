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

import br.com.matricula.web.domain.Aluno;
import br.com.matricula.web.dto.request.AlunoRequestDTO;
import br.com.matricula.web.dto.response.AlunoResponseDTO;
import br.com.matricula.web.dto.response.PageResponseDTO;
import br.com.matricula.web.exception.EntidadeNaoEncontradaException;
import br.com.matricula.web.exception.RecursoDuplicadoException;
import br.com.matricula.web.repository.AlunoRepository;

@ExtendWith(MockitoExtension.class)
class AlunoServiceTest {

	@Mock
	private AlunoRepository alunoRepository;

	@InjectMocks
	private AlunoService alunoService;

	@Test
	void deveCriarAluno_quandoDadosValidos() {
		AlunoRequestDTO request = requestValido();
		when(alunoRepository.existsByEmail(request.email())).thenReturn(false);
		when(alunoRepository.existsByCpf(request.cpf())).thenReturn(false);
		when(alunoRepository.save(any(Aluno.class))).thenAnswer(invocation -> {
			Aluno aluno = invocation.getArgument(0);
			aluno.setId(1L);
			return aluno;
		});

		AlunoResponseDTO response = alunoService.criar(request);

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.nome()).isEqualTo(request.nome());
		assertThat(response.email()).isEqualTo(request.email());
		assertThat(response.cpf()).isEqualTo(request.cpf());
		assertThat(response.endereco()).isEqualTo(request.endereco());

		ArgumentCaptor<Aluno> captor = ArgumentCaptor.forClass(Aluno.class);
		verify(alunoRepository).save(captor.capture());
		assertThat(captor.getValue().getNome()).isEqualTo(request.nome());
	}

	@Test
	void deveLancarEmailDuplicado_quandoEmailJaExisteNaCriacao() {
		AlunoRequestDTO request = requestValido();
		when(alunoRepository.existsByEmail(request.email())).thenReturn(true);

		assertThatThrownBy(() -> alunoService.criar(request))
				.isInstanceOf(RecursoDuplicadoException.class)
				.hasMessageContaining("e-mail")
				.extracting(ex -> ((RecursoDuplicadoException) ex).getCodigo())
				.isEqualTo("EMAIL_DUPLICADO");

		verify(alunoRepository, never()).save(any());
	}

	@Test
	void deveLancarCpfDuplicado_quandoCpfJaExisteNaCriacao() {
		AlunoRequestDTO request = requestValido();
		when(alunoRepository.existsByEmail(request.email())).thenReturn(false);
		when(alunoRepository.existsByCpf(request.cpf())).thenReturn(true);

		assertThatThrownBy(() -> alunoService.criar(request))
				.isInstanceOf(RecursoDuplicadoException.class)
				.hasMessageContaining("CPF")
				.extracting(ex -> ((RecursoDuplicadoException) ex).getCodigo())
				.isEqualTo("CPF_DUPLICADO");

		verify(alunoRepository, never()).save(any());
	}

	@Test
	void deveListarAlunosComPaginacao() {
		Aluno aluno = alunoExistente(1L);
		Pageable pageable = PageRequest.of(0, 10);
		Page<Aluno> page = new PageImpl<>(List.of(aluno), pageable, 1);
		when(alunoRepository.findAll(pageable)).thenReturn(page);

		PageResponseDTO<AlunoResponseDTO> response = alunoService.listar(pageable);

		assertThat(response.content()).hasSize(1);
		assertThat(response.content().get(0).id()).isEqualTo(aluno.getId());
		assertThat(response.content().get(0).nome()).isEqualTo(aluno.getNome());
		assertThat(response.page()).isZero();
		assertThat(response.size()).isEqualTo(10);
		assertThat(response.totalElements()).isEqualTo(1);
		assertThat(response.totalPages()).isEqualTo(1);
		assertThat(response.first()).isTrue();
		assertThat(response.last()).isTrue();
	}

	@Test
	void deveBuscarAlunoPorId_quandoExiste() {
		Long id = 1L;
		Aluno aluno = alunoExistente(id);
		when(alunoRepository.findById(id)).thenReturn(Optional.of(aluno));

		AlunoResponseDTO response = alunoService.buscarPorId(id);

		assertThat(response.id()).isEqualTo(id);
		assertThat(response.email()).isEqualTo(aluno.getEmail());
	}

	@Test
	void deveLancarEntidadeNaoEncontrada_quandoBuscarIdInexistente() {
		Long id = 99L;
		when(alunoRepository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> alunoService.buscarPorId(id))
				.isInstanceOf(EntidadeNaoEncontradaException.class)
				.hasMessageContaining(id.toString());
	}

	@Test
	void deveAtualizarAluno_quandoDadosValidos() {
		Long id = 1L;
		Aluno aluno = alunoExistente(id);
		AlunoRequestDTO request = new AlunoRequestDTO(
				"Maria Souza",
				"maria@email.com",
				"98765432100",
				"Av. Brasil, 200");

		when(alunoRepository.findById(id)).thenReturn(Optional.of(aluno));
		when(alunoRepository.existsByEmailAndIdNot(request.email(), id)).thenReturn(false);
		when(alunoRepository.existsByCpfAndIdNot(request.cpf(), id)).thenReturn(false);
		when(alunoRepository.save(aluno)).thenReturn(aluno);

		AlunoResponseDTO response = alunoService.atualizar(id, request);

		assertThat(response.nome()).isEqualTo("Maria Souza");
		assertThat(response.email()).isEqualTo("maria@email.com");
		assertThat(response.cpf()).isEqualTo("98765432100");
		assertThat(response.endereco()).isEqualTo("Av. Brasil, 200");
	}

	@Test
	void deveLancarEmailDuplicado_quandoEmailJaExisteNaAtualizacao() {
		Long id = 1L;
		Aluno aluno = alunoExistente(id);
		AlunoRequestDTO request = new AlunoRequestDTO(
				"Maria Souza",
				"outro@email.com",
				"12345678901",
				null);

		when(alunoRepository.findById(id)).thenReturn(Optional.of(aluno));
		when(alunoRepository.existsByEmailAndIdNot(request.email(), id)).thenReturn(true);

		assertThatThrownBy(() -> alunoService.atualizar(id, request))
				.isInstanceOf(RecursoDuplicadoException.class)
				.extracting(ex -> ((RecursoDuplicadoException) ex).getCodigo())
				.isEqualTo("EMAIL_DUPLICADO");
	}

	@Test
	void deveRemoverAluno_quandoExiste() {
		Long id = 1L;
		Aluno aluno = alunoExistente(id);
		when(alunoRepository.findById(id)).thenReturn(Optional.of(aluno));

		alunoService.remover(id);

		verify(alunoRepository).delete(aluno);
	}

	@Test
	void deveLancarEntidadeNaoEncontrada_quandoRemoverIdInexistente() {
		Long id = 99L;
		when(alunoRepository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> alunoService.remover(id))
				.isInstanceOf(EntidadeNaoEncontradaException.class);

		verify(alunoRepository, never()).delete(any());
	}

	private AlunoRequestDTO requestValido() {
		return new AlunoRequestDTO(
				"João Silva",
				"joao@email.com",
				"12345678901",
				"Rua A, 100");
	}

	private Aluno alunoExistente(Long id) {
		Aluno aluno = new Aluno();
		aluno.setId(id);
		aluno.setNome("João Silva");
		aluno.setEmail("joao@email.com");
		aluno.setCpf("12345678901");
		aluno.setEndereco("Rua A, 100");
		return aluno;
	}
}
