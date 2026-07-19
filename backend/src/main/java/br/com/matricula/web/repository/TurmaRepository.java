package br.com.matricula.web.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.matricula.web.domain.Turma;
import br.com.matricula.web.dto.response.TurmaResponseDTO;

public interface TurmaRepository extends JpaRepository<Turma, Long> {

	boolean existsByDisciplinaId(Long disciplinaId);

	boolean existsByCodTurma(String codTurma);

	boolean existsByCodTurmaAndIdNot(String codTurma, Long id);

	/**
	 * UPDATE atômico: evita concorrência se duas confirmações chegarem ao mesmo tempo.
	 * find + save não garante isso. Retorna 0 se não houver vaga.
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			UPDATE Turma t
			SET t.vagasOcupadas = t.vagasOcupadas + 1
			WHERE t.id = :id
			  AND t.vagasOcupadas < t.vagasTotais
			""")
	int incrementarVagasOcupadasSeDisponivel(@Param("id") Long id);

	/**
	 * UPDATE atômico: libera vaga no cancelamento de matrícula CONFIRMADA.
	 * Retorna 0 se não houver vaga ocupada para liberar.
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			UPDATE Turma t
			SET t.vagasOcupadas = t.vagasOcupadas - 1
			WHERE t.id = :id
			  AND t.vagasOcupadas > 0
			""")
	int decrementarVagasOcupadas(@Param("id") Long id);

	@Query("""
			SELECT new br.com.matricula.web.dto.response.TurmaResponseDTO(
				t.id,
				t.codTurma,
				t.disciplinaId,
				d.codDisciplina,
				d.nome,
				t.vagasTotais,
				t.vagasOcupadas,
				t.status)
			FROM Turma t
			JOIN Disciplina d ON d.id = t.disciplinaId
			""")
	Page<TurmaResponseDTO> findDetalhadas(Pageable pageable);

	@Query("""
			SELECT new br.com.matricula.web.dto.response.TurmaResponseDTO(
				t.id,
				t.codTurma,
				t.disciplinaId,
				d.codDisciplina,
				d.nome,
				t.vagasTotais,
				t.vagasOcupadas,
				t.status)
			FROM Turma t
			JOIN Disciplina d ON d.id = t.disciplinaId
			WHERE t.id = :id
			""")
	Optional<TurmaResponseDTO> findDetalhadaById(@Param("id") Long id);
}
