# User Stories — Frontend (Matrícula Web)

> Documento de backlog para desenvolvimento assistido por IA (Cursor).
> Referências obrigatórias: `docs/specs-backend.md`, `frontend/docs/guidelines.md`.

---

## Visão geral


| Item                 | Valor                                                        |
| -------------------- | ------------------------------------------------------------ |
| Stack                | Angular (standalone), TypeScript, HttpClient, Reactive Forms |
| API base (dev local) | `http://localhost:8080`                                      |
| State management     | **não usar** NgRx ou similar                                 |
| Regra de ouro        | Componente **nunca** chama `HttpClient` diretamente          |




### Ordem sugerida de implementação

```
 US00 (infra) →  US01–05 (CRUDs) →  US06–12 (matrículas) →  US13 (layout)
```

Dependências entre CRUDs (hierarquia de dados):

```
Curso → Disciplina → Turma → Aluno -> Matrícula
```

---



## Épico E00 — Infraestrutura compartilhada



### US00 — Configuração base da aplicação

**Como** desenvolvedor frontend  
**Quero** a infraestrutura HTTP, modelos compartilhados e tratamento centralizado de erros  
**Para** que todas as features consumam a API de forma consistente

#### Critérios de aceite

- [ ] `provideHttpClient(withInterceptors([errorHandlingInterceptor]))` registrado em `app.config.ts`
- [ ] Interceptor em `core/interceptors/error-handling.interceptor.ts`
- [ ] Dicionário de mensagens em `core/error-messages.ts` (ou equivalente)
- [ ] Interface `PageResponse<T>` espelhando `PageResponseDTO` do backend
- [ ] Interface `ErroResponse` espelhando `ErroResponseDTO` do backend
- [ ] Variável/config para URL base da API (ex: `environment.apiUrl = 'http://localhost:8080'`)



#### Especificação técnica

`PageResponse<T>` (espelho exato do backend):

```typescript
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
```

`ErroResponse`:

```typescript
export interface ErroResponse {
  timestamp: string;   // ISO-8601 Instant
  status: number;
  codigo: string;
  mensagem: string;
  detalhes: string[];  // ex: ["nome: é obrigatório", "email: deve ser um e-mail válido"]
}
```

**Parâmetros de paginação** (usar em todos os serviços de listagem):

```typescript
export interface PageParams {
  page?: number;   // default 0
  size?: number;   // default 10
  sort?: string;   // ex: 'nome,asc' | 'nome,desc'
}
```

**Mapeamento** `codigo` **→ mensagem amigável** (mínimo obrigatório):


| codigo                             | Mensagem sugerida na UI                               |
| ---------------------------------- | ----------------------------------------------------- |
| `VALIDACAO_FALHOU`                 | Exibir erros por campo (ver abaixo)                   |
| `ORDENACAO_INVALIDA`               | "Ordenação inválida. Tente outra coluna."             |
| `ENTIDADE_NAO_ENCONTRADA`          | "Registro não encontrado."                            |
| `EMAIL_DUPLICADO`                  | "Já existe um aluno com este e-mail."                 |
| `CPF_DUPLICADO`                    | "Já existe um aluno com este CPF."                    |
| `COD_CURSO_DUPLICADO`              | "Já existe um curso com este código."                 |
| `COD_DISCIPLINA_DUPLICADO`         | "Já existe uma disciplina com este código."           |
| `COD_TURMA_DUPLICADO`              | "Já existe uma turma com este código."                |
| `EXCLUSAO_BLOQUEADA_VINCULO_ATIVO` | Usar `mensagem` da API (já é descritiva)              |
| `TURMA_FECHADA`                    | "Esta turma está fechada para matrículas."            |
| `VAGA_INDISPONIVEL`                | "Não há vagas disponíveis nesta turma."               |
| `MATRICULA_DUPLICADA`              | "Este aluno já possui matrícula ativa nesta turma."   |
| `MATRICULA_JA_CANCELADA`           | "Esta matrícula já foi cancelada."                    |
| `MATRICULA_STATUS_INVALIDO`        | "Somente matrículas pendentes podem ser confirmadas." |
| `INCONSISTENCIA_VAGAS`             | "Erro ao liberar vaga. Tente novamente."              |


