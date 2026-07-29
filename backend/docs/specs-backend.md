# Matrícula Web — Sistema de Gestão de Matrículas Acadêmicas

> Documento de referência para desenvolvimento assistido por IA (Cursor).
> Objetivo: servir de contexto único para geração de código consistente.

---

## 1. Stack

- **Backend**: Java 17 (LTS) + Spring Boot (Web, Data JPA, Validation, Flyway)
- **Banco**: PostgreSQL
- **Migrations**: Flyway
- **Frontend**: Angular
- **Ambiente**: Docker Compose
- **Docs API**: springdoc-openapi (Swagger)
- **Testes**: JUnit 5 + Mockito (unitários) + Testcontainers 

---

## 2. Entidades

### Aluno
| Campo | Tipo | Regras |
|---|---|---|
| id | Long | PK |
| nome | String | obrigatório |
| email | String | obrigatório, único |
| cpf | String | obrigatório, único |
| endereco | String | opcional |

### Curso
| Campo | Tipo | Regras |
|---|---|---|
| id | Long | PK |
| codCurso | String | obrigatório, único |
| nome | String | obrigatório |
| descricao | String | opcional |

### Disciplina
| Campo | Tipo | Regras |
|---|---|---|
| id | Long | PK |
| codDisciplina | String | obrigatório, único |
| nome | String | obrigatório |
| cursoId | FK -> Curso | obrigatório |
| ano | Integer | obrigatório |
| periodo | Integer | obrigatório |

> **Listagem**: inclui também `codCurso` e `nomeCurso` do curso vinculado.

### Turma
| Campo | Tipo | Regras |
|---|---|---|
| id | Long | PK |
| codTurma | String | obrigatório, único |
| disciplinaId | FK -> Disciplina | obrigatório |
| vagasTotais | Integer | obrigatório, > 0 |
| vagasOcupadas | Integer | default 0, nunca > vagasTotais |
| status | Enum: ABERTA, FECHADA | default ABERTA |

> **Listagem**: inclui também `codDisciplina` e `nomeDisciplina` da disciplina vinculada.

### Matrícula
| Campo | Tipo | Regras |
|---|---|---|
| id | Long | PK |
| alunoId | FK -> Aluno | obrigatório |
| turmaId | FK -> Turma | obrigatório |
| status | Enum: PENDENTE, CONFIRMADA, CANCELADA | default PENDENTE |
| dataCriacao | Timestamp | auto |
| dataAtualizacao | Timestamp | auto |

> **Listagem**: inclui também `nomeAluno`, `emailAluno`, `cpfAluno` e `codTurma`.

**Constraint única**: `(alunoId, turmaId)` — uma linha por par aluno/turma.
Duplicidade de matrícula **ativa** (status ≠ CANCELADA) é bloqueada na aplicação.
Se existir matrícula CANCELADA para o par, uma nova solicitação **reativa** o registro
(status → PENDENTE), sem violar a constraint.

---

## 3. Regras de negócio (User Stories)

Formato: **Dado / Quando / Então** — usar exatamente estes casos como
cenários de teste. Cada história é referenciável no Cursor pelo código
(ex: `@specs.md US05`).

### US00.1 — CRUD de Aluno
   Dado dados válidos de aluno (nome, email único, cpf único)
   Quando o usuário cria, edita, lista ou remove um aluno
   Então a operação reflete no banco; email/cpf duplicado retorna 409;
   campo obrigatório ausente retorna 400 (ver seção 2); a listagem é
   paginada (ver seção 5.1)

### US00.2 — CRUD de Curso
   Dado dados válidos de curso (nome)
   Quando o usuário cria, edita, lista ou remove um curso
   Então a operação reflete no banco; campo obrigatório ausente retorna 400;
   exclusão de curso com disciplinas vinculadas é bloqueada (ver nota
   abaixo)

### US00.3 — CRUD de Disciplina
   Dado dados válidos de disciplina (nome, cursoId existente, ano, período)
   Quando o usuário cria, edita, lista ou remove uma disciplina
   Então a operação reflete no banco; cursoId inexistente retorna 404;
   campo obrigatório ausente retorna 400; exclusão de disciplina com
   turmas vinculadas é bloqueada (ver nota abaixo)

### US00.4 — CRUD de Turma
   Dado dados válidos de turma (disciplinaId existente, vagasTotais > 0)
   Quando o usuário cria, edita, lista ou remove uma turma
   Então a operação reflete no banco; disciplinaId inexistente retorna 404;
   vagasTotais <= 0 retorna 400; turma é criada com status ABERTA por padrão
   e `vagasOcupadas = 0`; exclusão de turma com matrículas vinculadas é
   bloqueada (ver nota abaixo)

> **Regra de integridade**: exclusão de Curso, Disciplina ou Turma que
> possua vínculos ativos abaixo dela na hierarquia (Curso → Disciplina →
> Turma → Matrícula) não é permitida. Exemplo: uma turma com matrículas
> não pode ser excluída. Tentativa de exclusão nessas condições retorna
> erro 409 com código específico (ex: `EXCLUSAO_BLOQUEADA_VINCULO_ATIVO`).

