package br.com.matricula.web.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.matricula.web.domain.Aluno;
import br.com.matricula.web.dto.request.AlunoRequestDTO;
import br.com.matricula.web.dto.response.AlunoResponseDTO;
import br.com.matricula.web.dto.response.PageResponseDTO;
import br.com.matricula.web.exception.EntidadeNaoEncontradaException;
import br.com.matricula.web.exception.ExclusaoBloqueadaVinculoAtivoException;
import br.com.matricula.web.exception.RecursoDuplicadoException;
import br.com.matricula.web.repository.AlunoRepository;
import br.com.matricula.web.repository.MatriculaRepository;

@Service
public class AlunoService {

	private final AlunoRepository alunoRepository;
	private final MatriculaRepository matriculaRepository;

	public AlunoService(AlunoRepository alunoRepository, MatriculaRepository matriculaRepository) {
		this.alunoRepository = alunoRepository;
		this.matriculaRepository = matriculaRepository;
	}

	@Transactional
	public AlunoResponseDTO criar(AlunoRequestDTO alunoRequestDTO) {
		validarUnicidade(alunoRequestDTO.email(), alunoRequestDTO.cpf(), null);

		Aluno aluno = new Aluno();
		aplicarDados(aluno, alunoRequestDTO);

		return mapToResponseDTO(alunoRepository.save(aluno));
	}

	@Transactional(readOnly = true)
	public PageResponseDTO<AlunoResponseDTO> listar(Pageable pageable) {
		Page<AlunoResponseDTO> page = alunoRepository.findAll(pageable)
				.map(this::mapToResponseDTO);
		return PageResponseDTO.from(page);
	}

	@Transactional(readOnly = true)
	public AlunoResponseDTO buscarPorId(Long id) {
		return mapToResponseDTO(buscarAluno(id));
	}

	@Transactional
	public AlunoResponseDTO atualizar(Long id, AlunoRequestDTO alunoRequestDTO) {
		Aluno aluno = buscarAluno(id);
		validarUnicidade(alunoRequestDTO.email(), alunoRequestDTO.cpf(), id);
		aplicarDados(aluno, alunoRequestDTO);

		return mapToResponseDTO(alunoRepository.save(aluno));
	}

	@Transactional
	public void remover(Long id) {
		Aluno aluno = buscarAluno(id);

		if (matriculaRepository.existsByAlunoId(id)) {
			throw new ExclusaoBloqueadaVinculoAtivoException(
					"Não é possível excluir o aluno pois existem matrículas vinculadas.");
		}

		alunoRepository.delete(aluno);
	}

	private Aluno buscarAluno(Long id) {
		return alunoRepository.findById(id)
				.orElseThrow(() -> new EntidadeNaoEncontradaException(
						"Aluno não encontrado com id: " + id));
	}

	private void validarUnicidade(String email, String cpf, Long idAtual) {
		boolean emailDuplicado = idAtual == null
				? alunoRepository.existsByEmail(email)
				: alunoRepository.existsByEmailAndIdNot(email, idAtual);

		if (emailDuplicado) {
			throw new RecursoDuplicadoException(
					"EMAIL_DUPLICADO",
					"Já existe um aluno cadastrado com o e-mail informado.");
		}

		boolean cpfDuplicado = idAtual == null
				? alunoRepository.existsByCpf(cpf)
				: alunoRepository.existsByCpfAndIdNot(cpf, idAtual);

		if (cpfDuplicado) {
			throw new RecursoDuplicadoException(
					"CPF_DUPLICADO",
					"Já existe um aluno cadastrado com o CPF informado.");
		}
	}

	private void aplicarDados(Aluno aluno, AlunoRequestDTO alunoRequestDTO) {
		aluno.setNome(alunoRequestDTO.nome());
		aluno.setEmail(alunoRequestDTO.email());
		aluno.setCpf(alunoRequestDTO.cpf());
		aluno.setEndereco(alunoRequestDTO.endereco());
	}

	private AlunoResponseDTO mapToResponseDTO(Aluno aluno) {
		return new AlunoResponseDTO(
				aluno.getId(),
				aluno.getNome(),
				aluno.getEmail(),
				aluno.getCpf(),
				aluno.getEndereco());
	}
}