**Tratamento de** `VALIDACAO_FALHOU` **(400)**:

- Cada item em `detalhes` segue o formato `"campo: mensagem"`.
- Parsear e aplicar no `FormControl` correspondente via `setErrors({ server: mensagem })`.
- **Não** exibir como toast genérico quando houver detalhes por campo.

**Tratamento de erros de negócio (409)**:

- Interceptor pode mapear mensagem, mas componente decide **onde** renderizar.



#### Componentes shared sugeridos


| Componente               | Caminho                             | Responsabilidade                     |
| ------------------------ | ----------------------------------- | ------------------------------------ |
| `LoadingComponent`       | `shared/components/loading/`        | Spinner durante requisições          |
| `AlertComponent`         | `shared/components/alert/`          | Mensagens de sucesso/erro genéricas  |
| `PaginationComponent`    | `shared/components/pagination/`     | Navegação page/size (reutilizável)   |
| `ConfirmDialogComponent` | `shared/components/confirm-dialog/` | Confirmação antes de DELETE/cancelar |




#### O que evitar

- `catchError` duplicado em cada componente para erros já tratados pelo interceptor
- Recalcular vagas disponíveis no client (`vagasTotais - vagasOcupadas`)

---



## Épico E01 — CRUD de Alunos

> Backend: `@specs-backend.md US00.1`  
> Endpoints: `/alunos`



### US01 — Listar alunos (paginado)

**Como** operador acadêmico  
**Quero** visualizar a lista de alunos com paginação  
**Para** consultar e gerenciar cadastros

#### Critérios de aceite

**Dado** que existem alunos cadastrados  
**Quando** acesso `/alunos`  
**Então** vejo tabela com colunas: nome, email, cpf, endereco (ou "—" se vazio)  
**E** paginação server-side (não carregar lista inteira)  
**E** controles de página anterior/próxima e indicador "Página X de Y"

#### API

```
GET /alunos?page=0&size=10&sort=nome,asc
→ 200 PageResponse<AlunoResponse>
```

**Sort default do backend**: `nome,asc`  
**Campos ordenáveis**: propriedades da entidade (`nome`, `email`, `cpf`, `id`)

#### Arquivos


| Arquivo                                      | Ação                         |
| -------------------------------------------- | ---------------------------- |
| `features/alunos/aluno.model.ts`             | criar interfaces             |
| `core/services/aluno.service.ts`             | implementar `listar(params)` |
| `features/alunos/aluno-list/aluno-list.ts`   | implementar listagem         |
| `features/alunos/aluno-list/aluno-list.html` | template tabela + paginação  |
| `app.routes.ts`                              | rota `/alunos`               |




#### Model

```typescript
// aluno.model.ts
export interface Aluno {
  id: number;
  nome: string;
  email: string;
  cpf: string;
  endereco: string | null;
}

export interface AlunoRequest {
  nome: string;
  email: string;
  cpf: string;      // exatamente 11 dígitos numéricos
  endereco?: string;
}
```



#### UI

- Botão "Novo aluno" → navega para `/alunos/novo`
- Ações por linha: Editar (`/alunos/:id/editar`), Excluir (com confirmação)
- Estado loading enquanto carrega
- Estado vazio: "Nenhum aluno cadastrado"

---



### US02 — Criar e editar aluno

**Como** operador acadêmico  
**Quero** cadastrar e editar alunos via formulário  
**Para** manter dados atualizados

#### Critérios de aceite

**Dado** formulário com dados válidos  
**Quando** submeto criação (`POST`) ou edição (`PUT`)  
**Então** sou redirecionado à listagem com feedback de sucesso  
**E** campos obrigatórios validados no client **e** no server

**Dado** email ou CPF duplicado  
**Quando** submeto  
**Então** exibo erro 409 (`EMAIL_DUPLICADO` / `CPF_DUPLICADO`) no formulário

**Dado** payload inválido (400)  
**Quando** submeto  
**Então** erros aparecem campo a campo

#### API

