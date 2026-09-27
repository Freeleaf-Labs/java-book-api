# Book API — Learning Report & Build Checklist

A book-keeping REST API (simple datasets) built with Spring Boot, used as a vehicle for
learning **layered architecture**, **OOP concepts**, and **SOLID principles**.

> **How to use this file:** work top to bottom. Sections 1–5 are *understanding*.
> Section 6 is the **checklist** you tick off while building. No Java code is given on
> purpose — you write it, the file tells you *what* and *why*.

---

## 1. Where the project stands today

| Thing | Value |
|---|---|
| Location | `book-api/book/` |
| Package | `np.com.milapmagar.book` |
| Spring Boot | 4.1.1 |
| Java | 25 |
| Build tool | Maven, via the wrapper `./mvnw` (no system Maven needed) |
| Database now | H2 in-memory (we will replace with PostgreSQL) |
| Files written | `BookApplication.java`, `BookApplicationTests.java`, `application.yaml` |

Already on the classpath: **Web MVC**, **Data JPA**, **Validation**, **Actuator**, **DevTools**,
**H2 + H2 console**, **REST Docs** (test only).

Nothing else exists yet. That is the point — you are going to add it.

---

## 2. What actually happens when a Spring Boot app runs

You said you know "Main file runs it, Controller maps the API". True, but incomplete.
Here is the full mental model.

### 2.1 Startup, in order

1. `main()` calls `SpringApplication.run(...)`.
2. Spring creates the **ApplicationContext** — a container that holds objects.
3. **Component scanning**: Spring scans the package of your main class *and every
   sub-package*, looking for classes marked as components
   (`@Component`, `@Service`, `@Repository`, `@RestController`, `@Configuration`).
4. Each such class is instantiated once and stored in the container. A container-managed
   object is called a **bean**.
5. **Dependency injection**: if a bean's constructor asks for another bean, Spring finds
   it and passes it in. You never call `new` on your own services.
6. **Auto-configuration**: Spring inspects the classpath and configures what it finds
   (JPA present → build a `DataSource` and an `EntityManager`; Web MVC present → start
   an embedded Tomcat on port 8080).
7. Tomcat starts listening. The app is up.

**Consequence worth remembering:** your main class must sit *above* everything else in the
package tree. `np.com.milapmagar.book.BookApplication` can see
`np.com.milapmagar.book.controller.*`, but would never see `np.com.other.*`.

### 2.2 The path of one HTTP request

```
HTTP request
   ↓
Controller      — talks HTTP. Reads the request, returns a response. Nothing else.
   ↓
Service         — the business rules. Knows nothing about HTTP or SQL.
   ↓
Repository      — talks to the database. An interface; Spring writes the implementation.
   ↓
Entity          — a Java class mapped to a database table.
   ↓
PostgreSQL
```

And two supporting cast members:

- **DTO** (Data Transfer Object) — the shape you accept and return over HTTP. Deliberately
  *not* the Entity.
- **Exception handler** — turns thrown exceptions into clean JSON error responses.

### 2.3 Why not just put everything in the Controller?

You can. It works. It also means: you cannot unit-test the rules without starting a web
server, you cannot reuse the rules from a scheduled job or a CLI, and changing the JSON
shape risks changing your database schema. The layers exist to keep *reasons to change*
apart. That is literally the first SOLID principle, arriving early.

### 2.4 Why a DTO instead of returning the Entity

Four concrete reasons:

1. **Leakage** — an Entity returned as JSON exposes every column, including ones you did
   not mean to publish.
2. **Coupling** — renaming a column would silently break every API client.
3. **Input is not output** — on create, the client must not send `id`; in the response it
   must be present. Two shapes, two classes (`BookRequest`, `BookResponse`).
4. **Lazy loading** — serialising a JPA entity with relationships can trigger surprise
   database queries or infinite recursion.

---

## 3. OOP concepts, mapped onto *this* project

Not textbook definitions — where each one physically lives in the Book API.

### Encapsulation — *hide the data, expose behaviour*

The `Book` entity keeps its fields private. Outside code cannot set an invalid state
directly. Instead of a public setter for copies, the entity gets behaviour:
`borrowOneCopy()` which refuses when the count is already zero.

> **The smell it prevents:** the "anaemic model" — an entity that is nothing but getters
> and setters, with all the rules scattered across services that forget to check them.

