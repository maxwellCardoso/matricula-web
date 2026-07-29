# GUIDELINES — Padrões de Arquitetura e Desenvolvimento

> Complementa o `specs-backend.md` (regras de negócio). Este documento define
> **como** o código deve ser estruturado, nomeado e testado. 
---

## 1. Estrutura de pacotes

Pacote por camada (adequado ao tamanho do projeto — não usar pacote por
feature/módulo, geraria complexidade desnecessária):

```
br.com.matricula.web
├── controller/       # recebe request, valida DTO, chama service, monta response
├── service/          # regra de negócio, transações (@Transactional)
├── domain/            # entidades JPA (@Entity)
├── repository/        # interfaces Spring Data JPA
├── dto/
│   ├── request/        # *RequestDTO
│   └── response/        # *ResponseDTO
├── exception/          # exceptions customizadas + handler global
└── config/             # configs
```

Regra fixa: **controller nunca acessa repository diretamente**, e
**entidade JPA nunca é serializada direto na resposta HTTP** — sempre via DTO.

---

## 2. Convenção de nomes

| Elemento | Padrão | Exemplo |
|---|---|---|
| Controller | `{Entidade}Controller` | `AlunoController` |
| Service | `{Entidade}Service` | `AlunoService`, `MatriculaService` |
| Repository | `{Entidade}Repository` | `TurmaRepository` |
| Entidade | `{Entidade}` | `Matricula` |
| DTO de entrada | `{Entidade}RequestDTO` (record) | `MatriculaRequestDTO` |
| DTO de saída | `{Entidade}ResponseDTO` (record) | `MatriculaResponseDTO` |
| Exception de negócio | `{Motivo}Exception` | `VagaIndisponivelException` |

---

## 3. Uso do Lombok e records

- **Java 17 (LTS)** — versão definida do projeto, com suporte total a
  `record`.
- **Entidades JPA**: Usar `@Getter` + `@Setter` pontuais, e
  `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` marcando apenas o
  campo `id` com `@EqualsAndHashCode.Include`.
- **DTOs**: usar `record`, não Lombok. Com Java 17 disponível, `record`
  é o padrão do projeto para request/response — imutável, sem
  boilerplate, sem dependência extra.
- Validação de entrada (`@NotNull`, `@Positive`, etc.) funciona
  normalmente em `record` usado como `@RequestBody` com `@Valid`.

Exemplo de DTO:

```java
public record MatriculaResponseDTO(
    UUID id,
    UUID alunoId,
    UUID turmaId,
    StatusMatricula status
) {}
```

- **Exceptions customizadas**: não usar Lombok nem record — construtor
  explícito é mais claro quando a exception carrega o `código` do erro
  além da mensagem.

---

## 4. Tratamento de erros

- Uma exception customizada por regra de negócio violada (não usar
  `RuntimeException` genérica em nenhum ponto do fluxo de matrícula):
  - `VagaIndisponivelException`
  - `MatriculaDuplicadaException`
  - `TurmaFechadaException`
  - `MatriculaJaCanceladaException`
  - `EntidadeNaoEncontradaException` (genérica para 404, reaproveitável)
- Handler único: `@RestControllerAdvice` mapeando cada exception para o
  status HTTP correto (409 para conflitos de regra, 404 para não
  encontrado, 400 para validação de payload via `@Valid`).
- Corpo de erro padronizado, por exemplo:

```json
{
  "timestamp": "...",
  "status": 409,
  "codigo": "VAGA_INDISPONIVEL",
  "mensagem": "Não há vagas disponíveis nesta turma.",
  "detalhes": []
}
```

---

## 5. Estratégia de testes

**Unitários (service, com Mockito mockando repository)**
- Cobrir os 10 casos formais do specs-backend.md (seção 3), item por item
- Cada regra de negócio = pelo menos 1 teste que a viola (caminho de erro)
  e 1 que a cumpre (caminho feliz)
- Nome de teste descritivo: `deveLancarVagaIndisponivel_quandoTurmaSemVaga()`

**Integração (API + banco real via Testcontainers)**
- Fluxo completo: criar aluno → criar turma → matricular → confirmar → cancelar
- Caso de concorrência da última vaga (specs-backend.md US08) —  vale a pena ter um teste dedicado disparando
  confirmações concorrentes (ex: `ExecutorService` com N threads) e
  verificando que só uma teve sucesso.
- Validação de payload inválido (400) e entidade inexistente (404)


---

## 6. Transações

- `@Transactional` nos métodos de `MatriculaService` que confirmam ou
  cancelam (nunca no controller).

---

## 7. Ordem sugerida de geração (para guiar prompts no Cursor)

1. Entidades JPA (`domain/`)
2. Migration Flyway (`V1__create_tables.sql`) — schema deve refletir
   exatamente as entidades, incluindo constraint única `(aluno_id, turma_id)`.
   As migrations devem ser adicionadas na pasta /src/main/resources/db/migration
3. Repositories
4. Services + testes unitários (regra por regra, seguindo specs-backend.md seção 3)
5. DTOs + Controllers
6. `@RestControllerAdvice` + exceptions
7. Testes de integração (fluxo completo + concorrência)
8. Swagger/OpenAPI
9. Docker Compose (banco) → validar subida local
10. Frontend Angular (Após conclusão da API)

Gerar código fora dessa ordem tende a produzir retrabalho — especialmente
pedir controller antes de fechar a regra no service.

---

## 9. O que evitar 

- Múltiplos módulos/serviços, mensageria real, arquitetura distribuída.
- Regras de negócio dentro de controller ou de entidade — sempre no service.
- Complexidade desnecessária.