```
POST /alunos          → 201 AlunoResponse
GET  /alunos/{id}     → 200 AlunoResponse  (pré-preencher edição)
PUT  /alunos/{id}     → 200 AlunoResponse
```



#### Validações client-side (Reactive Forms)


| Campo      | Regras                          |
| ---------- | ------------------------------- |
| `nome`     | required, maxLength(255)        |
| `email`    | required, email, maxLength(255) |
| `cpf`      | required, pattern `/^\d{11}$/`  |
| `endereco` | optional, maxLength(255)        |




#### Arquivos


| Arquivo                                      | Ação                                 |
| -------------------------------------------- | ------------------------------------ |
| `features/alunos/aluno-form/aluno-form.ts`   | formulário create/edit               |
| `features/alunos/aluno-form/aluno-form.html` | template                             |
| `core/services/aluno.service.ts`             | `criar`, `buscarPorId`, `atualizar`  |
| `app.routes.ts`                              | `/alunos/novo`, `/alunos/:id/editar` |




#### Rotas

```typescript
{ path: 'alunos/novo', component: AlunoForm },
{ path: 'alunos/:id/editar', component: AlunoForm },
```

---



### US03 — Excluir aluno

**Como** operador acadêmico  
**Quero** excluir um aluno  
**Para** remover cadastros incorretos

#### Critérios de aceite

**Dado** aluno existente  
**Quando** confirmo exclusão na listagem  
**Então** `DELETE /alunos/{id}` retorna 204  
**E** listagem é recarregada

**Dado** id inexistente  
**Quando** tento excluir  
**Então** erro 404 tratado com mensagem amigável

#### API

```
DELETE /alunos/{id} → 204 No Content
```

---



## Épico  E02 — CRUD de Cursos

> Backend: `@specs-backend.md US00.2`  
> Endpoints: `/cursos`



### US04 — CRUD completo de cursos

**Como** operador acadêmico  
**Quero** gerenciar cursos (listar, criar, editar, excluir)  
**Para** estruturar a oferta acadêmica

#### Critérios de aceite

- [ ] Listagem paginada em `/cursos` (`GET /cursos?page&size&sort=nome,asc`)
- [ ] Formulário create/edit em `/cursos/novo` e `/cursos/:id/editar`
- [ ] Exclusão com confirmação; se 409 `EXCLUSAO_BLOQUEADA_VINCULO_ATIVO`, exibir mensagem da API
- [ ] Código duplicado → 409 `COD_CURSO_DUPLICADO`



#### Model

```typescript
export interface Curso {
  id: number;
  codCurso: string;
  nome: string;
  descricao: string | null;
}

export interface CursoRequest {
  codCurso: string;   // required, max 50
  nome: string;       // required, max 255
  descricao?: string; // optional, max 255
}
```



#### Arquivos (padrão guidelines)

```
features/cursos/curso.model.ts
features/cursos/curso-list/
features/cursos/curso-form/
core/services/curso.service.ts
```



#### API completa

```
POST   /cursos           → 201
GET    /cursos           → 200 PageResponse<Curso>
GET    /cursos/{id}      → 200
PUT    /cursos/{id}      → 200
DELETE /cursos/{id}      → 204
```

---



## Épico  E03 — CRUD de Disciplinas

> Backend: `@specs-backend.md US00.3`  
> Endpoints: `/disciplinas`



### US05 — CRUD completo de disciplinas

**Como** operador acadêmico  
**Quero** gerenciar disciplinas vinculadas a cursos  
**Para** organizar o currículo por ano/período

#### Critérios de aceite

- [ ] Listagem mostra: codDisciplina, nome, codCurso, nomeCurso, ano, periodo
- [ ] Formulário com `<select>` de curso populado via `GET /cursos?size=100` (ou paginação com busca)
- [ ] `cursoId` inexistente → 404 na criação/edição
- [ ] Exclusão bloqueada se houver turmas → 409 `EXCLUSAO_BLOQUEADA_VINCULO_ATIVO`



#### Model

```typescript
export interface Disciplina {
  id: number;
  codDisciplina: string;
  nome: string;
  cursoId: number;
  codCurso: string;
  nomeCurso: string;
  ano: number;
  periodo: number;
}

export interface DisciplinaRequest {
  codDisciplina: string; // required, max 50
  nome: string;          // required, max 255
  cursoId: number;       // required
  ano: number;           // required
  periodo: number;       // required
}
```