### Abstraction — *depend on the idea, not the machinery*

`BookRepository` is an **interface**. Your service knows only "something can save and find
books". It does not know about JDBC, connection pools, or SQL. Spring Data generates the
implementation at runtime from the method names.

### Inheritance — *share structure, carefully*

Use it where the relationship is genuinely "is-a":

- A `BookNotFoundException` **is an** exception → extends `RuntimeException`.
- Entities that all need `createdAt` / `updatedAt` → a shared `BaseEntity`
  (`@MappedSuperclass`) they extend.

Use it sparingly. Two classes sharing a few fields is not a reason to make one the parent
of the other. Prefer composition — hold a reference instead of extending.

### Polymorphism — *one call, many behaviours*

One interface, several implementations, chosen at runtime. In this project:

- `NotificationSender` with an email implementation and a log implementation — the service
  calls `send(...)` and does not care which arrived.
- Spring's own exception handling: you throw different exception *types*, one handler
  method per type decides the HTTP status.

---

## 4. SOLID, mapped onto *this* project

| Principle | In one line | Where it shows up here |
|---|---|---|
| **S** — Single Responsibility | One class, one reason to change | Controller changes when the API changes; Service when the rules change; Repository when storage changes. Three files, not one. |
| **O** — Open/Closed | Extend behaviour without editing existing code | Adding a new export format (CSV, JSON) means adding a new `BookExporter` implementation — not adding an `if` to the old one. |
| **L** — Liskov Substitution | A subtype must work anywhere its parent does | If `BookExporter` promises "never returns null", every implementation must honour it. A subclass that throws where the parent returned a value breaks callers. |
| **I** — Interface Segregation | Small focused interfaces, not one fat one | Don't build a `BookOperations` with twelve methods that implementations half-implement. Split by use case. |
| **D** — Dependency Inversion | Depend on abstractions, not concretions | `BookService` holds a `BookRepository` *interface*, injected through its constructor. In a unit test you pass a fake. No database needed. |

### The one habit that gives you most of SOLID for free

**Constructor injection with `final` fields.**

- Field is `final` → the dependency cannot be swapped after construction.
- It is an interface → you have Dependency Inversion.
- The constructor makes dependencies *visible* → if it needs six things, the class is doing
  too much, and Single Responsibility tells you so immediately.
- No Spring needed to build the object in a test.

Avoid `@Autowired` on fields. It hides dependencies and makes the class untestable without
reflection.

---

## 5. PostgreSQL setup (Docker)

You have Docker 29.7.2. Docker is preferred over the local install: disposable, versioned
with the project, identical on any machine.

### 5.1 The compose file

Create `book-api/book/compose.yaml`:

```yaml
services:
  postgres:
    image: postgres:18
    environment:
      POSTGRES_DB: bookdb
      POSTGRES_USER: book
      POSTGRES_PASSWORD: book
    ports:
      - "5432:5432"
    volumes:
      - book-pgdata:/var/lib/postgresql/data

volumes:
  book-pgdata:
```

The named volume means your rows survive `docker compose down`. To wipe the database
completely: `docker compose down -v`.

### 5.2 Commands you will use constantly

```bash
docker compose up -d          # start Postgres in the background
docker compose ps             # is it healthy?
docker compose logs -f postgres
docker compose down           # stop, keep data
docker compose down -v        # stop, destroy data

# open a SQL prompt inside the container
docker compose exec postgres psql -U book -d bookdb
#   \dt            list tables
#   \d books       describe the books table
#   \q             quit
```

### 5.3 Wire Spring to it

Swap the H2 dependency in `pom.xml` for the PostgreSQL driver
(`org.postgresql:postgresql`, scope `runtime`), then set `application.yaml`:

```yaml
spring:
  application:
    name: book
  datasource:
    url: jdbc:postgresql://localhost:5432/bookdb
    username: book
    password: book
  jpa:
    hibernate:
      ddl-auto: update      # learning only — see the warning below
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

**`ddl-auto` — know what you are choosing:**

| Value | Behaviour | Use when |
|---|---|---|
| `none` | Hibernate touches nothing | Production, or once you adopt Flyway |
| `validate` | Checks entities match the schema, fails otherwise | Safe default once the schema is stable |
| `update` | Adds missing tables/columns. Never drops or renames. | Learning, early development |
| `create-drop` | Rebuilds the schema every start, drops on shutdown | Tests |

`update` is convenient and *silently drifts*. It will not remove a column you deleted from
an entity. Once the model settles, move to **Flyway** migrations (versioned `.sql` files in
`src/main/resources/db/migration`) and set `ddl-auto: validate`. That is the professional
setup and it is Phase 7 below.

> **Optional shortcut:** Spring Boot can start `compose.yaml` itself and inject the
> connection details, via the `spring-boot-docker-compose` module. Nice once you're
> comfortable — but do the manual `docker compose up -d` first, so you understand what is
> being automated. Verify the artifact id resolves for Boot 4.1 before relying on it.

---

## 6. The build checklist

Work one phase at a time. **Run the app after every phase.** A phase is not done until its
*Verify* line passes.

### Phase 0 — Baseline

- [ ] `cd book-api/book`
- [ ] `./mvnw spring-boot:run` — confirm it starts on port 8080
- [ ] Open `http://localhost:8080/actuator/health` → expect `{"status":"UP"}`
- [ ] Read `BookApplication.java`. Identify the annotation that triggers component
      scanning and auto-configuration.
- [ ] **Verify:** app starts, health endpoint responds.

**Concept:** the container starts even with zero endpoints of your own. Actuator's
endpoint came from auto-configuration, not from code you wrote.

---

### Phase 1 — Package structure

- [ ] Inside `np.com.milapmagar.book`, create packages:
      `controller`, `service`, `repository`, `model` (or `entity`), `dto`, `exception`
- [ ] **Verify:** every package sits *under* the package holding `BookApplication`.

**Concept:** package-by-layer. (There is a rival convention, package-by-feature — a
`book` package holding its own controller/service/repository. Layers are easier while
learning; note that the alternative exists.)

---

### Phase 2 — Entity

- [ ] Create `Book` in `model`. Fields: `id`, `title`, `author`, `isbn`,
      `publishedYear`, `totalCopies`, `availableCopies`
- [ ] Mark the class as a JPA entity; map it to a `books` table
- [ ] `id`: generated primary key (identity strategy suits Postgres)
- [ ] `isbn`: unique, not null
- [ ] All fields **private**
- [ ] Add one behaviour method, e.g. `borrowOneCopy()`, that throws when no copies remain
- [ ] Give it a no-arg constructor (JPA requires one) and a constructor taking the real fields
- [ ] Implement `equals`/`hashCode` on `isbn` (the business key), **not** on `id`
- [ ] **Verify:** start the app, then `\dt` in psql — the `books` table exists.

**Concept: Encapsulation.** Ask yourself: *can outside code put this object into an
invalid state?* If yes, you exposed too much. The `borrowOneCopy()` method is the
difference between a real object and a data bag.

---

### Phase 3 — Repository

- [ ] Create `BookRepository` in `repository` as an **interface** extending
      `JpaRepository<Book, Long>`
- [ ] Write zero method bodies
- [ ] Add derived query methods by naming convention:
      `findByAuthor(...)`, `findByIsbn(...)` (returning `Optional`),
      `existsByIsbn(...)`, `findByTitleContainingIgnoreCase(...)`
- [ ] **Verify:** the app still starts. Spring generated the implementation — you can
      confirm by logging the bean's class name and seeing a proxy type.

**Concepts: Abstraction + Dependency Inversion.** You declared *what* you need; the
framework supplied *how*. Note how little you had to write — that is the payoff of coding
against an interface.

---

### Phase 4 — DTOs

- [ ] Create `BookRequest` in `dto` — what a client may send. **No `id` field.**
- [ ] Create `BookResponse` — what you return. Includes `id`.
- [ ] Add validation constraints to the request: `@NotBlank` on title and author,
      `@Positive` on totalCopies, a pattern or size rule on isbn
- [ ] Decide how to convert Entity ↔ DTO. Start with a small static factory or a dedicated
      mapper class. (MapStruct exists; add it later, once you have felt the boilerplate.)
- [ ] **Verify:** nothing to run yet — this phase is design.

**Concept: Single Responsibility at the data level.** The Entity's job is persistence;
the DTO's job is the API contract. Two jobs, two classes, two independent reasons to change.

---

### Phase 5 — Service

