# Book API: To-do

Everything left to build, in order, in one list.
For how to run the project, its conventions, and the concepts behind each step, see
[`PROJECT_GUIDE.md`](PROJECT_GUIDE.md).

*Last updated: 2026-09-30. Next step: **0** (Husky).*

Do the steps in order. **Run → curl → commit → push → curl the Render URL** after each one
(see `PROJECT_GUIDE.md` → The dev loop). A step is done only when its **✔ check** passes.

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