#### Dependência

- `CursoService.listar()` para popular dropdown de curso no formulário



#### API

```
POST   /disciplinas      → 201
GET    /disciplinas      → 200 PageResponse<Disciplina>  (sort default: nome,asc)
GET    /disciplinas/{id} → 200
PUT    /disciplinas/{id} → 200
DELETE /disciplinas/{id} → 204
```



#### Rotas

```
/disciplinas
/disciplinas/novo
/disciplinas/:id/editar
```

---



## Épico  E04 — CRUD de Turmas

> Backend: `@specs-backend.md US00.4`  
> Endpoints: `/turmas`



### US06 — CRUD completo de turmas

**Como** operador acadêmico  
**Quero** gerenciar turmas de disciplinas  
**Para** controlar vagas e status de matrícula

#### Critérios de aceite

- [ ] Listagem mostra: codTurma, codDisciplina, nomeDisciplina, vagasOcupadas/vagasTotais, status (badge)
- [ ] Badge de status: `ABERTA` (verde), `FECHADA` (cinza/vermelho)
- [ ] Formulário: codTurma, disciplinaId (select), vagasTotais (> 0)
- [ ] **Não** permitir editar `status`, `vagasOcupadas` no formulário — são gerenciados pelo backend
- [ ] Turma criada pelo backend já vem `status=ABERTA`, `vagasOcupadas=0`
- [ ] Exclusão bloqueada se houver matrículas → 409



#### Model

```typescript
export type StatusTurma = 'ABERTA' | 'FECHADA';

export interface Turma {
  id: number;
  codTurma: string;
  disciplinaId: number;
  codDisciplina: string;
  nomeDisciplina: string;
  vagasTotais: number;
  vagasOcupadas: number;
  status: StatusTurma;
}

export interface TurmaRequest {
  codTurma: string;     // required, max 50
  disciplinaId: number; // required
  vagasTotais: number;  // required, > 0 (Positive no backend)
}
```



#### Exibição de vagas (somente leitura)

```
Vagas: {vagasOcupadas} / {vagasTotais}  ({vagasTotais - vagasOcupadas} disponíveis)
```

> Apenas exibição — **não** usar esse cálculo para decidir se matrícula é permitida; a API decide.



#### API

```
POST   /turmas           → 201
GET    /turmas           → 200 PageResponse<Turma>  (sort default: id,asc)
GET    /turmas/{id}      → 200
PUT    /turmas/{id}      → 200
DELETE /turmas/{id}      → 204
```



#### Dependência

- `DisciplinaService.listar()` para dropdown

---



## Épico  E05 — Matrículas (fluxo principal)

> Backend: `@specs-backend.md US01–US10`  
> Endpoints: `/matriculas`  
> **Atenção especial** conforme `guidelines.md` seção 5



### US07 — Listar matrículas

**Como** operador acadêmico  
**Quero** consultar matrículas com filtros opcionais  
**Para** acompanhar status e tomar ações

#### Critérios de aceite

- [ ] Listagem em `/matriculas` com paginação server-side
- [ ] Colunas: nomeAluno, cpfAluno, codTurma, status (badge), dataCriacao
- [ ] Badge status: `PENDENTE` (amarelo), `CONFIRMADA` (verde), `CANCELADA` (cinza)
- [ ] Filtro opcional por aluno (`?alunoId=`) — select ou autocomplete de alunos
- [ ] Filtro opcional por turma (`?turmaId=`) — select de turmas
- [ ] Ambos filtros podem ser combinados



#### API

```
GET /matriculas?page=0&size=10&sort=id,asc
GET /matriculas?alunoId=1&page=0&size=10
GET /matriculas?turmaId=2&page=0&size=10
GET /matriculas?alunoId=1&turmaId=2&page=0&size=10
→ 200 PageResponse<Matricula>
```



#### Model

