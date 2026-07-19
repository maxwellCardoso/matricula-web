package br.com.matricula.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import br.com.matricula.web.TestcontainersConfiguration;
import br.com.matricula.web.domain.StatusMatricula;
import br.com.matricula.web.domain.StatusTurma;
import br.com.matricula.web.domain.Turma;
import br.com.matricula.web.repository.AlunoRepository;
import br.com.matricula.web.repository.CursoRepository;
import br.com.matricula.web.repository.DisciplinaRepository;
import br.com.matricula.web.repository.MatriculaRepository;
import br.com.matricula.web.repository.TurmaRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class MatriculaControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AlunoRepository alunoRepository;

	@Autowired
	private CursoRepository cursoRepository;

	@Autowired
	private DisciplinaRepository disciplinaRepository;

	@Autowired
	private TurmaRepository turmaRepository;

	@Autowired
	private MatriculaRepository matriculaRepository;

	@BeforeEach
	void setUp() {
		matriculaRepository.deleteAll();
		turmaRepository.deleteAll();
		disciplinaRepository.deleteAll();
		cursoRepository.deleteAll();
		alunoRepository.deleteAll();
	}

	@Test
	void deveMatricularAlunoEmTurmaAbertaComVaga() throws Exception {
		Long alunoId = criarAluno("Ana Silva", "ana.silva@email.com", "12345678901");
		Long turmaId = criarTurma("T01", 30);

		mockMvc.perform(post("/matriculas")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payloadMatricula(alunoId, turmaId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.alunoId").value(alunoId))
				.andExpect(jsonPath("$.nomeAluno").value("Ana Silva"))
				.andExpect(jsonPath("$.emailAluno").value("ana.silva@email.com"))
				.andExpect(jsonPath("$.cpfAluno").value("12345678901"))
				.andExpect(jsonPath("$.turmaId").value(turmaId))
				.andExpect(jsonPath("$.codTurma").value("T01"))
				.andExpect(jsonPath("$.status").value("PENDENTE"));
	}

	@Test
	void us02_deveRetornarConflito_quandoMatricularEmTurmaFechada() throws Exception {
		Long alunoId = criarAluno("Ana Silva", "ana.silva@email.com", "12345678901");
		Long turmaId = criarTurma("T01", 30);
		fecharTurma(turmaId);

		mockMvc.perform(post("/matriculas")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payloadMatricula(alunoId, turmaId)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("TURMA_FECHADA"));

		assertThat(matriculaRepository.count()).isZero();
	}

	@Test
	void us03_deveRetornarVagaIndisponivel_quandoConfirmarSemVaga() throws Exception {
		Long turmaId = criarTurma("T01", 1);
		Long alunoComVagaId = criarAluno("Ana Silva", "ana.silva@email.com", "12345678901");
		Long alunoSemVagaId = criarAluno("Bruno Costa", "bruno.costa@email.com", "98765432100");

		Long matriculaComVagaId = matricular(alunoComVagaId, turmaId);
		Long matriculaSemVagaId = matricular(alunoSemVagaId, turmaId);

		mockMvc.perform(patch("/matriculas/{id}/confirmar", matriculaComVagaId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CONFIRMADA"));

		mockMvc.perform(patch("/matriculas/{id}/confirmar", matriculaSemVagaId))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("VAGA_INDISPONIVEL"));

		Turma turma = turmaRepository.findById(turmaId).orElseThrow();
		assertThat(turma.getVagasOcupadas()).isEqualTo(1);
		assertThat(matriculaRepository.findById(matriculaSemVagaId).orElseThrow().getStatus())
				.isEqualTo(StatusMatricula.PENDENTE);
	}

	@Test
	void us04_deveRetornarConflito_quandoMatriculaDuplicada() throws Exception {
		Long alunoId = criarAluno("Ana Silva", "ana.silva@email.com", "12345678901");
		Long turmaId = criarTurma("T01", 30);

		mockMvc.perform(post("/matriculas")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payloadMatricula(alunoId, turmaId)))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/matriculas")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payloadMatricula(alunoId, turmaId)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("MATRICULA_DUPLICADA"));

		assertThat(matriculaRepository.count()).isEqualTo(1);
	}

	@Test
	void deveRetornarNotFound_quandoAlunoInexistente() throws Exception {
		Long turmaId = criarTurma("T01", 30);

		mockMvc.perform(post("/matriculas")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payloadMatricula(99999L, turmaId)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("ENTIDADE_NAO_ENCONTRADA"));
	}

	@Test
	void deveRetornarBadRequest_quandoPayloadInvalido() throws Exception {
		String payload = """
				{
				  "alunoId": null,
				  "turmaId": null
				}
				""";

		mockMvc.perform(post("/matriculas")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("VALIDACAO_FALHOU"))
				.andExpect(jsonPath("$.detalhes", hasItem(containsString("alunoId"))))
				.andExpect(jsonPath("$.detalhes", hasItem(containsString("turmaId"))));
	}

	@Test
	void deveListarMatriculasComDadosDoAlunoECodTurma() throws Exception {
		Long alunoId = criarAluno("Ana Silva", "ana.silva@email.com", "12345678901");
		Long turmaId = criarTurma("T01", 30);

		matricular(alunoId, turmaId);

		mockMvc.perform(get("/matriculas").param("alunoId", alunoId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].alunoId").value(alunoId))
				.andExpect(jsonPath("$[0].nomeAluno").value("Ana Silva"))
				.andExpect(jsonPath("$[0].emailAluno").value("ana.silva@email.com"))
				.andExpect(jsonPath("$[0].cpfAluno").value("12345678901"))
				.andExpect(jsonPath("$[0].turmaId").value(turmaId))
				.andExpect(jsonPath("$[0].codTurma").value("T01"))
				.andExpect(jsonPath("$[0].status").value("PENDENTE"));

		mockMvc.perform(get("/matriculas").param("turmaId", turmaId.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].nomeAluno").value("Ana Silva"))
				.andExpect(jsonPath("$[0].codTurma").value("T01"));
	}

	private void fecharTurma(Long turmaId) {
		Turma turma = turmaRepository.findById(turmaId).orElseThrow();
		turma.setStatus(StatusTurma.FECHADA);
		turmaRepository.save(turma);
	}

	private Long matricular(Long alunoId, Long turmaId) throws Exception {
		MvcResult result = mockMvc.perform(post("/matriculas")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payloadMatricula(alunoId, turmaId)))
				.andExpect(status().isCreated())
				.andReturn();
		return extrairId(result);
	}

	private String payloadMatricula(Long alunoId, Long turmaId) {
		return """
				{
				  "alunoId": %d,
				  "turmaId": %d
				}
				""".formatted(alunoId, turmaId);
	}

	private Long criarAluno(String nome, String email, String cpf) throws Exception {
		String payload = """
				{
				  "nome": "%s",
				  "email": "%s",
				  "cpf": "%s",
				  "endereco": "Rua A, 100"
				}
				""".formatted(nome, email, cpf);

		MvcResult result = mockMvc.perform(post("/alunos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isCreated())
				.andReturn();

		return extrairId(result);
	}

	private Long criarTurma(String codTurma, int vagasTotais) throws Exception {
		Long cursoId = criarCurso();
		Long disciplinaId = criarDisciplina(cursoId);

		String payload = """
				{
				  "codTurma": "%s",
				  "disciplinaId": %d,
				  "vagasTotais": %d
				}
				""".formatted(codTurma, disciplinaId, vagasTotais);

		MvcResult result = mockMvc.perform(post("/turmas")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isCreated())
				.andReturn();

		return extrairId(result);
	}

	private Long criarDisciplina(Long cursoId) throws Exception {
		String payload = """
				{
				  "codDisciplina": "ALG",
				  "nome": "Algoritmos",
				  "cursoId": %d,
				  "ano": 2026,
				  "periodo": 1
				}
				""".formatted(cursoId);

		MvcResult result = mockMvc.perform(post("/disciplinas")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isCreated())
				.andReturn();

		return extrairId(result);
	}

	private Long criarCurso() throws Exception {
		String payload = """
				{
				  "codCurso": "ES",
				  "nome": "Engenharia de Software",
				  "descricao": "Descrição"
				}
				""";

		MvcResult result = mockMvc.perform(post("/cursos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isCreated())
				.andReturn();

		return extrairId(result);
	}

	private Long extrairId(MvcResult result) throws Exception {
		String body = result.getResponse().getContentAsString();
		Number id = com.jayway.jsonpath.JsonPath.read(body, "$.id");
		return id.longValue();
	}
}
