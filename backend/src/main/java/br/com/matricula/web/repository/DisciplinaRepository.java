package br.com.matricula.web.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.matricula.web.domain.Disciplina;
import br.com.matricula.web.dto.response.DisciplinaResponseDTO;

public interface DisciplinaRepository extends JpaRepository<Disciplina, Long> {

	boolean existsByCursoId(Long cursoId);

	boolean existsByCodDisciplina(String codDisciplina);

	boolean existsByCodDisciplinaAndIdNot(String codDisciplina, Long id);

	@Query("""
			SELECT new br.com.matricula.web.dto.response.DisciplinaResponseDTO(
				d.id,
				d.codDisciplina,
				d.nome,
				d.cursoId,
				c.codCurso,
				c.nome,
				d.ano,
				d.periodo)
			FROM Disciplina d
			JOIN Curso c ON c.id = d.cursoId
			""")
	Page<DisciplinaResponseDTO> findDetalhadas(Pageable pageable);

	@Query("""
			SELECT new br.com.matricula.web.dto.response.DisciplinaResponseDTO(
				d.id,
				d.codDisciplina,
				d.nome,
				d.cursoId,
				c.codCurso,
				c.nome,
				d.ano,
				d.periodo)
			FROM Disciplina d
			JOIN Curso c ON c.id = d.cursoId
			WHERE d.id = :id
			""")
	Optional<DisciplinaResponseDTO> findDetalhadaById(@Param("id") Long id);
}