```typescript
export type StatusMatricula = 'PENDENTE' | 'CONFIRMADA' | 'CANCELADA';

export interface Matricula {
  id: number;
  alunoId: number;
  nomeAluno: string;
  emailAluno: string;
  cpfAluno: string;
  turmaId: number;
  codTurma: string;
  status: StatusMatricula;
  dataCriacao: string;    // ISO LocalDateTime
  dataAtualizacao: string;
}

export interface MatriculaRequest {
  alunoId: number;
  turmaId: number;
}
```

---



### US08 — Criar matrícula (solicitar)

**Como** operador acadêmico  
**Quero** matricular um aluno em uma turma  
**Para** registrar a intenção de matrícula

> Corresponde a `@specs-backend.md US01`, `US02`, `US03`, `US04`



#### Critérios de aceite

**Dado** turma ABERTA com vaga disponível e aluno sem matrícula ativa na turma  
**Quando** submeto o formulário  
**Então** `POST /matriculas` retorna 201 com `status: PENDENTE`  
**E** sou redirecionado à listagem ou vejo confirmação na tela

**Dado** turma FECHADA (`US02`)  
**Quando** submeto  
**Então** erro 409 `TURMA_FECHADA` exibido claramente na tela (não erro técnico genérico)

**Dado** turma sem vaga (`US03`)  
**Quando** submeto  
**Então** erro 409 `VAGA_INDISPONIVEL`

**Dado** matrícula duplicada (`US04`) — já existe matrícula PENDENTE ou CONFIRMADA para o par
**Quando** submeto
**Então** erro 409 `MATRICULA_DUPLICADA`

**Dado** matrícula anterior CANCELADA para o mesmo aluno e turma
**Quando** submeto nova solicitação
**Então** `POST /matriculas` retorna 201 com `status: PENDENTE` (reativação do registro existente)

**Dado** alunoId ou turmaId inexistente  
**Quando** submeto  
**Então** erro 404

#### UX obrigatória (guidelines §5)

- [ ] Botão submit **desabilitado** durante requisição (evitar duplo clique)
- [ ] Select de aluno e turma com informação contextual:
  - Turma: mostrar `{codTurma} — {nomeDisciplina} — {vagasOcupadas}/{vagasTotais} — {status}`
  - Aluno: `{nome} — {cpf}`



#### API

```
POST /matriculas
Body: { "alunoId": 1, "turmaId": 2 }
→ 201 MatriculaResponse
```



#### Arquivos

```
features/matriculas/matricula.model.ts
features/matriculas/matricula-form/
features/matriculas/matricula-list/
core/services/matricula.service.ts
```



#### Rota

```
/matriculas/novo
```

---



### US09 — Confirmar matrícula

**Como** operador acadêmico  
**Quero** confirmar uma matrícula pendente  
**Para** consumir uma vaga na turma

> Corresponde a `@specs-backend.md US05`, `US08`



#### Critérios de aceite

**Dado** matrícula com status PENDENTE e vaga disponível  
**Quando** clico "Confirmar"  
**Então** `PATCH /matriculas/{id}/confirmar` retorna 200 com `status: CONFIRMADA`  
**E** listagem atualizada (vagas da turma refletem incremento)

**Dado** matrícula não PENDENTE  
**Quando** confirmo  
**Então** erro 409 `MATRICULA_STATUS_INVALIDO`

**Dado** última vaga e confirmação concorrente (`US08`)  
**Quando** confirmo  
**Então** uma requisição vence; a outra recebe 409 `VAGA_INDISPONIVEL` — exibir mensagem clara

#### UX

- [ ] Botão "Confirmar" visível **somente** se `status === 'PENDENTE'`
- [ ] Botão desabilitado durante requisição
- [ ] Confirmação opcional via dialog ("Confirmar matrícula de {nomeAluno} na turma {codTurma}?")



#### API

```
PATCH /matriculas/{id}/confirmar → 200 Matricula
```

---



### US10 — Cancelar matrícula

**Como** operador acadêmico  
**Quero** cancelar uma matrícula pendente ou confirmada  
**Para** liberar a vaga (se confirmada) ou descartar solicitação pendente

> Corresponde a `@specs-backend.md US06`, `US07`



#### Critérios de aceite

