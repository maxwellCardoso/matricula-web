package br.com.matricula.web.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
	void deveListarMatriculasComDadosDoAlunoECodTurma() throws Exception {
		Long alunoId = criarAluno();
		Long turmaId = criarTurma();

		Matricula matricula = new Matricula();
		matricula.setAlunoId(alunoId);
		matricula.setTurmaId(turmaId);
		matricula.setStatus(StatusMatricula.PENDENTE);
		matriculaRepository.save(matricula);

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

	private Long criarTurma() throws Exception {
		Long cursoId = criarCurso();
		Long disciplinaId = criarDisciplina(cursoId);

		String payload = """
				{
				  "codTurma": "T01",
				  "disciplinaId": %d,
				  "vagasTotais": 30
				}
				""".formatted(disciplinaId);

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
