package br.com.matricula.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.matricula.web.domain.Turma;

public interface TurmaRepository extends JpaRepository<Turma, Long> {

	boolean existsByDisciplinaId(Long disciplinaId);

	boolean existsByCodTurma(String codTurma);

	boolean existsByCodTurmaAndIdNot(String codTurma, Long id);
}