**Dado** matrícula CONFIRMADA (`US06`)  
**Quando** cancelo  
**Então** `PATCH /matriculas/{id}/cancelar` retorna 200 com `status: CANCELADA`  
**E** vagas da turma decrementadas (refletido ao recarregar turmas/listagem)

**Dado** matrícula PENDENTE  
**Quando** cancelo  
**Então** status vira CANCELADA (sem alterar vagasOcupadas — backend não decrementa para PENDENTE)

**Dado** matrícula CANCELADA (`US07`)  
**Quando** tento cancelar  
**Então** erro 409 `MATRICULA_JA_CANCELADA`  
**E** botão cancelar **não aparece** na UI

#### UX (guidelines §5)

- [ ] Ação "Cancelar" visível **somente** para `PENDENTE` ou `CONFIRMADA`
- [ ] **Ocultar** botão para `CANCELADA`
- [ ] Dialog de confirmação antes de cancelar
- [ ] Botão desabilitado durante requisição



#### API

```
PATCH /matriculas/{id}/cancelar → 200 Matricula
```

---



### US11 — Consultar matrículas por aluno

**Como** operador acadêmico  
**Quero** ver todas as matrículas de um aluno específico  
**Para** acompanhar histórico acadêmico

> Corresponde a `@specs-backend.md US09`



#### Critérios de aceite

**Dado** aluno selecionado (ex: link na listagem de alunos "Ver matrículas")  
**Quando** navego para `/alunos/:id/matriculas`  
**Então** listo `GET /matriculas?alunoId={id}` com paginação  
**E** exibo nome/cpf do aluno no cabeçalho

#### Implementação sugerida

- Reutilizar `MatriculaList` com `@Input()` ou query param `alunoId`
- Ou rota dedicada que lê `:id` da URL e passa filtro ao serviço

---



### US12 — Consultar matrículas por turma

**Como** operador acadêmico  
**Quero** ver todos os alunos matriculados em uma turma  
**Para** gerenciar a turma

> Corresponde a `@specs-backend.md US10`



#### Critérios de aceite

**Dado** turma selecionada (link na listagem de turmas)  
**Quando** navego para `/turmas/:id/matriculas`  
**Então** listo `GET /matriculas?turmaId={id}`  
**E** exibo codTurma, disciplina e vagas no cabeçalho

---



## Épico  E06 — Layout e navegação



### US13 — Aplicação com menu de navegação

**Como** operador acadêmico  
**Quero** navegar entre as seções do sistema  
**Para** acessar todas as funcionalidades

#### Critérios de aceite

- [ ] Layout com header/nav contendo links: Alunos, Cursos, Disciplinas, Turmas, Matrículas
- [ ] `<router-outlet>` para conteúdo das features
- [ ] Rota default `/` redireciona para `/alunos` (já configurado)
- [ ] Indicação visual da rota ativa
- [ ] Responsivo básico (mobile-friendly)



#### Rotas completas esperadas

```typescript
export const routes: Routes = [
  { path: '', redirectTo: 'alunos', pathMatch: 'full' },

  // Alunos
  { path: 'alunos', component: AlunoList },
  { path: 'alunos/novo', component: AlunoForm },
  { path: 'alunos/:id/editar', component: AlunoForm },
  { path: 'alunos/:id/matriculas', component: MatriculaList }, //  US11

  // Cursos
  { path: 'cursos', component: CursoList },
  { path: 'cursos/novo', component: CursoForm },
  { path: 'cursos/:id/editar', component: CursoForm },

  // Disciplinas
  { path: 'disciplinas', component: DisciplinaList },
  { path: 'disciplinas/novo', component: DisciplinaForm },
  { path: 'disciplinas/:id/editar', component: DisciplinaForm },

  // Turmas
  { path: 'turmas', component: TurmaList },
  { path: 'turmas/novo', component: TurmaForm },
  { path: 'turmas/:id/editar', component: TurmaForm },
  { path: 'turmas/:id/matriculas', component: MatriculaList }, //  US12

  // Matrículas
  { path: 'matriculas', component: MatriculaList },
  { path: 'matriculas/novo', component: MatriculaForm },
];
```

---


