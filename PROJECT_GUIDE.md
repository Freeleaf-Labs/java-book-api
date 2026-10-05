# Book API: Project Guide

How to work on this project: what it is, how to run it, the rules the code follows, and the
concepts behind them. The list of what's left to build lives in **[`TODO.md`](TODO.md)**.

A book-keeping REST API built with Spring Boot, used to learn **layered architecture**,
**OOP**, and **SOLID**. No Java solutions are given here on purpose. You write the code;
this guide explains *how* the project works and *why*.

| Thing | Value |
|---|---|
| Location / package | `book-api/book/` · `np.com.milapmagar.book` |
| Stack | Spring Boot 4.1.1 · Java 25 · Maven wrapper `./mvnw` (no system Maven needed) |
| Database | PostgreSQL 18: `compose.yaml` locally (host port **5434**), Render Postgres in prod |
| Deployment | Docker image on Render, auto-deploys on push to `main` (see `DOCKER_DEPLOY.md`) |
| Base URL | `/api/v1` |

---

## 1. Status

*Last updated: 2026-09-30.*

**Live endpoints:** `GET /`, `GET /actuator/health`, `GET /api/v1/books`, `GET /api/v1/books/{id}`

**✅ Done so far**
- [x] **Baseline:** app starts on 8080, `/actuator/health` → `UP`
- [x] **Packages:** `controller`, `services`, `repository`, `model`, `dto`, `exception`
- [x] **Containerise & deploy:** multi-stage `Dockerfile`, `compose.yaml`, env-var driven config, live on Render
- [x] **Entity:** `Book` with private fields, `equals`/`hashCode` on `id`
- [x] **Repository:** `BookRepository extends JpaRepository<Book, Long>` with derived queries
- [x] **DTOs:** `BookRequestDto` (no `id`) and `BookResponseDto` (with `id`) as records
- [x] **First reads:** `BookService` + `BookController` serving list and by-id as DTOs

**▶ Next:** [`TODO.md`](TODO.md), Stage 0 (Husky), then Stage A.

---

## 2. Project structure

```
book/
├── compose.yaml, Dockerfile, .dockerignore     # infra: leave alone while building the API
├── pom.xml
└── src/main/
    ├── resources/application.yaml               # config, all values overridable by env vars
    └── java/np/com/milapmagar/book/
        ├── BookApplication.java                 # entry point: must sit ABOVE every other package
        ├── RootController.java                  # GET / landing JSON
        ├── controller/   # HTTP only: parse the request, call the service, build the response
        ├── services/     # business rules: no HTTP, no SQL
        ├── repository/   # JpaRepository interfaces: Spring writes the implementation
        ├── model/        # @Entity classes: one per table
        ├── dto/          # request/response records: the API contract
        └── exception/    # custom exceptions, ErrorResponse, (soon) the global handler
```

**The request path:**

```
HTTP → Controller → Service → Repository → Entity → PostgreSQL
          ↑ DTOs in/out      ↑ Entities stay below this line
```

When you add a feature, it usually touches each layer once, from the bottom up:
entity → repository → service → controller.

---

## 3. Running locally

`compose.yaml` publishes Postgres on host port **5434**, but `application.yaml` defaults to `5432`.
Pick one of these:

```bash
# A) Everything in Docker. Slow rebuilds, zero config.
docker compose up --build

# B) Postgres in Docker, app from IDE/mvnw. Use this while coding.
docker compose up -d postgres
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5434/bookdb ./mvnw spring-boot:run
```

For B in IntelliJ, put the same variable in the `BookApplication` run configuration.

```bash
docker compose ps                                     # status
docker compose logs -f postgres                       # logs
docker compose down                                   # stop, keep data
docker compose down -v                                # stop, wipe data
docker compose exec postgres psql -U book -d bookdb   # SQL prompt: \dt  \d book  \q
./mvnw test                                           # run the tests
```

---

## 4. Workflow

### 4.1 The dev loop

```
pick the next step in TODO.md → write code → run locally (§3 B) → curl it
→ tick the box in TODO.md → commit → push → Render redeploys → curl the Render URL
```

Push after every step. Small diffs make "works on my machine" bugs easy to find. If something
works locally but not on Render (the first request after idling can take 30–60 s, so be patient),
you've found a real bug.

### 4.2 Git

