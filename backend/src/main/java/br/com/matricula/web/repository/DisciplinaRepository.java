package br.com.matricula.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.matricula.web.domain.Disciplina;

public interface DisciplinaRepository extends JpaRepository<Disciplina, Long> {

	boolean existsByCursoId(Long cursoId);

	boolean existsByCodDisciplina(String codDisciplina);

	boolean existsByCodDisciplinaAndIdNot(String codDisciplina, Long id);
}
