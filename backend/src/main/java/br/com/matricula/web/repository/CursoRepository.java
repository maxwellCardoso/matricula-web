package br.com.matricula.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.matricula.web.domain.Curso;

public interface CursoRepository extends JpaRepository<Curso, Long> {

	boolean existsByCodCurso(String codCurso);

	boolean existsByCodCursoAndIdNot(String codCurso, Long id);
}
