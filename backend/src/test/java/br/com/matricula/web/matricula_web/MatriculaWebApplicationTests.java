package br.com.matricula.web.matricula_web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MatriculaWebApplicationTests {

	@Test
	void contextLoads() {
	}

}
