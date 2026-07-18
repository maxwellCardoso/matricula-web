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
import br.com.matricula.web.domain.StatusTurma;
import br.com.matricula.web.domain.Turma;
import br.com.matricula.web.repository.CursoRepository;
import br.com.matricula.web.repository.DisciplinaRepository;
import br.com.matricula.web.repository.MatriculaRepository;
import br.com.matricula.web.repository.TurmaRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class DisciplinaControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

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
	}

	@Test
	void deveCriarListarBuscarAtualizarERemoverDisciplina() throws Exception {
		Long cursoId = criarCurso("ES", "Engenharia de Software");

		String payload = """
				{
				  "codDisciplina": "ALG",
				  "nome": "Algoritmos",
				  "cursoId": %d,
				  "ano": 2026,
				  "periodo": 1
				}
				""".formatted(cursoId);

		MvcResult criarResult = mockMvc.perform(post("/disciplinas")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.codDisciplina").value("ALG"))
				.andExpect(jsonPath("$.nome").value("Algoritmos"))
				.andExpect(jsonPath("$.cursoId").value(cursoId))
				.andExpect(jsonPath("$.codCurso").value("ES"))
				.andExpect(jsonPath("$.nomeCurso").value("Engenharia de Software"))
				.andExpect(jsonPath("$.ano").value(2026))
				.andExpect(jsonPath("$.periodo").value(1))
				.andReturn();

		Long id = extrairId(criarResult);

		mockMvc.perform(get("/disciplinas").param("page", "0").param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(id))
				.andExpect(jsonPath("$.content[0].codDisciplina").value("ALG"))
				.andExpect(jsonPath("$.content[0].codCurso").value("ES"))
				.andExpect(jsonPath("$.content[0].nomeCurso").value("Engenharia de Software"))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(10))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.totalPages").value(1))
				.andExpect(jsonPath("$.first").value(true))
				.andExpect(jsonPath("$.last").value(true));

		mockMvc.perform(get("/disciplinas/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Algoritmos"))
				.andExpect(jsonPath("$.codCurso").value("ES"))
				.andExpect(jsonPath("$.nomeCurso").value("Engenharia de Software"));

		String atualizacao = """
				{
				  "codDisciplina": "ED",
				  "nome": "Estruturas de Dados",
				  "cursoId": %d,
				  "ano": 2027,
				  "periodo": 2
				}
				""".formatted(cursoId);

		mockMvc.perform(put("/disciplinas/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content(atualizacao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.codDisciplina").value("ED"))
				.andExpect(jsonPath("$.nome").value("Estruturas de Dados"))
				.andExpect(jsonPath("$.ano").value(2027))
				.andExpect(jsonPath("$.periodo").value(2));

		mockMvc.perform(delete("/disciplinas/{id}", id))
				.andExpect(status().isNoContent());

		assertThat(disciplinaRepository.findById(id)).isEmpty();
	}

	@Test
	void deveRetornar400_quandoCampoObrigatorioAusente() throws Exception {
		Long cursoId = criarCurso("ES", "Engenharia de Software");

		String payload = """
				{
				  "codDisciplina": "ALG",
				  "nome": "",
				  "cursoId": %d,
				  "ano": 2026,
				  "periodo": 1
				}
				""".formatted(cursoId);

		mockMvc.perform(post("/disciplinas")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.codigo").value("VALIDACAO_FALHOU"))
				.andExpect(jsonPath("$.detalhes").isArray())
				.andExpect(jsonPath("$.detalhes", hasItem(containsString("nome"))));
	}

	@Test
	void deveRetornar404_quandoCursoIdInexistente() throws Exception {
		Long cursoIdInexistente = 999L;

		String payload = """
				{
				  "codDisciplina": "ALG",
				  "nome": "Algoritmos",
				  "cursoId": %d,
				  "ano": 2026,
				  "periodo": 1
				}
				""".formatted(cursoIdInexistente);

		mockMvc.perform(post("/disciplinas")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.codigo").value("ENTIDADE_NAO_ENCONTRADA"));
	}

	@Test
	void deveRetornar404_quandoDisciplinaNaoExiste() throws Exception {
		Long idInexistente = 999L;

		mockMvc.perform(get("/disciplinas/{id}", idInexistente))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.codigo").value("ENTIDADE_NAO_ENCONTRADA"));
	}

	@Test
	void deveRetornar409_quandoExcluirDisciplinaComTurmaVinculada() throws Exception {
		Long cursoId = criarCurso("ES", "Engenharia de Software");
		Long disciplinaId = criarDisciplina(cursoId, "ALG", "Algoritmos");

		Turma turma = new Turma();
		turma.setCodTurma("T01");
		turma.setDisciplinaId(disciplinaId);
		turma.setVagasTotais(30);
		turma.setVagasOcupadas(0);
		turma.setStatus(StatusTurma.ABERTA);
		turmaRepository.save(turma);

		mockMvc.perform(delete("/disciplinas/{id}", disciplinaId))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.codigo").value("EXCLUSAO_BLOQUEADA_VINCULO_ATIVO"));

		assertThat(disciplinaRepository.findById(disciplinaId)).isPresent();
	}

	private Long criarCurso(String codCurso, String nome) throws Exception {
		String payload = """
				{
				  "codCurso": "%s",
				  "nome": "%s",
				  "descricao": "Descrição"
				}
				""".formatted(codCurso, nome);

		MvcResult result = mockMvc.perform(post("/cursos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isCreated())
				.andReturn();

		return extrairId(result);
	}

	private Long criarDisciplina(Long cursoId, String codDisciplina, String nome) throws Exception {
		String payload = """
				{
				  "codDisciplina": "%s",
				  "nome": "%s",
				  "cursoId": %d,
				  "ano": 2026,
				  "periodo": 1
				}
				""".formatted(codDisciplina, nome, cursoId);

		MvcResult result = mockMvc.perform(post("/disciplinas")
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
