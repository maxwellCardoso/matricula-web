# Matrícula Web - Sistema de Gestão de Matrículas Acadêmicas

Sistema de gestão de matrículas acadêmicas.

- **Backend**: Java 17 + Spring Boot (Web, Data JPA, Validation, Flyway)
- **Banco**: PostgreSQL 16
- **Frontend**: Angular (em desenvolvimento)
- **Ambiente**: Docker Compose

---

## Pré-requisitos

| Ferramenta | Versão | Uso |
|---|---|---|
| JDK | 17+ | Backend local (`./mvnw`) |
| Docker + Docker Compose | recente | Banco e/ou stack containerizada |
| Node.js + Angular CLI | LTS / atual | Frontend local (quando existir) |

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

## Subida individual (desenvolvimento)

Ideal para trabalhar no backend com hot reload do Maven/DevTools. O frontend ainda não está disponível.

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

### 4. Frontend local 

```bash
cd frontend
npm install
ng serve
```

App em [http://localhost:4200](http://localhost:4200).

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

## Documentação

- [docs/specs.md](docs/specs.md) — regras de negócio
- [docs/guidelines.md](docs/guidelines.md) — padrões de arquitetura e código
