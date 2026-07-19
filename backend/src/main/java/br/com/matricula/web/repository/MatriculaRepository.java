package br.com.matricula.web.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.matricula.web.domain.Matricula;
import br.com.matricula.web.domain.StatusMatricula;
import br.com.matricula.web.dto.response.MatriculaResponseDTO;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

	boolean existsByTurmaId(Long turmaId);

	boolean existsByAlunoIdAndTurmaIdAndStatusNot(Long alunoId, Long turmaId, StatusMatricula status);

	@Query("""
			SELECT new br.com.matricula.web.dto.response.MatriculaResponseDTO(
				m.id,
				m.alunoId,
				a.nome,
				a.email,
				a.cpf,
				m.turmaId,
				t.codTurma,
				m.status,
				m.dataCriacao,
				m.dataAtualizacao)
			FROM Matricula m
			JOIN Aluno a ON a.id = m.alunoId
			JOIN Turma t ON t.id = m.turmaId
			WHERE m.id = :id
			""")
	Optional<MatriculaResponseDTO> findDetalhadaById(@Param("id") Long id);

	@Query("""
			SELECT new br.com.matricula.web.dto.response.MatriculaResponseDTO(
				m.id,
				m.alunoId,
				a.nome,
				a.email,
				a.cpf,
				m.turmaId,
				t.codTurma,
				m.status,
				m.dataCriacao,
				m.dataAtualizacao)
			FROM Matricula m
			JOIN Aluno a ON a.id = m.alunoId
			JOIN Turma t ON t.id = m.turmaId
			WHERE (:alunoId IS NULL OR m.alunoId = :alunoId)
			  AND (:turmaId IS NULL OR m.turmaId = :turmaId)
			""")
	List<MatriculaResponseDTO> findDetalhadas(
			@Param("alunoId") Long alunoId,
			@Param("turmaId") Long turmaId);
}
