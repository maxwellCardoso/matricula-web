ALTER TABLE curso
    ADD COLUMN cod_curso VARCHAR(50) NOT NULL;

CREATE UNIQUE INDEX uk_curso_cod_curso ON curso (cod_curso);

ALTER TABLE disciplina
    ADD COLUMN cod_disciplina VARCHAR(50) NOT NULL;

CREATE UNIQUE INDEX uk_disciplina_cod_disciplina ON disciplina (cod_disciplina);

ALTER TABLE turma
    ADD COLUMN cod_turma VARCHAR(50) NOT NULL;

CREATE UNIQUE INDEX uk_turma_cod_turma ON turma (cod_turma);
