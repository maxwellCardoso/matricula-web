package br.com.matricula.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.matricula.web.domain.Aluno;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {

	boolean existsByEmail(String email);

	boolean existsByCpf(String cpf);

	boolean existsByEmailAndIdNot(String email, Long id);

	boolean existsByCpfAndIdNot(String cpf, Long id);
}
