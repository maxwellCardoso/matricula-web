# GUIDELINES (Frontend) — Padrões de Desenvolvimento Angular

> Complementa o `specs-frontend.md` (regras de negócio, compartilhado com o backend).
> Este documento define **como** o frontend deve ser estruturado,
> nomeado e organizado. Colar junto com o specs-frontend.md como contexto no Cursor.

---

## 1. Stack

- Angular.
- TypeScript.
- `HttpClient` para consumo da API.
- Formulários reativos (`ReactiveFormsModule`) para telas de
  cadastro/matrícula, pela validação mais explícita.
- Sem necessidade de state management externo (NgRx, etc.) — escopo não
  justifica essa complexidade.

---

## 2. Estrutura de pastas

```
src/app/
├── core/
│   ├── interceptors/       # error-handling.interceptor.ts
│   └── services/            # serviços HTTP por entidade
├── features/
│   ├── alunos/
│   │   ├── aluno-list/
│   │   ├── aluno-form/
│   │   └── aluno.model.ts
│   ├── cursos/
│   ├── disciplinas/
│   ├── turmas/
│   └── matriculas/
│       ├── matricula-list/
│       ├── matricula-form/
│       └── matricula.model.ts
└── shared/
    └── components/          # componentes reutilizáveis (ex: loading, alerta de erro)
```

Regra fixa: **componente nunca chama `HttpClient` diretamente** — sempre
via serviço da pasta `core/services/`.

---

## 3. Convenção de nomes

| Elemento | Padrão | Exemplo |
|---|---|---|
| Serviço HTTP | `{entidade}.service.ts` | `matricula.service.ts` |
| Model/Interface | `{entidade}.model.ts` | `matricula.model.ts` |
| Componente de listagem | `{entidade}-list` | `matricula-list` |
| Componente de formulário | `{entidade}-form` | `matricula-form` |
| Interceptor | `{proposito}.interceptor.ts` | `error-handling.interceptor.ts` |

Os `model.ts` devem espelhar exatamente os `*ResponseDTO`/`*RequestDTO`
do backend (ver `specs-frontend.md`), incluindo os mesmos nomes de campo — evita
mapeamento manual desnecessário.

---

## 4. Tratamento de erros da API

O backend retorna erro padronizado (ver `specs-frontend.md`, seção "Tratamento de
erros"):
```json
{ "status": 409, "codigo": "VAGA_INDISPONIVEL", "mensagem": "...", "detalhes": [] }
```

- Um `HttpInterceptor` único captura toda resposta de erro e centraliza o
  tratamento (não replicar `catchError` em cada componente).
- Mapear `codigo` para mensagens de UI amigáveis em um único lugar
  (ex: um `error-messages.ts` com um dicionário `codigo -> mensagem exibida`).
- Erros de validação (400, com `detalhes` por campo) devem ser exibidos
  no formulário, campo a campo — não como alerta genérico.
- Erros de negócio (409, ex: `VAGA_INDISPONIVEL`, `MATRICULA_DUPLICADA`)
  devem ser exibidos como feedback claro na tela de matrícula, não como
  erro técnico genérico.

---

## 5. Fluxo de matrícula 

- Ao confirmar matrícula, desabilitar o botão durante a requisição
  (evita duplo clique gerando 2 tentativas — mesmo que o backend já
  proteja, a UX deve refletir isso).
- Exibir claramente o status da matrícula (PENDENTE/CONFIRMADA/CANCELADA)
  na listagem, com indicação visual (badge/cor).
- Ação de cancelar só deve aparecer para matrículas CONFIRMADA ou
  PENDENTE (não para já CANCELADA) — espelha a regra 7 do `specs-frontend.md`.

---

## 6. O que evitar

- NgRx ou qualquer state management global — escopo não justifica
- Lógica de negócio duplicada no frontend (ex: recalcular vaga
  disponível no client) — o frontend só reflete o que a API retorna,
  nunca decide a regra.
- Um único componente gigante fazendo listagem + formulário + lógica de
  erro — sempre separar por responsabilidade
- Chamada HTTP direto no componente sem passar pelo serviço

---

## 7. Testes

- Não é necessário implementar testes unitários no frontend para este projeto.
