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
@Table(name = "curso")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Curso {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	@Column(name = "cod_curso", nullable = false, unique = true, length = 50)
	private String codCurso;

	@Column(nullable = false)
	private String nome;

	@Column
	private String descricao;
}