- [ ] Create `BookService`, marked as a service component
- [ ] `private final BookRepository repository;` — injected **via the constructor**
- [ ] Methods: `create`, `findAll`, `findById`, `update`, `delete`, `borrow`
- [ ] Signatures take and return **DTOs**, never Entities
- [ ] Reject a duplicate ISBN on create — throw your own exception, not a raw
      `IllegalArgumentException`
- [ ] No `HttpServletRequest`, no `ResponseEntity`, no annotation from the web package
      anywhere in this file
- [ ] Mark write methods transactional; mark reads read-only
- [ ] **Verify:** grep the file for `Http` and `ResponseEntity` — zero hits.

**Concepts: SRP + DIP together.** The grep is the test. If the service mentions HTTP, it
has two reasons to change and you have coupled business rules to a transport protocol.

---

### Phase 6 — Controller & error handling

- [ ] Create `BookController`, mapped at `/api/books`
- [ ] Inject `BookService` through the constructor
- [ ] Endpoints:

| Method | Path | Returns |
|---|---|---|
| `GET` | `/api/books` | 200, list |
| `GET` | `/api/books/{id}` | 200, or 404 |
| `POST` | `/api/books` | 201 + `Location` header |
| `PUT` | `/api/books/{id}` | 200 |
| `DELETE` | `/api/books/{id}` | 204 |
| `POST` | `/api/books/{id}/borrow` | 200 |

- [ ] Annotate the request body parameter so validation actually runs (a bare
      `@RequestBody` does **not** validate)
- [ ] Create `BookNotFoundException` and `DuplicateIsbnException` in `exception`
- [ ] Create a global exception handler class (`@RestControllerAdvice`) mapping:
      not-found → 404, duplicate → 409, validation failure → 400 with field errors
- [ ] Define one error response shape and use it everywhere
- [ ] **Verify:** exercise every endpoint:

```bash
curl -s -X POST localhost:8080/api/books \
  -H 'Content-Type: application/json' \
  -d '{"title":"Clean Code","author":"Robert C. Martin","isbn":"9780132350884","publishedYear":2008,"totalCopies":3}'

curl -s localhost:8080/api/books
curl -i localhost:8080/api/books/999          # expect 404, not a stack trace
curl -i -X POST localhost:8080/api/books -H 'Content-Type: application/json' -d '{}'   # expect 400
```

**Concepts: SRP + Polymorphism.** The controller only translates HTTP ↔ Java. The handler
picks a response by *exception type* — polymorphic dispatch replacing a pile of `if`s.

---

### Phase 7 — PostgreSQL

- [ ] Write `compose.yaml` (Section 5.1)
- [ ] `docker compose up -d`, confirm healthy with `docker compose ps`
- [ ] Replace the H2 dependency with the PostgreSQL driver in `pom.xml`
- [ ] Also remove the H2 console dependency — no longer used
- [ ] Update `application.yaml` (Section 5.3), `ddl-auto: update`, `show-sql: true`
- [ ] Restart, POST a book, then confirm it in SQL:
      `docker compose exec postgres psql -U book -d bookdb -c 'select * from books;'`
- [ ] Restart the app again — the row is still there (this is what H2 could not do)
- [ ] **Later in this phase:** add Flyway, move the schema into
      `src/main/resources/db/migration/V1__create_books.sql`, switch to `ddl-auto: validate`
- [ ] **Verify:** data survives an application restart.

**Concept:** the only files that changed were `pom.xml` and `application.yaml`. Your
controller, service, and entity did not. *That* is what depending on abstractions buys you.

---

### Phase 8 — Tests

- [ ] Unit-test `BookService` with a **mock** repository — no Spring context, no database
- [ ] Assert the failure paths: unknown id throws, duplicate ISBN throws, borrowing the
      last copy throws
- [ ] Web-layer test of `BookController` with a mocked service — assert status codes and
      JSON, not business rules
- [ ] One integration test that starts the app against a real Postgres
      (Testcontainers is the right tool; your Docker setup already satisfies it)
- [ ] **Verify:** `./mvnw test` green; the service tests run in milliseconds.

**Concept:** fast unit tests are only possible *because* of constructor injection against
an interface. If tests are slow or need a running database to check a rule, the design is
too coupled. Test speed is a design signal.

---

### Phase 9 — Apply Open/Closed deliberately

Pick one and build it, purely to feel the principle:

- [ ] **Export:** a `BookExporter` interface with CSV and JSON implementations. Spring can
      inject *all* implementations as a `List`; the endpoint picks by a `?format=` parameter.
      Adding XML later means adding one class and editing nothing.
