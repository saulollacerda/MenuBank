# Coding Guidelines

1. All code in English — class names, variables, methods, endpoints, comments, commit messages.
2. All UI text in Portuguese (pt-BR) — labels, buttons, messages, tooltips, error messages.
3. RESTful API design — proper HTTP methods and status codes.
4. DTOs for API communication — never expose JPA entities in REST responses.
5. TypeScript strict mode — proper types and interfaces, avoid `any`.
6. Test coverage — unit tests for services (backend) and components (frontend).

## TDD — strictly enforced

**NEVER write implementation code before the test exists. No exceptions.**

Cycle: 🔴 Red (failing test) → 🟢 Green (minimal implementation) → 🔵 Refactor.

- Stub classes (`throw new UnsupportedOperationException`) are the only production code allowed before a test.
- If asked to implement a feature, always create the test file first and get explicit confirmation (or follow the TDD cycle within the same response).

### Backend

- **Unit:** JUnit 5 (Jupiter) + Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`, BDD `given/then` style).
- **Controller:** `@WebMvcTest` + `MockMvc` + `@MockitoBean`.

### Frontend

- **Unit:** Vitest + `@vue/test-utils`.