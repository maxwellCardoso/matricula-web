package br.com.matricula.web.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
		name = "matricula",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_matricula_aluno_turma",
				columnNames = { "aluno_id", "turma_id" }))
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Matricula {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	@Column(name = "aluno_id", nullable = false)
	private Long alunoId;

	@Column(name = "turma_id", nullable = false)
	private Long turmaId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private StatusMatricula status = StatusMatricula.PENDENTE;

	@Column(name = "data_criacao", nullable = false, updatable = false)
	private LocalDateTime dataCriacao;

	@Column(name = "data_atualizacao", nullable = false)
	private LocalDateTime dataAtualizacao;

	@PrePersist
	void onCreate() {
		LocalDateTime agora = LocalDateTime.now();
		dataCriacao = agora;
		dataAtualizacao = agora;
		if (status == null) {
			status = StatusMatricula.PENDENTE;
		}
	}

	@PreUpdate
	void onUpdate() {
		dataAtualizacao = LocalDateTime.now();
	}
}
