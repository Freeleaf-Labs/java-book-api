# Book API: To-do

One list of everything left to build, in order. How to run the project and the ideas behind each
step are in [`PROJECT_GUIDE.md`](PROJECT_GUIDE.md).

*Last updated: 2026-11-16 (checked against the code). The table below is current; the detailed
checkboxes further down were written before the big 2026-11-16 build and many of them are now done
in the code (they are kept as the explanation of each step).*

**Rules**
- Do the steps in order. After each one: run → curl → commit → push → curl the Render URL.
- A step is done only when its **✔ check** passes.
- Order from here: **Stage S** (auth) → **Stage T** (categories, genres, authors) → the lettered stages.

## Progress at a glance

| Stage | What | Status |
|---|---|---|
| A | Clean up the existing code | ✅ done |
| B | Error handling (404, 400, JSON errors) | ✅ done |
| S | Authentication | ✅ done: JWT access token, rotating refresh cookie, roles, admin seeding, CORS, JSON 401/403 |
| 0 | Pre-commit hook | ⬜ not done (nothing in the repo yet) |
| T | Categories, genres, authors | 🟡 categories done; genres and authors not started |
| C | Books: create, update, delete | ✅ done (admin only) |
| D | Database migrations (Flyway) | ⬜ not started |
| E | Patch, search, pagination | 🟡 search, pagination and sorting done; PATCH not started |
| F | Tests | 🟡 end-to-end tests on Testcontainers Postgres (`ApiFlowTests`); no unit tests yet |
| G | Exporters (Open/Closed) | ⬜ not started |
| H | Borrowing, purchases, subscriptions | ⬜ not started (the empty shells were removed) |
| I | Production polish | 🟡 Swagger UI at `/` done; prod profile not started |
| + | Reviews, admin users and stats (from the frontend contract) | ✅ done |

## 🔐 Stage S: Authentication (current)

**What exists today:** `POST /api/v1/auth/register` and `POST /api/v1/auth/login`. Register hashes
the password with BCrypt and rejects a duplicate email with 409. Login checks the password and
returns 401 on failure. **What is missing:** a token on login, locked routes (everything is still
`permitAll`), roles, refresh, CORS, tests.

**How it will work:** login returns a signed JWT. The client sends it as
`Authorization: Bearer <token>`. Spring Security checks it before any controller runs.
The JSON shapes are in the frontend repo: `../book/docs/BACKEND_TASKS.md` → Stage 3 and Part 2.

S1. - [X] **Project compiles.** ✔ `cd book && ./mvnw -q compile` prints nothing.

S2. - [ ] **Dependencies** in `pom.xml`
      - [X] `spring-boot-starter-security`
      - [ ] `spring-boot-starter-oauth2-resource-server` (gives the JWT encoder/decoder and the
            bearer-token filter, so no hand-written filter is needed)
      - [ ] `spring-security-test` with `<scope>test</scope>` (not in the pom yet)
      ✔ App starts.

S3. - [ ] **User entity and roles**
      - [X] Table is called `users`, email is unique
      - [ ] `Role` enum (`USER`, `ADMIN`) and a `role` column stored as a string
      - [ ] Rename `password` → `passwordHash` (makes it obvious it is never the raw password)
      - [ ] Rename `fullName` → `displayName` to match the frontend contract
      - [ ] Add `enabled` (default true) and `createdAt` (set automatically on insert)
      - [ ] Make `phone` optional. It is required and unique now, but it is not in the contract.
      - [ ] Remove the password from `toString()`
      - [ ] Local DB only: `docker compose down -v` once so the table is recreated cleanly
      ✔ `docker compose exec postgres psql -U book -d bookdb -c '\d users'` shows the new columns.

S4. - [ ] **User repository**
      - [X] `findByEmail`
      - [ ] `existsByEmail` must return `boolean`, not `String` (the service currently parses the
            string, which is a bug waiting to happen)
      - [ ] Lower-case the email in the service before saving and before looking up
      ✔ Compiles; app starts.

S5. - [ ] **Auth DTOs**
      - [X] Request/response types are records
      - [ ] Remove validation annotations from the *response* records (they do nothing there)
      - [ ] `UserResponse`: id, email, displayName, role, enabled, createdAt, with a static
            `from(User)`. It must never contain the password.
      - [ ] `AuthResponse`: `accessToken`, `expiresIn` (900 seconds), `user`
      ✔ Compiles.

