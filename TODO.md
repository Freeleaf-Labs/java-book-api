# Book API: To-do

Everything left to build, in order, in one list.
For how to run the project, its conventions, and the concepts behind each step, see
[`PROJECT_GUIDE.md`](PROJECT_GUIDE.md).

*Last updated: 2026-10-05. Next step: **T1** (Categories), see Today below.*

Do the steps in order. **Run → curl → commit → push → curl the Render URL** after each one
(see `PROJECT_GUIDE.md` → The dev loop). A step is done only when its **✔ check** passes.

## 🎯 Today (2026-10-05): categories, genres, authors, users/me

Four small APIs. The first three are the same shape, so build **categories** end to end first, then
copy the pattern. JSON shapes for categories and `users/me` are in the frontend repo:
`../book/docs/BACKEND_TASKS.md` → Part 2. **Genres and authors are not in that contract yet**, so the
shapes below are a proposal; once they are settled, add them to the contract so the frontend can use them.

Each of T1–T3 needs: entity → repository → request/response DTO (with a static `from(...)`) →
service → controller. Reuse `ResourceNotFoundException` for 404s and add one `DuplicateResourceException`
mapped to **409** in `GlobalExceptionHandler` for all three.

T1. - [ ] **Categories** `/api/v1/categories`
      - [ ] `Category` entity: `id`, `name` (unique), `slug` (unique, generated from the name:
            lower-case, spaces → `-`)
      - [ ] `GET /categories` returns a plain array sorted by name (not paged)
      - [ ] `POST /categories` with `@Valid` `{ name }` → **201** `CategoryResponse`; **409** if it exists
      - [ ] `PUT /categories/{id}` `{ name }` → 200; 404 if missing
      - [ ] `DELETE /categories/{id}` → **204**; 404 if missing
      ✔ `curl -s localhost:8080/api/v1/categories` shows
        `[{"id":1,"name":"Programming","slug":"programming"}]`, and POSTing the same name twice gives 409.

T2. - [ ] **Genres** `/api/v1/genres`: same five endpoints and the same `{ id, name, slug }` shape as T1.
      Decide first how a genre differs from a category (for example category = subject area such as
      *Programming*, genre = kind of book such as *Textbook* or *Reference*). If they turn out to be
      the same thing, keep only categories.
      ✔ Same checks as T1 against `/genres`.

T3. - [ ] **Authors** `/api/v1/authors`
      - [ ] `Author` entity: `id`, `name` (required), `bio` (optional)
      - [ ] `GET /authors` (sorted by name), `GET /authors/{id}`, `POST`, `PUT /authors/{id}`,
            `DELETE /authors/{id}` with the same status codes as T1
      - [ ] Leave `Book.author` as a `String` for today. Linking `Book` → `Author` with `@ManyToOne`
            changes the book JSON, so do it as a separate step together with the frontend contract.
      ✔ POST an author, GET it back by id, and `GET /authors/999` returns the 404 JSON.

T4. - [ ] **`GET /api/v1/users/me`** (and `PATCH /users/me` with `{ displayName }`)
      - [ ] Prerequisite: this needs to know *who* is calling, and today `SecurityConfig`,
            `JwtService`, `JwtAuthenticationFilter` and `UserController` are empty classes and
            Spring Security is not in `pom.xml`. Get register + login + the JWT filter working first
            (frontend contract → Stage 3), or `/users/me` has no user to return.
      - [ ] `User` has `fullname`, `email`, `password`, `phone` but the contract's `UserResponse` is
            `{ id, email, displayName, role, enabled, createdAt }`. Add `role`, `enabled`,
            `createdAt`, and either rename `fullname` → `displayName` or map it in the DTO.
      - [ ] `UserResponse.from(User)` must never include the password.
      - [ ] Read the current user from the security context (`@AuthenticationPrincipal` or
            `Authentication`), not from an id in the URL or body.
      ✔ `curl -i localhost:8080/api/v1/users/me` with no token → **401**; with
        `-H 'Authorization: Bearer <token>'` → 200 and the logged-in user, with no `password` key.

T5. - [ ] **Link them.** Add `categories`, `genres` and `authors` to the links map in `RootController`.

## 📌 Stage 0: Git hooks with Husky (do this first)

