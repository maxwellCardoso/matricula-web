package br.com.matricula.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import br.com.matricula.web.domain.Matricula;
import br.com.matricula.web.domain.StatusMatricula;
import br.com.matricula.web.repository.AlunoRepository;
import br.com.matricula.web.repository.CursoRepository;
import br.com.matricula.web.repository.DisciplinaRepository;
import br.com.matricula.web.repository.MatriculaRepository;
import br.com.matricula.web.repository.TurmaRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class TurmaControllerIntegrationTest {

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
	void deveCriarListarBuscarAtualizarERemoverTurma() throws Exception {
		Long disciplinaId = criarDisciplina();

		String payload = """
				{
				  "codTurma": "T01",
				  "disciplinaId": %d,
				  "vagasTotais": 30
				}
				""".formatted(disciplinaId);

		MvcResult criarResult = mockMvc.perform(post("/turmas")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.codTurma").value("T01"))
				.andExpect(jsonPath("$.disciplinaId").value(disciplinaId))
				.andExpect(jsonPath("$.codDisciplina").value("ALG"))
				.andExpect(jsonPath("$.nomeDisciplina").value("Algoritmos"))
				.andExpect(jsonPath("$.vagasTotais").value(30))
				.andExpect(jsonPath("$.vagasOcupadas").value(0))
				.andExpect(jsonPath("$.status").value("ABERTA"))
				.andReturn();

		Long id = extrairId(criarResult);

		mockMvc.perform(get("/turmas").param("page", "0").param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(id))
				.andExpect(jsonPath("$.content[0].codTurma").value("T01"))
				.andExpect(jsonPath("$.content[0].codDisciplina").value("ALG"))
				.andExpect(jsonPath("$.content[0].nomeDisciplina").value("Algoritmos"))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(10))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.totalPages").value(1))
				.andExpect(jsonPath("$.first").value(true))
				.andExpect(jsonPath("$.last").value(true));

		mockMvc.perform(get("/turmas/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.disciplinaId").value(disciplinaId))
				.andExpect(jsonPath("$.codDisciplina").value("ALG"))
				.andExpect(jsonPath("$.nomeDisciplina").value("Algoritmos"))
				.andExpect(jsonPath("$.status").value("ABERTA"));

		String atualizacao = """
				{
				  "codTurma": "T02",
				  "disciplinaId": %d,
				  "vagasTotais": 40
				}
				""".formatted(disciplinaId);

		mockMvc.perform(put("/turmas/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content(atualizacao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.codTurma").value("T02"))
				.andExpect(jsonPath("$.vagasTotais").value(40))
				.andExpect(jsonPath("$.vagasOcupadas").value(0))
				.andExpect(jsonPath("$.status").value("ABERTA"));

		mockMvc.perform(delete("/turmas/{id}", id))
				.andExpect(status().isNoContent());

		assertThat(turmaRepository.findById(id)).isEmpty();
	}

	@Test
	void deveRetornar400_quandoVagasTotaisMenorOuIgualAZero() throws Exception {
		Long disciplinaId = criarDisciplina();

		String payload = """
				{
				  "codTurma": "T01",
				  "disciplinaId": %d,
				  "vagasTotais": 0
				}
				""".formatted(disciplinaId);

		mockMvc.perform(post("/turmas")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.codigo").value("VALIDACAO_FALHOU"))
				.andExpect(jsonPath("$.detalhes").isArray())
				.andExpect(jsonPath("$.detalhes", hasItem(containsString("vagasTotais"))));
	}

	@Test
	void deveRetornar404_quandoDisciplinaIdInexistente() throws Exception {
		Long disciplinaIdInexistente = 999L;

		String payload = """
				{
				  "codTurma": "T01",
				  "disciplinaId": %d,
				  "vagasTotais": 30
				}
				""".formatted(disciplinaIdInexistente);

		mockMvc.perform(post("/turmas")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.codigo").value("ENTIDADE_NAO_ENCONTRADA"));
	}

	@Test
	void deveRetornar404_quandoTurmaNaoExiste() throws Exception {
		Long idInexistente = 999L;

		mockMvc.perform(get("/turmas/{id}", idInexistente))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.codigo").value("ENTIDADE_NAO_ENCONTRADA"));
	}

	@Test
	void deveRetornar409_quandoExcluirTurmaComMatriculaVinculada() throws Exception {
		Long disciplinaId = criarDisciplina();
		Long turmaId = criarTurma(disciplinaId, "T01", 30);
		Long alunoId = criarAluno();

		Matricula matricula = new Matricula();
		matricula.setAlunoId(alunoId);
		matricula.setTurmaId(turmaId);
		matricula.setStatus(StatusMatricula.PENDENTE);
		matriculaRepository.save(matricula);

		mockMvc.perform(delete("/turmas/{id}", turmaId))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.codigo").value("EXCLUSAO_BLOQUEADA_VINCULO_ATIVO"));

		assertThat(turmaRepository.findById(turmaId)).isPresent();
	}

	private Long criarAluno() throws Exception {
		String payload = """
				{
				  "nome": "Ana Silva",
				  "email": "ana.silva@email.com",
				  "cpf": "12345678901",
				  "endereco": "Rua A, 100"
				}
				""";

		MvcResult result = mockMvc.perform(post("/alunos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isCreated())
				.andReturn();

		return extrairId(result);
	}

	private Long criarDisciplina() throws Exception {
		Long cursoId = criarCurso();

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

	private Long criarTurma(Long disciplinaId, String codTurma, int vagasTotais) throws Exception {
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

	private Long extrairId(MvcResult result) throws Exception {
		String body = result.getResponse().getContentAsString();
		Number id = com.jayway.jsonpath.JsonPath.read(body, "$.id");
		return id.longValue();
	}
}