S6. - [ ] **Security config, first version**
      - [X] CSRF off, BCrypt `PasswordEncoder` bean
      - [X] Duplicate email → 409 (`EmailAlreadyExistsException`)
      - [X] Stateless sessions
      - [X] Public: `/`, `/actuator/health`, `/api/v1/auth/**`, `GET /api/v1/books/**`.
            Everything else needs a login.
      ✔ `curl -i localhost:8080/api/v1/books` → 200, `curl -i -X POST localhost:8080/api/v1/books` → 401.

S7. - [ ] **Register**
      - [X] Service + endpoint, duplicate email → 409, password stored as a BCrypt hash
      - [ ] `@Valid` on the request body (right now `{}` gives a 500-ish error, not a clean 400)
      - [ ] Return **201**, not 200
      - [ ] Mark the service method `@Transactional`
      ✔ First call → 201 without a `password` key; same call → 409; `{}` → 400 with `fieldErrors`.

S8. - [ ] **Issue tokens** (`JwtService` is an empty class right now)
      - [ ] `app.jwt.secret` in `application.yaml`, read from `JWT_SECRET`, 32+ characters
      - [ ] `JwtEncoder` and `JwtDecoder` beans built from that secret (HS256)
      - [ ] `JwtService.issue(user)`: subject = user id, claim `role`, expires in 15 minutes
      ✔ App starts.

S9. - [ ] **Login**
      - [X] Service + endpoint, same 401 message for unknown email and wrong password
      - [ ] `@Valid` on the request body
      - [ ] Also reject a disabled user with the same 401
      - [ ] Return `AuthResponse` (token + user) instead of the user's details
      ✔ 200 with an `accessToken`; pasted into jwt.io it shows `sub`, `role`, and `exp` 15 min out.

S10. - [ ] **Accept tokens, answer 401/403 as JSON**
       - [ ] Turn on the resource server (`oauth2ResourceServer(...jwt...)`) in the filter chain
       - [ ] Map the `role` claim to the authority `ROLE_<role>`
       - [ ] Delete the empty `JwtAuthenticationFilter` class
       - [ ] Custom 401 and 403 handlers that write `ErrorResponse` JSON (these errors happen in the
             security filters, so the `@RestControllerAdvice` never sees them)
       ✔ `curl -i localhost:8080/api/v1/users/me` → 401 JSON; with a valid token → 404 (route comes
         in S11); with a tampered token → 401.

S11. - [ ] **Current user**: `GET /api/v1/users/me` and `PATCH /api/v1/users/me` `{ displayName }`
       - [ ] `UserService` with get-by-id and update-display-name; missing user → 404
       - [ ] `UserController` reads the user id from the token, never from the URL or body
       ✔ No token → 401; with token → 200 and the logged-in user. **Checkpoint: commit and push.**

S12. - [ ] **Roles**
       - [ ] Admin-only: `/api/v1/admin/**`, and POST/PUT/DELETE on books and categories.
             `GET /api/v1/categories` is public. Put these rules above `anyRequest()`.
       - [ ] Seed one admin on startup from `ADMIN_EMAIL` / `ADMIN_PASSWORD` if it does not exist
       ✔ `POST /api/v1/books`: no token → 401, user token → 403, admin token → not 401/403.

S13. - [ ] **Refresh tokens**: `POST /auth/refresh`, `POST /auth/logout`
       - [ ] `RefreshToken` entity (user, tokenHash unique, expiresAt, revokedAt) + repository
       - [ ] Token = 32 random bytes, Base64-URL. Store only its SHA-256 hash.
       - [ ] Login sets an `HttpOnly` cookie `refresh_token`, path `/api/v1/auth`, 7 days
       - [ ] Refresh: validate the cookie, revoke it, issue a new refresh + access token (rotation)
       - [ ] Logout: revoke the token, clear the cookie, return 204
       ✔ Refresh → 200 with a new token. Replaying the old cookie → 401. After logout → 401.

S14. - [ ] **CORS** for the frontend
       - [ ] Allowed origins from `APP_CORS_ORIGINS` (default `http://localhost:5173`),
             methods GET/POST/PUT/PATCH/DELETE/OPTIONS, headers Authorization + Content-Type,
             credentials allowed
       ✔ An OPTIONS preflight from `http://localhost:5173` returns the allow-origin header.