Husky is the same tool you know from Node projects. It lives at the **repo root** (`book-api/`, where
`.git` is), not inside `book/`.

0. - [ ] **Set up Husky.** 
** NO HUSKY IN JAVA SO EXPLORE MORE**
     - [X] `npm init -y` at the repo root, then `npm i -D husky` and `npx husky init`
     - [X] Add `node_modules/` to `.gitignore`
     - [X] **pre-commit** hook: `cd book && ./mvnw -q compile` (switch it to `./mvnw -q test` once Stage F has tests)
     - [X] *(Optional)* **commit-msg** hook: use commitlint or a small regex check so messages follow
           `type(scope): message` (see `PROJECT_GUIDE.md` → Git)
     - [X] Commit `package.json`, `package-lock.json` and `.husky/`
     ✔ A commit containing a Java compile error is **blocked**, and a clean commit goes through.

## Stage A: Clean up what exists 🧹

1. - [X] **Constructor injection.** In `BookService` and `BookController`, swap field `@Autowired` for
         `private final` fields set through the constructor. *(Why: see `PROJECT_GUIDE.md` → Conventions)*
         ✔ No `@Autowired` in the project.
2. - [X] **Fix the `ibsn` typo** so it reads `isbn` in both DTOs. The JSON key follows the field name.
         ✔ `curl /api/v1/books` shows `"isbn"`.
3. - [X] **Tidy `Book`.** Make `id` a `Long` and have `setId` take a `Long` (right now it takes an `int`).
         Delete the stray `getTui()`. `price` and `applyDiscount()` were removed: BookHub has no prices.
4. - [X] **Tidy `BookRepository`.** `findByIsbn` → `Optional<Book>`. Add `existsByIsbn` and
         `findByTitleContainingIgnoreCase`. Remove the redundant `findAll()` redeclaration.
5. - [X] **One mapper.** Move entity → DTO into one place (a static `BookResponseDto.from(Book)` or a
         `BookMapper`) so the constructor call stops being copied into every method.
6. - [X] **Housekeeping.** Choose `service` or `services` as the package name and use it everywhere. Delete
         the empty `database/` package. Delete `DatabaseTestRunner` once you no longer need it.
         Remove H2 from `pom.xml` because it's unused.
         ✔ App still starts, and both GET endpoints still work.

## Stage B: Error handling 🚨

7.  - [X] **Upgrade `ErrorResponse`.** Change `LocalTime` → `Instant` (a time with no date is useless in logs), and add
          a `fieldErrors` map for 400s.
8.  - [X] **Real 404s.** `getBookById` throws `ResourceNotFoundException`, not a bare `RuntimeException`.
          Create a `@RestControllerAdvice` and map it to 404 using `ErrorResponse`.
          ✔ `curl -i localhost:8080/api/v1/books/999` returns 404 JSON, not a 500.
9.  - [X] **Validation.** Put `@NotBlank` on title/author/isbn in `BookRequestDto`, and
          a size/pattern rule on isbn. Handle `MethodArgumentNotValidException` → 400 with
          `fieldErrors` such as `{"title": "must not be blank"}`.

## Stage C: Finish CRUD ✍️

10. - [ ] **POST** `/api/v1/books` with `@Valid @RequestBody BookRequestDto` (a bare `@RequestBody` does
          **not** validate). Return **201** with `Location: /api/v1/books/{id}`
          (hint: `ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")…`).
          Reject a duplicate ISBN (`existsByIsbn`) by throwing `DuplicateIsbnException`, and map it to **409**.
11. - [ ] **PUT** `/api/v1/books/{id}`: 404 if missing; otherwise copy the fields, save, and return 200.
12. - [ ] **DELETE** `/api/v1/books/{id}`: 404 if missing; otherwise return **204** with no body.
13. - [ ] **Transactions.** Put `@Transactional` on the write methods and `@Transactional(readOnly = true)` on
          the reads. Look up "dirty checking" to see why PUT works without `save()`.
14. - [ ] **Service purity.** No `Http*`, `ResponseEntity`, or web annotation anywhere in the service.
          ✔ Grepping the service for `Http` and `ResponseEntity` finds nothing.