### US01 — Matricular em turma aberta com vaga
   Dado uma turma ABERTA com `vagasOcupadas < vagasTotais`
   Quando um aluno solicita matrícula
   Então a matrícula é criada com status PENDENTE

### US02 — Matricular em turma fechada
   Dado uma turma com status FECHADA
   Quando um aluno solicita matrícula
   Então retorna erro 422/409 com mensagem clara — matrícula não é criada

### US03 — Matricular em turma sem vaga
   Dado `vagasOcupadas == vagasTotais`
   Quando um aluno tenta confirmar matrícula
   Então retorna erro específico (ex: `VAGA_INDISPONIVEL`) — nenhuma vaga é consumida

### US04 — Matrícula duplicada
   Dado que o aluno já possui matrícula (qualquer status ≠ CANCELADA) na turma
   Quando tenta se matricular novamente na mesma turma
   Então retorna erro específico (ex: `MATRICULA_DUPLICADA`)

### US05 — Confirmar matrícula
   Dado uma matrícula PENDENTE e vaga disponível
   Quando confirmada
   Então status vira CONFIRMADA **e** `vagasOcupadas` é incrementado — operação atômica

### US06 — Cancelar matrícula confirmada
   Dado uma matrícula CONFIRMADA
   Quando cancelada
   Então status vira CANCELADA **e** `vagasOcupadas` é decrementado — operação atômica

### US07 — Cancelar matrícula já cancelada
   Dado uma matrícula CANCELADA
   Quando se tenta cancelar novamente
   Então retorna erro (operação idempotente negada, não silenciosa)

### US08 — Concorrência na última vaga
   Dado `vagasOcupadas == vagasTotais - 1`
   Quando duas confirmações simultâneas chegam
   Então apenas uma é bem-sucedida; a outra recebe `VAGA_INDISPONIVEL`

### US09 — Consulta de matrículas por aluno
   Dado um alunoId
   Então retorna todas as matrículas desse aluno com status atual

### US10 — Consulta de matrículas por turma
   Dado um turmaId
   Então retorna todas as matrículas dessa turma com status atual

---

## 4. Proteção da regra de vagas 
- Confirmação e cancelamento são `@Transactional`.
- Confirmação usa `UPDATE turma SET vagas_ocupadas = vagas_ocupadas + 1
  WHERE id = :id AND vagas_ocupadas < vagas_totais`, checando linhas
  afetadas antes de marcar a matrícula como CONFIRMADA.

---

## 5. Endpoints (visão geral)

```
POST   /alunos
GET    /alunos?page=0&size=10&sort=nome,asc
GET    /alunos/{id}
PUT    /alunos/{id}
DELETE /alunos/{id}

POST   /cursos ... (CRUD análogo, listagem paginada)
POST   /disciplinas ... (CRUD análogo, listagem paginada)
POST   /turmas ... (CRUD análogo, listagem paginada)

POST   /matriculas                     -> cria PENDENTE
PATCH  /matriculas/{id}/confirmar      -> consome vaga
PATCH  /matriculas/{id}/cancelar       -> libera vaga (se aplicável)
GET    /matriculas?alunoId=...
GET    /matriculas?turmaId=...
```

Todas as respostas de erro seguem um formato padronizado (código, mensagem,
timestamp, detalhes de validação quando aplicável) via `@ControllerAdvice`.

### 5.1 Paginação nas listagens

Todas as listagens de CRUD (`GET /alunos`, `GET /cursos`, `GET /disciplinas`,
`GET /turmas`) usam paginação no backend (Spring Data `Pageable` +
`findAll(Pageable)`). O frontend **não** deve carregar a lista inteira para
paginar localmente.

**Query params** (padrão Spring Data):

| Param | Tipo | Default | Descrição |
|---|---|---|---|
| `page` | int | `0` | Índice da página (base 0) |
| `size` | int | `10` | Quantidade de itens por página |
| `sort` | string | `nome,asc` | Campo e direção (`campo,asc` ou `campo,desc`) |

No Swagger UI, use `sort=nome` ou `sort=nome,asc`. Não deixe o placeholder
`string` — isso gera erro de ordenação.

**Formato da resposta** (`PageResponseDTO`):

```json
{
  "content": [ { "...": "itens da página" } ],
  "page": 0,
  "size": 10,
  "totalElements": 42,
  "totalPages": 5,
  "first": true,
  "last": false
}
```

---
## 6. Casos de erro/borda a cobrir em teste

- Payload inválido (campos obrigatórios ausentes) → 400 com detalhe por campo
- Entidade referenciada inexistente (ex: `alunoId` que não existe) → 404
- Matrícula duplicada → 409
- Vaga esgotada → 409
- Cancelar matrícula inexistente ou já cancelada → 409/404
- Concorrência na última vaga → apenas uma confirmação vence

---

