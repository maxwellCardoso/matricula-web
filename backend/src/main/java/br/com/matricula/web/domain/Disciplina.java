package br.com.matricula.web.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "disciplina")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Disciplina {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	@Column(name = "cod_disciplina", nullable = false, unique = true, length = 50)
	private String codDisciplina;

	@Column(nullable = false)
	private String nome;

	@Column(name = "curso_id", nullable = false)
	private Long cursoId;

	@Column(nullable = false)
	private Integer ano;

	@Column(nullable = false)
	private Integer periodo;
}