- [ ] **Pagination & sorting:** accept a `Pageable` on the list endpoint. Note that you
      added a feature without changing the repository at all.
- [ ] **Verify:** adding the third exporter requires zero edits to existing files.

**Concept: Open/Closed + Polymorphism.** The test of OCP is mechanical — count the files
you had to *edit* to add a feature. Ideally: zero.

---

### Phase 10 — Grow the dataset

Now that one entity works end to end, add a relationship:

- [ ] `Member` entity (name, email, joinedAt)
- [ ] `Loan` entity linking a `Book` and a `Member`, with `borrowedAt` / `returnedAt`
- [ ] Map the relationships (`@ManyToOne` on `Loan` → both sides). Use `LAZY` fetching.
- [ ] Borrow/return logic in a `LoanService`, wrapped in a transaction
- [ ] **Verify:** borrowing decrements `availableCopies` and creates a `Loan` row —
      or, on failure, does neither.

**Concept:** transactions and aggregate consistency. Two writes that must both happen or
neither. Also watch for the **N+1 query problem** in your SQL log — `show-sql: true`
exists for exactly this.

---

## 7. Mistakes to avoid (they are all common)

| Mistake | Why it hurts |
|---|---|
| `@Autowired` on fields | Hides dependencies, blocks testing without reflection, allows non-`final` fields |
| Returning Entities from controllers | Leaks schema, couples API to database, causes lazy-loading surprises |
| Business logic in the controller | Untestable without a web server, unreusable |
| Calling the repository straight from the controller | Skips the layer where rules live; the rules then get duplicated |
| Catching exceptions and returning `null` | Hides the failure; the caller crashes later with no clue why |
| One giant `BookService` doing everything | Fails SRP; 800 lines with no obvious place to add anything |
| `equals`/`hashCode` on the generated `id` | Unsaved entities all have `id == null` and collapse into one another in a `Set` |
| `ddl-auto: update` forever | Schema drifts silently; no record of how it got that way |
| Passwords committed in `application.yaml` | Fine for local Docker; a real incident anywhere else. Use environment variables before deploying. |

---

## 8. Annotation cheat sheet

| Annotation | Layer | What it does |
|---|---|---|
| `@SpringBootApplication` | main | Component scan + auto-configuration + config class |
| `@RestController` | controller | Component whose return values become the response body |
| `@RequestMapping` / `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping` | controller | Map a URL + HTTP method to a method |
| `@PathVariable` / `@RequestParam` / `@RequestBody` | controller | Bind URL segment / query string / JSON body |
| `@Valid` | controller | Actually run the constraints on the bound object |
| `@RestControllerAdvice` + `@ExceptionHandler` | controller | Global exception → HTTP response mapping |
| `@Service` | service | Component marker; signals "business logic lives here" |
| `@Transactional` | service | Wrap the method in a database transaction |
| `@Repository` | repository | Component marker; translates persistence exceptions |
| `@Entity` / `@Table` | model | Map the class to a table |
| `@Id` / `@GeneratedValue` | model | Primary key and generation strategy |
| `@Column` | model | Column name, nullability, uniqueness, length |
| `@ManyToOne` / `@OneToMany` | model | Relationships between entities |
| `@MappedSuperclass` | model | Shared fields for a parent class that is not itself a table |
| `@NotBlank` / `@NotNull` / `@Positive` / `@Email` / `@Size` | dto | Validation constraints |
| `@Configuration` / `@Bean` | config | Declare beans you construct yourself |

---

## 9. Deployment — deliberately later

Not now, but so you know what is coming: containerise the app (Dockerfile or
`./mvnw spring-boot:build-image`), move credentials to environment variables, add a Spring
profile per environment, put Actuator behind authentication, and add Spring Security.
Finish Phase 10 first.

---

## 10. Definition of done for this project

- [ ] Six endpoints work and return correct HTTP status codes
- [ ] Data persists in PostgreSQL across restarts
- [ ] Schema is managed by Flyway, `ddl-auto: validate`
- [ ] No Entity ever crosses the HTTP boundary
- [ ] Service layer has zero web imports
- [ ] Every dependency is a `final` field injected through a constructor
- [ ] Service unit tests run without a database
- [ ] Errors return structured JSON, never a stack trace
- [ ] You can explain, out loud, where each SOLID principle lives in your code