S15. - [ ] **Tests and ship**
       - [ ] MockMvc tests: 401 / 403 / 200 for one public, one user, one admin endpoint
       - [ ] Unit tests for the auth service: duplicate email, wrong password, unknown email
       - [ ] Render env vars: `JWT_SECRET`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `APP_CORS_ORIGINS`
       - [ ] Add `auth` and `me` to the links in `RootController`
       - [ ] Update `PROJECT_GUIDE.md` status and package list (new `security/` package)
       ✔ `./mvnw test` passes; register → login → `/users/me` works on Render.

## 📚 Stage T: Categories, genres, authors

Three small APIs with the same shape. Build **categories** end to end first, then copy the pattern.
Each one needs: entity → repository → DTOs → service → controller. Reuse the 404 exception, and the
409 exception for duplicates. Writes are admin-only (add the URL rule in the security config).

T1. - [ ] **Categories** `/api/v1/categories`
      - [ ] Entity: id, name (unique), slug (unique, from the name: lower-case, spaces → `-`)
      - [ ] `GET` list sorted by name (plain array, not paged)
      - [ ] `POST { name }` → 201, 409 if it exists
      - [ ] `PUT /{id}` → 200, 404 if missing
      - [ ] `DELETE /{id}` → 204, 404 if missing
      ✔ `GET` shows `[{"id":1,"name":"Programming","slug":"programming"}]`; posting it twice → 409.

T2. - [ ] **Genres** `/api/v1/genres`: same endpoints and shape as T1. First decide how a genre
      differs from a category. If they are the same thing, keep only categories.

T3. - [ ] **Authors** `/api/v1/authors`: id, name (required), bio (optional). Same endpoints as T1
      plus `GET /{id}`. Leave `Book.author` as a string for now; linking it is a separate step
      agreed with the frontend.

T4. - [ ] Add `categories`, `genres`, `authors` to the links in `RootController`.

## 📌 Stage 0: Pre-commit hook

Nothing is set up yet (no hook, no `package.json`). Husky is only a git-hook manager, so it works in
a Java repo too, but it needs Node. Pick **one** of these:

- **Husky** at the repo root (`book-api/`): `npm init -y`, `npm i -D husky`, `npx husky init`,
  add `node_modules/` to `.gitignore`, commit `package.json`, `package-lock.json`, `.husky/`.
- **Plain git hook, no Node**: a script in `.githooks/pre-commit`, then
  `git config core.hooksPath .githooks`. Commit the script.

0. - [ ] Pre-commit runs `cd book && ./mvnw -q compile` (switch to `./mvnw -q test` after Stage F)
   - [ ] *(Optional)* commit-msg check that messages follow `type(scope): message`
   ✔ A commit with a compile error is blocked; a clean commit goes through.

## 🧹 Stage A: Clean up what exists (done)

- [X] Constructor injection everywhere, no `@Autowired`
- [X] `ibsn` typo → `isbn`
- [X] `Book.id` is a `Long`; stray methods removed
- [X] Repository returns `Optional`, has `existsByIsbn` and a case-insensitive title search
- [X] One mapper: `BookResponseDto.from(Book)`
- [X] Package naming settled, dead code and H2 removed
- [ ] **Leftover:** `Book` still has a `price` field. Decide: keep it (Stage H purchases may need
      it) or remove it. Either way, write the decision in `PROJECT_GUIDE.md`.

## 🚨 Stage B: Error handling (done)

- [X] `ErrorResponse` uses `Instant` and has a `fieldErrors` map
- [X] Unknown id → 404 JSON through a `@RestControllerAdvice`
- [X] `@Valid` on the book request, bad input → 400 with `fieldErrors`
- [X] Bad JSON and wrong id type → 400; anything unexpected → 500 without a stack trace

## ✍️ Stage C: Books create, update, delete

Only `GET /books` and `GET /books/{id}` exist. The three write endpoints are placeholder comments
in the controller.

10. - [ ] **POST** `/api/v1/books` with `@Valid`. Return **201** with a `Location` header.
          Duplicate ISBN → 409 (reuse the 409 exception or add a `DuplicateIsbnException`).
11. - [ ] **PUT** `/api/v1/books/{id}`: 404 if missing, otherwise copy the fields and return 200.
12. - [ ] **DELETE** `/api/v1/books/{id}`: 404 if missing, otherwise 204 with no body.
13. - [ ] **Transactions:** `@Transactional` on writes (reads are already read-only). Look up
          "dirty checking" to see why PUT works without calling `save()`.
