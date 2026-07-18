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
import br.com.matricula.web.domain.Disciplina;
import br.com.matricula.web.repository.CursoRepository;
import br.com.matricula.web.repository.DisciplinaRepository;
import br.com.matricula.web.repository.MatriculaRepository;
import br.com.matricula.web.repository.TurmaRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CursoControllerIntegrationTest {

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
	void deveCriarListarBuscarAtualizarERemoverCurso() throws Exception {
		String payload = """
				{
				  "codCurso": "ES",
				  "nome": "Engenharia de Software",
				  "descricao": "Formação em desenvolvimento de sistemas"
				}
				""";

		MvcResult criarResult = mockMvc.perform(post("/cursos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.codCurso").value("ES"))
				.andExpect(jsonPath("$.nome").value("Engenharia de Software"))
				.andExpect(jsonPath("$.descricao").value("Formação em desenvolvimento de sistemas"))
				.andReturn();

		Long id = extrairId(criarResult);

		mockMvc.perform(get("/cursos").param("page", "0").param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(id))
				.andExpect(jsonPath("$.content[0].codCurso").value("ES"))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(10))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.totalPages").value(1))
				.andExpect(jsonPath("$.first").value(true))
				.andExpect(jsonPath("$.last").value(true));

		mockMvc.perform(get("/cursos/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.codCurso").value("ES"))
				.andExpect(jsonPath("$.nome").value("Engenharia de Software"));

		String atualizacao = """
				{
				  "codCurso": "CC",
				  "nome": "Ciência da Computação",
				  "descricao": "Curso atualizado"
				}
				""";

		mockMvc.perform(put("/cursos/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content(atualizacao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.codCurso").value("CC"))
				.andExpect(jsonPath("$.nome").value("Ciência da Computação"))
				.andExpect(jsonPath("$.descricao").value("Curso atualizado"));

		mockMvc.perform(delete("/cursos/{id}", id))
				.andExpect(status().isNoContent());

		assertThat(cursoRepository.findById(id)).isEmpty();
	}

	@Test
	void deveRetornar400_quandoNomeAusente() throws Exception {
		String payload = """
				{
				  "codCurso": "ES",
				  "nome": "",
				  "descricao": "Sem nome"
				}
				""";

		mockMvc.perform(post("/cursos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.codigo").value("VALIDACAO_FALHOU"))
				.andExpect(jsonPath("$.detalhes").isArray())
				.andExpect(jsonPath("$.detalhes", hasItem(containsString("nome"))));
	}

	@Test
	void deveRetornar404_quandoCursoNaoExiste() throws Exception {
		Long idInexistente = 999L;

		mockMvc.perform(get("/cursos/{id}", idInexistente))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.codigo").value("ENTIDADE_NAO_ENCONTRADA"));
	}

	@Test
	void deveRetornar409_quandoExcluirCursoComDisciplinaVinculada() throws Exception {
		Long cursoId = criarCurso("ES", "Engenharia de Software", "Descrição");

		Disciplina disciplina = new Disciplina();
		disciplina.setCodDisciplina("ALG");
		disciplina.setNome("Algoritmos");
		disciplina.setCursoId(cursoId);
		disciplina.setAno(2026);
		disciplina.setPeriodo(1);
		disciplinaRepository.save(disciplina);

		mockMvc.perform(delete("/cursos/{id}", cursoId))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.codigo").value("EXCLUSAO_BLOQUEADA_VINCULO_ATIVO"));

		assertThat(cursoRepository.findById(cursoId)).isPresent();
	}

	private Long criarCurso(String codCurso, String nome, String descricao) throws Exception {
		String payload = """
				{
				  "codCurso": "%s",
				  "nome": "%s",
				  "descricao": "%s"
				}
				""".formatted(codCurso, nome, descricao);

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
