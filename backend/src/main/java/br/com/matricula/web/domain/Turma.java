package br.com.matricula.web.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "turma")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Turma {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	@Column(name = "cod_turma", nullable = false, unique = true, length = 50)
	private String codTurma;

	@Column(name = "disciplina_id", nullable = false)
	private Long disciplinaId;

	@Column(name = "vagas_totais", nullable = false)
	private Integer vagasTotais;

	@Column(name = "vagas_ocupadas", nullable = false)
	private Integer vagasOcupadas = 0;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private StatusTurma status = StatusTurma.ABERTA;
}
