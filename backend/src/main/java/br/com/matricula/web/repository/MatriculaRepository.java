package br.com.matricula.web.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.matricula.web.domain.Matricula;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

	boolean existsByTurmaId(Long turmaId);

	List<Matricula> findByAlunoId(Long alunoId);

	List<Matricula> findByTurmaId(Long turmaId);

	List<Matricula> findByAlunoIdAndTurmaId(Long alunoId, Long turmaId);
}