15. - [X] **Link it.** Add `"books": "/api/v1/books"` to the links map in `RootController`.
16. - [ ] **Exercise everything**, locally first and then on Render (the first request after idling can take 30–60 s):

    ```bash
    curl -s -X POST localhost:8080/api/v1/books -H 'Content-Type: application/json' \
      -d '{"title":"Clean Code","author":"Robert C. Martin","isbn":"9780132350884","publisher":"Prentice Hall","publishedYear":"2008"}'
    curl -s  localhost:8080/api/v1/books
    curl -i  localhost:8080/api/v1/books/999                                                 # 404
    curl -i -X POST localhost:8080/api/v1/books -H 'Content-Type: application/json' -d '{}'  # 400
    ```

    ✔ Every status code is correct: 200 / 201 / 204 / 400 / 404 / 409.

## Stage D: Database done properly 🐘

17. - [ ] POST a book, then check it in SQL:
          `docker compose exec postgres psql -U book -d bookdb -c 'select * from book;'`
          Restart the app and confirm the row is still there.
18. - [ ] **Flyway.** Move the schema into `src/main/resources/db/migration/V1__create_book.sql` and switch
          to `ddl-auto: validate` locally **and** in Render's `SPRING_JPA_HIBERNATE_DDL_AUTO`.
          ✔ The app starts with `validate`, and Render redeploys cleanly.

## Stage E: Extra API features 🟡

19. - [ ] **PATCH** `/api/v1/books/{id}`: update only the fields that were sent. Write down how this differs from PUT.
20. - [ ] **Search:** `GET /api/v1/books?author=…&title=…` using your derived queries.
21. - [ ] **Pagination & sorting:** `?page=0&size=10&sort=title,asc` using `Pageable`. Look at how the
          JSON changes, and note that the repository didn't need any changes.

## Stage F: Tests 🧪

23. - [ ] Unit-test `BookService` with a Mockito mock repository, with no Spring context and no DB.
          Cover the failure paths: unknown id, duplicate ISBN, and a bad discount.
24. - [ ] `@WebMvcTest` for `BookController` with a mocked service. Assert status codes and JSON only.
25. - [ ] One integration test against a real Postgres (Testcontainers).
          ✔ `./mvnw test` passes, and the service tests run in milliseconds.

## Stage G: Feel Open/Closed 🧩

26. - [ ] `BookExporter` interface with CSV and JSON implementations. Inject them all as a `List` and pick one
          with `?format=`.
          ✔ Adding a third exporter (XML) means adding one new class and editing **zero** existing files.

## Stage H: Grow the domain 🌱

27. - [ ] **Audit fields:** a `BaseEntity` (`@MappedSuperclass`) with `createdAt`/`updatedAt`
          (`@CreationTimestamp` / `@UpdateTimestamp`).
28. - [ ] Add `totalCopies`/`availableCopies` to `Book`, plus a `borrowOneCopy()` method that refuses when the count is zero.
29. - [ ] `Member` entity (name, email, joinedAt) and a `Loan` entity (`@ManyToOne` LAZY → Book and Member,
          `borrowedAt`/`returnedAt`).
30. - [ ] `LoanService` handles borrow/return in **one transaction**, and `POST /api/v1/books/{id}/borrow` exposes it.
          ✔ Borrowing decrements `availableCopies` **and** creates a `Loan` row, or on failure does neither.
          Check the SQL log for N+1 queries (`show-sql: true`).

## Stage I: Production polish 🚀

31. - [ ] **API docs:** add springdoc-openapi and open Swagger UI (it plays the same role as `@fastify/swagger`).
32. - [ ] `application-prod.yaml` profile (Render already sets `SPRING_PROFILES_ACTIVE=prod`).
33. - [ ] Restrict Actuator exposure, or put it behind auth.
34. - [ ] Spring Security. This is where `UserLoginRequest` finally gets used; until then, park it or delete it.

---

## 🏁 Definition of done

- [ ] All endpoints work locally **and** on Render with correct status codes
- [ ] Data persists across restarts; schema is owned by Flyway with `ddl-auto: validate`
- [ ] No Entity crosses the HTTP boundary; the service layer has zero web imports
- [ ] Every dependency is a `final` field injected through the constructor
- [ ] Errors are structured JSON, never a stack trace
- [ ] Service unit tests run without a database
- [ ] You can explain, out loud, where each SOLID principle lives in your code