14. - [ ] **Service purity:** no `Http*`, `ResponseEntity`, or web annotation in the service.
15. - [X] `books` is in the links map in `RootController`.
16. - [ ] **Exercise everything**, locally and then on Render (first request after idle takes 30–60 s):

    ```bash
    curl -s -X POST localhost:8080/api/v1/books -H 'Content-Type: application/json' \
      -d '{"title":"Clean Code","author":"Robert C. Martin","isbn":"9780132350884","publisher":"Prentice Hall","publishedYear":"2008"}'
    curl -i localhost:8080/api/v1/books/999                                                 # 404
    curl -i -X POST localhost:8080/api/v1/books -H 'Content-Type: application/json' -d '{}'  # 400
    ```

    ✔ Every status code is right: 200 / 201 / 204 / 400 / 404 / 409.

## 🐘 Stage D: Database done properly

17. - [ ] POST a book, check it in SQL, restart the app, confirm the row is still there.
18. - [ ] **Flyway.** Put the schema in `src/main/resources/db/migration/V1__init.sql` (books and
          users tables) and switch `ddl-auto` to `validate` locally **and** on Render.
          ✔ App starts with `validate`; Render redeploys cleanly.

## 🟡 Stage E: Extra API features

19. - [ ] **PATCH** `/api/v1/books/{id}`: update only the fields that were sent. Note how it differs from PUT.
20. - [ ] **Search:** `GET /api/v1/books?author=…&title=…` using the derived queries.
21. - [ ] **Pagination & sorting:** `?page=0&size=10&sort=title,asc` with `Pageable`.

## 🧪 Stage F: Tests

22. - [ ] Unit-test `BookService` with a mocked repository (no Spring, no DB): unknown id, duplicate ISBN.
23. - [ ] `@WebMvcTest` for `BookController` with a mocked service: status codes and JSON only.
24. - [ ] One integration test against a real Postgres (Testcontainers).
          ✔ `./mvnw test` passes; service tests run in milliseconds.

## 🧩 Stage G: Feel Open/Closed

25. - [ ] `BookExporter` interface with CSV and JSON implementations, injected as a `List`, chosen with `?format=`.
          ✔ Adding an XML exporter means one new class and zero edited files.

## 🌱 Stage H: Borrowing, purchases, subscriptions

The last commit added `Borrowing`, `Purchase`, and `Subscription` entities plus `BurrowRequest` and
`PurchaseRequest` DTOs, but they are empty shells: no getters, no repositories, no services, and
`Subscription` is missing `@Entity`. Empty classes confuse readers, so either finish them in this
stage or delete them until you get here.

26. - [ ] **Decide the domain first** and write it in `PROJECT_GUIDE.md`: is this a library
          (borrow/return), a shop (purchase), or both? What does a subscription unlock?
27. - [ ] **Audit fields:** a `BaseEntity` with `createdAt` / `updatedAt`, used by every entity.
28. - [ ] **Book copies:** `totalCopies` / `availableCopies` on `Book`, and a `borrowOneCopy()`
          that refuses when none are left.
29. - [ ] **Borrowing:** finish the entity (lazy `@ManyToOne` to user and book, borrowedAt, dueDate,
          returnedAt), rename `BurrowRequest` → `BorrowRequest`, add repository + service.
          `POST /api/v1/books/{id}/borrow` and `POST /api/v1/books/{id}/return`, logged-in users only.
          ✔ Borrowing decrements `availableCopies` **and** creates a row, in one transaction, or does neither.
30. - [ ] **Purchase:** fix the `purachasedAt` typo, add repository + service + `POST /api/v1/books/{id}/purchase`.
31. - [ ] **Subscription:** add `@Entity`, decide the plans, add repository + service + endpoints.
32. - [ ] Turn on `show-sql` once and check the logs for N+1 queries on the list endpoints.

## 🚀 Stage I: Production polish

33. - [ ] **API docs:** springdoc-openapi + Swagger UI.
34. - [ ] `application-prod.yaml` profile (Render already sets `SPRING_PROFILES_ACTIVE=prod`).
35. - [ ] Actuator: only `/actuator/health` is public; the rest is off or admin-only.

---

## 🏁 Definition of done

- [ ] All endpoints work locally **and** on Render with the right status codes
- [ ] Data survives restarts; the schema is owned by Flyway with `ddl-auto: validate`
- [ ] No entity crosses the HTTP boundary; the service layer has zero web imports
- [ ] Every dependency is a `final` field set in the constructor
- [ ] Errors are structured JSON, never a stack trace
- [ ] Service unit tests run without a database
- [ ] You can explain, out loud, where each SOLID principle lives in the code
