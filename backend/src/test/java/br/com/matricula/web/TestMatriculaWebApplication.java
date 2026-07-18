package br.com.matricula.web;

import org.springframework.boot.SpringApplication;

public class TestMatriculaWebApplication {

	public static void main(String[] args) {
		SpringApplication.from(MatriculaWebApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