- Work on a branch named `feat/<thing>` or `fix/<thing>`, then open a PR into `main`. Merging to `main` deploys.
- Commit messages follow the existing style: `type(scope): what changed`
  - e.g. `added(dto): book request/response records`, `fixes(controller): 404 for missing book`
- Keep one TODO step per commit where possible.
- Husky git hooks (set up in `TODO.md` Stage 0) compile the project before every commit. Don't skip them with `--no-verify`.

### 4.3 Keeping the docs current

- **`TODO.md`:** tick the box when the step's ✔ check passes, and update the "Next step" line.
- **This guide:** update §1 Status when a stage finishes, and add to §7.4 whenever you hit a bug worth remembering.

---

## 5. Conventions (the rules this codebase follows)

These are the rules. Some existing code still breaks them; `TODO.md` Stage A fixes that.

**Layers**
- Controllers only translate HTTP ↔ Java. No business logic, and they never call a repository directly.
- Services hold the rules. No `HttpServletRequest`, `ResponseEntity`, or web annotations.
- Services take and return **DTOs**. Entities never cross the HTTP boundary.
- Entity → DTO conversion lives in **one** place (`BookResponseDto.from(Book)` or a `BookMapper`).

**Dependencies**
- Constructor injection into `private final` fields. **Never** `@Autowired` on fields.
- If a constructor needs more than ~4 dependencies, the class is doing too much. Split it.

**REST**
- All resources live under `/api/v1/<plural-noun>`. Use a bare `@GetMapping` for the collection route (see §7.4).
- Status codes: `200` read/update · `201` + `Location` create · `204` delete · `400` validation ·
  `404` not found · `409` conflict (e.g. duplicate ISBN).
- Request bodies are always `@Valid @RequestBody`; the constraints live on the request DTO.

**Errors**
- Services **throw** typed exceptions (`ResourceNotFoundException`, `DuplicateIsbnException`). Never
  return `null`, and never throw a bare `RuntimeException`.
- One `@RestControllerAdvice` maps exceptions → status codes, and every error body is an `ErrorResponse`.
- Clients never see a stack trace.

**Data**
- Writes are `@Transactional`; reads are `@Transactional(readOnly = true)`.
- `equals`/`hashCode` use the business key (`isbn`), never the generated `id`.
- Entities guard their own state with methods (`applyDiscount()`), not bare setters called from services.
- Schema: `ddl-auto: update` for now. Once Flyway lands, every schema change becomes a new `V<n>__*.sql` file.

**Config & secrets**
- Every setting in `application.yaml` is `${ENV_VAR:local-default}`. Real credentials only come from Render env vars.

**Tests**
- Service tests use a Mockito mock repository: no Spring context, no DB, and they run in milliseconds.
- Controller tests (`@WebMvcTest`) assert status codes and JSON only, not business rules.

---

## 6. Deployment


auto-deploys on push to `main`, with its health check on `/actuator/health`. You almost never need to
touch the `Dockerfile` or `compose.yaml` while building the API. Full details are in `DOCKER_DEPLOY.md`.

| `ddl-auto` | Behaviour | Use when |
|---|---|---|
| `none` | Touches nothing | Prod with Flyway |
| `validate` | Fails if entities ≠ schema | Once Flyway owns the schema |
| `update` | Adds, never drops/renames, and drifts silently | Learning (current setting) |
| `create-drop` | Rebuild on start, drop on stop | Tests |

---
The app runs as a Docker image on Render, configured entirely by environment variables. It
## 7. Concepts

### 7.1 How Spring Boot starts
1. `main()` calls `SpringApplication.run(...)`, which creates the **ApplicationContext** (the bean container).
2. **Component scan** searches the main class's package and every sub-package for `@Component`, `@Service`,
   `@Repository`, `@RestController`, and `@Configuration`.
3. Each one is created once as a **bean**, and its dependencies are **injected** through constructors. You never `new` a service.
4. **Auto-configuration** reads the classpath: JPA gives a DataSource + EntityManager, and Web MVC gives embedded Tomcat.

### 7.2 Why DTOs, not Entities
1. **Leakage:** every column gets exposed.
2. **Coupling:** renaming a column breaks clients.
3. **Input ≠ output:** a create request has no `id`, but the response does.
4. **Lazy loading:** you get surprise queries or infinite recursion.

### 7.3 OOP & SOLID in this project

