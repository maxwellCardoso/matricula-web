# Matrícula Web - Sistema de Gestão de Matrículas Acadêmicas

Sistema de gestão de matrículas acadêmicas.

- **Backend**: Java 17 + Spring Boot (Web, Data JPA, Validation, Flyway)
- **Banco**: PostgreSQL 16
- **Frontend**: Angular 22 
- **Ambiente**: Docker Compose

---

## Pré-requisitos

| Ferramenta | Versão | Uso |
|---|---|---|
| JDK | 17+ | Backend local (`./mvnw`) |
| Docker + Docker Compose | recente | Banco e/ou stack containerizada |
| Node.js | 22.22.3+ ou 24.15.0+ | Frontend local (`npm install`, `npm start`) |
| npm | 11+ | Instalação de dependências do frontend |

Garanta que as portas **5432** (Postgres), **8080** (API) e **4200** (frontend) estejam livres.

### Credenciais do banco (dev)

| Campo | Valor |
|---|---|
| Host | `localhost` (fora do Docker) / `db` (entre containers) |
| Port | `5432` |
| Database | `matriculas` |
| User | `postgres` |
| Password | `postgres` |

---

## Estrutura

```
matricula-web/
├── backend/           # Spring Boot + Dockerfile
├── frontend/          # Angular 
├── docs/              # specs e guidelines
└── docker-compose.yml # db + backend + frontend
```

---

## Subida individual (Para Desenvolvimento)

Ideal para trabalhar em backend ou frontend separadamente, com hot reload.

### 1. Subir só o Postgres

Na raiz do repositório:

```bash
docker compose up db -d
```

### 2. Subir o backend localmente
Subir API + banco pelos containers (usa o `backend/Dockerfile`):

```bash
docker compose up --build db backend
```

Nesse modo as variáveis do Compose apontam o datasource para `db:5432`.

App em [http://localhost:8080](http://localhost:8080).

### 3. Frontend local

Com a API rodando em `:8080` (passo 2 ou `./mvnw spring-boot:run` com o Postgres do passo 1):

```bash
cd frontend
npm install
npm start
```

App em [http://localhost:4200](http://localhost:4200).

O Angular CLI vem como dependência do projeto — não é necessário instalar `@angular/cli` globalmente.

---

## Subida simultânea (frontend + backend)

Na raiz do repositório:

```bash
docker compose up --build
```

Sobe:

| Serviço | Porta |
|---|---|
| Postgres (`db`) | `5432` |
| Backend | `8080` |
| Frontend | `4200` |


Para parar:

```bash
docker compose down
```

Para limpar também o volume do banco:

```bash
docker compose down -v
```

---

## Testes automatizados

Os testes ficam no **backend**. O frontend não exige suite de testes unitários neste projeto (ver `frontend/docs/guidelines.md`).

### Pré-requisitos

- **JDK 17+**
- **Docker** em execução — os testes de integração sobem PostgreSQL via **Testcontainers**

Não é necessário subir o `docker compose` manualmente para rodar os testes; o Testcontainers provisiona o banco durante a suíte.

### Executar todos os testes

Na pasta `backend/`:

```bash
./mvnw test
```

Inclui testes **unitários** (`*ServiceTest`, com Mockito) e de **integração** (`*IntegrationTest`, API + banco real).

### Executar por tipo

```bash
# Só unitários (services)
./mvnw test -Dtest='*ServiceTest'

# Só integração (controllers + Testcontainers)
./mvnw test -Dtest='*IntegrationTest'

# Exemplo: fluxo de matrícula
./mvnw test -Dtest=MatriculaServiceTest,MatriculaControllerIntegrationTest
```

### O que a suíte cobre

| Tipo | Escopo |
|---|---|
| Unitários | Regras de negócio nos services (validações, exceções, caminhos feliz/erro) |
| Integração | CRUDs completos via HTTP; fluxo matricular → confirmar → cancelar |
| Concorrência | Confirmação simultânea na última vaga (`MatriculaControllerIntegrationTest`) |

---

## Swagger / OpenAPI

Com a API rodando (local ou Docker) em `:8080`:

| Recurso | URL |
|---|---|
| **Swagger UI** | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) |
| **OpenAPI (JSON)** | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) |

A documentação é gerada automaticamente pelo **springdoc-openapi** a partir dos controllers. Use a UI para explorar endpoints, payloads e códigos de resposta.

**Dica (listagens paginadas):** use `sort=nome,asc` (ou outro campo válido). Evite placeholders genéricos como `sort=string` — ver `backend/docs/specs-backend.md`.

---

## Uso de ferramentas de IA

O desenvolvimento foi **assistido por IA** com os specs e guidelines como contexto fixo para os prompts.

### Ferramenta

| Ferramenta | Uso |
|---|---|
| **Cursor** | Geração e refino de código (backend e frontend), esboço de testes, ajustes de documentação |

Documentos usados como contexto nos prompts:

- `backend/docs/specs-backend.md` e `backend/docs/guidelines.md`
- `frontend/docs/specs-frontend.md` e `frontend/docs/guidelines.md`

### Onde a IA foi mais utilizada

| Área | Exemplos |
|---|---|
| **Backend — scaffolding** | Entidades JPA, repositories, DTOs, controllers CRUD |
| **Backend — testes** | Estrutura de `*ServiceTest` e `*IntegrationTest`, casos feliz/erro |
| **Frontend — CRUDs** | Listagens, formulários reativos, serviços HTTP, componentes shared |
| **Frontend — layout** | Navegação, alertas, paginação, diálogos de confirmação |

### O que foi revisado manualmente

- **Regras de matrícula** — status (`PENDENTE` / `CONFIRMADA` / `CANCELADA`), duplicidade e turma fechada
- **Proteção de vagas** — `UPDATE` condicional + `@Transactional` em confirmar/cancelar
- **Concorrência** — teste com threads disputando a última vaga
- **Mapeamento de erros** — códigos HTTP e `codigo` no JSON (`409`, `404`, `400` com `detalhes`)
- **Schema do banco** — migration Flyway e constraint `(aluno_id, turma_id)`
- **Fluxos end-to-end** — execução dos testes de integração e validação manual via Swagger
- **UX de matrícula (frontend)** — feedback de erro de negócio, desabilitar botão no submit, badges de status

### Trechos mais críticos 

| Trecho | Motivo |
|---|---|
| `MatriculaService.confirmar()` / `cancelar()` | Transação, incremento/decremento de vagas e mudança de status |
| `TurmaRepository.incrementarVagasOcupadasSeDisponivel()` | Última linha de defesa contra overbooking |
| `MatriculaControllerIntegrationTest` (concorrência) | Garante a regra de vaga sob carga paralela |
| `GlobalExceptionHandler` | Contrato de erro consumido pelo frontend |
| `matricula-form` / `matricula-list` | Tela principal; erros 409 e UX de confirmação/cancelamento |
| `error-handling.interceptor.ts` + `error-messages.ts` | Tradução de `codigo` da API para mensagens na UI |

---

## Documentação

- [backend/docs/specs-backend.md](backend/docs/specs-backend.md) — regras de negócio e contratos da API
- [backend/docs/guidelines.md](backend/docs/guidelines.md) — padrões de arquitetura e código (backend)
- [frontend/docs/specs-frontend.md](frontend/docs/specs-frontend.md) — user stories e contratos de UI
- [frontend/docs/guidelines.md](frontend/docs/guidelines.md) — padrões de arquitetura e código (frontend)
