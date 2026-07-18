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
import br.com.matricula.web.repository.AlunoRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AlunoControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AlunoRepository alunoRepository;

	@BeforeEach
	void setUp() {
		alunoRepository.deleteAll();
	}

	@Test
	void deveCriarListarBuscarAtualizarERemoverAluno() throws Exception {
		String payload = """
				{
				  "nome": "João Silva",
				  "email": "joao@email.com",
				  "cpf": "12345678901",
				  "endereco": "Rua A, 100"
				}
				""";

		MvcResult criarResult = mockMvc.perform(post("/alunos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.nome").value("João Silva"))
				.andExpect(jsonPath("$.email").value("joao@email.com"))
				.andExpect(jsonPath("$.cpf").value("12345678901"))
				.andExpect(jsonPath("$.endereco").value("Rua A, 100"))
				.andReturn();

		Long id = extrairId(criarResult);

		mockMvc.perform(get("/alunos").param("page", "0").param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(id))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(10))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.totalPages").value(1))
				.andExpect(jsonPath("$.first").value(true))
				.andExpect(jsonPath("$.last").value(true));

		mockMvc.perform(get("/alunos/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("joao@email.com"));

		String atualizacao = """
				{
				  "nome": "João Silva Atualizado",
				  "email": "joao.atualizado@email.com",
				  "cpf": "12345678901",
				  "endereco": "Rua B, 200"
				}
				""";

		mockMvc.perform(put("/alunos/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content(atualizacao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("João Silva Atualizado"))
				.andExpect(jsonPath("$.email").value("joao.atualizado@email.com"))
				.andExpect(jsonPath("$.endereco").value("Rua B, 200"));

		mockMvc.perform(delete("/alunos/{id}", id))
				.andExpect(status().isNoContent());

		assertThat(alunoRepository.findById(id)).isEmpty();
	}

	@Test
	void deveRetornar400_quandoCamposObrigatoriosAusentes() throws Exception {
		String payload = """
				{
				  "nome": "",
				  "email": "",
				  "cpf": ""
				}
				""";

		mockMvc.perform(post("/alunos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.codigo").value("VALIDACAO_FALHOU"))
				.andExpect(jsonPath("$.detalhes").isArray())
				.andExpect(jsonPath("$.detalhes", hasItem(containsString("nome"))))
				.andExpect(jsonPath("$.detalhes", hasItem(containsString("email"))))
				.andExpect(jsonPath("$.detalhes", hasItem(containsString("cpf"))));
	}

	@Test
	void deveRetornar409_quandoEmailDuplicado() throws Exception {
		criarAluno("João Silva", "joao@email.com", "12345678901");

		String payload = """
				{
				  "nome": "Outro Aluno",
				  "email": "joao@email.com",
				  "cpf": "98765432100"
				}
				""";

		mockMvc.perform(post("/alunos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.codigo").value("EMAIL_DUPLICADO"));
	}

	@Test
	void deveRetornar409_quandoCpfDuplicado() throws Exception {
		criarAluno("João Silva", "joao@email.com", "12345678901");

		String payload = """
				{
				  "nome": "Outro Aluno",
				  "email": "outro@email.com",
				  "cpf": "12345678901"
				}
				""";

		mockMvc.perform(post("/alunos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.codigo").value("CPF_DUPLICADO"));
	}

	@Test
	void deveRetornar404_quandoAlunoNaoExiste() throws Exception {
		Long idInexistente = 999L;

		mockMvc.perform(get("/alunos/{id}", idInexistente))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.codigo").value("ENTIDADE_NAO_ENCONTRADA"));
	}

	private void criarAluno(String nome, String email, String cpf) throws Exception {
		String payload = """
				{
				  "nome": "%s",
				  "email": "%s",
				  "cpf": "%s"
				}
				""".formatted(nome, email, cpf);

		mockMvc.perform(post("/alunos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isCreated());
	}

	private Long extrairId(MvcResult result) throws Exception {
		String body = result.getResponse().getContentAsString();
		Number id = com.jayway.jsonpath.JsonPath.read(body, "$.id");
		return id.longValue();
	}
}