| Concept | Where it lives here |
|---|---|
| **Encapsulation** | Private `Book` fields; behaviour like `applyDiscount()` guards state. Avoid the "anaemic model". |
| **Abstraction** | `BookRepository` is an interface; Spring Data writes the implementation. |
| **Inheritance** | Only for real "is-a": `DuplicateIsbnException extends RuntimeException`, `Book extends BaseEntity`. |
| **Polymorphism** | `BookExporter` implementations; `@ExceptionHandler` picks a response by exception *type*. |
| **S**: Single Responsibility | Controller changes with the API, Service with the rules, Repository with storage. |
| **O**: Open/Closed | A new export format means a new class, not a new `if`. Count the files you *edit*. |
| **L**: Liskov | Every `BookExporter` keeps the interface's promises (e.g. never returns null). |
| **I**: Interface Segregation | Small interfaces per use case, not a 12-method `BookOperations`. |
| **D**: Dependency Inversion | `BookService` gets a `BookRepository` *interface* through its constructor; tests pass a fake. |

### 7.4 Lessons learned 🐛
- **2026-09-30:** `@GetMapping("/")` on a class mapped to `/api/v1/books` gives `/api/v1/books/`. Since
  Boot 3, `/api/v1/books` won't match that route. Use a bare `@GetMapping` for the collection.
- **2026-09-30:** Casting `findAll()`'s `List<Book>` to `(Book)` compiles but throws `ClassCastException`
  at runtime. Use `.stream().map(...).toList()` instead, which is the same idea as `rows.map(toDto)`.

### 7.5 Mistakes to avoid

| Mistake | Why it hurts |
|---|---|
| `@Autowired` on fields | Hides dependencies, fields can't be `final`, hard to test |
| Returning Entities from controllers | Leaks schema, couples API to DB, lazy-loading surprises |
| Logic in the controller, or the controller calling the repository | Untestable, and the rules get duplicated |
| Catching exceptions and returning `null` | The failure surfaces later with no clue why |
| One giant `BookService` | Breaks SRP; no obvious place to add anything |
| `equals`/`hashCode` on generated `id` | Unsaved entities (`id == null`) collapse together in a `Set` |
| `ddl-auto: update` forever | Schema drifts, with no history of how it got there |

---

## 8. Cheat sheets

### 8.1 Express/Fastify → Spring Boot 🔁

| Express / Fastify | Spring Boot |
|---|---|
| `express.Router()` + `app.use('/api/v1/books', router)` | `@RestController` + `@RequestMapping("/api/v1/books")` |
| `router.get('/', h)` / `post` / `put` / `delete` | `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping` |
| `req.params.id` / `req.query.page` / `req.body` | `@PathVariable Long id` / `@RequestParam int page` / `@RequestBody Dto body` |
| Joi / Zod / Fastify schema | `@Valid` + `@NotBlank`, `@Positive`… on the DTO |
| `res.status(201).json(x)` | `ResponseEntity.created(uri).body(x)` |
| `res.status(204).end()` | `ResponseEntity.noContent().build()` |
| Error middleware `(err, req, res, next)` | `@RestControllerAdvice` + `@ExceptionHandler` |
| Passing deps in / `require` | Constructor injection |
| Prisma / TypeORM model | `@Entity` + `JpaRepository` |
| `rows.map(toDto)` | `list.stream().map(BookResponseDto::from).toList()` |
| `.env` + `process.env.X` | `application.yaml` + `${X:default}` |

### 8.2 Annotations

| Annotation | Layer | What it does |
|---|---|---|
| `@SpringBootApplication` | main | Component scan + auto-config + config class |
| `@RestController` | controller | Return values become the response body |
| `@RequestMapping` / `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping` | controller | Map URL + method |
| `@PathVariable` / `@RequestParam` / `@RequestBody` | controller | Bind path / query / JSON body |
| `@Valid` | controller | Actually run the DTO constraints |
| `@RestControllerAdvice` + `@ExceptionHandler` | controller | Global exception → HTTP mapping |
| `@Service` / `@Repository` | service / repo | Component markers (`@Repository` also translates persistence exceptions) |
| `@Transactional` | service | Wrap the method in a DB transaction |
| `@Entity` / `@Table` / `@Id` / `@GeneratedValue` / `@Column` | model | Table, PK, and column mapping |
| `@ManyToOne` / `@OneToMany` / `@MappedSuperclass` | model | Relationships / shared parent fields |
| `@NotBlank` / `@NotNull` / `@Positive` / `@Email` / `@Size` | dto | Validation constraints |
| `@Configuration` / `@Bean` | config | Beans you construct yourself |
