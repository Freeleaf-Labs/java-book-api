# Book API: To-do

Everything left to build, in order, in one list.
For how to run the project, its conventions, and the concepts behind each step, see
[`PROJECT_GUIDE.md`](PROJECT_GUIDE.md).

*Last updated: 2026-10-05. Next step: **S1** (get the project compiling), see Stage S below.*

Do the steps in order. **Run → curl → commit → push → curl the Render URL** after each one
(see `PROJECT_GUIDE.md` → The dev loop). A step is done only when its **✔ check** passes.

**Order from here:** Sta****ge S (authentication) first, then T1–T3 and T5 (categories, genres, authors),
then the lettered stages.

## 🔐 Stage S: Authentication (do this first)

Register, login, JWT access token, protected routes, roles, refresh cookie. The JSON shapes and URL
rules are in the frontend repo: `../book/docs/BACKEND_TASKS.md` → Stage 3 and Part 2 (Auth, Users,
Access rules summary).

**How it fits together:** login checks the password and hands back a signed JWT. The client sends it
as `Authorization: Bearer <token>`. Spring Security verifies the signature on every request *before*
your controller runs, and puts the caller in the security context.

**Libraries:** `spring-boot-starter-security` plus `spring-boot-starter-oauth2-resource-server`. The
second one gives you `JwtEncoder`/`JwtDecoder` and a ready-made bearer-token filter, so you do **not**
write `JwtAuthenticationFilter` by hand (it gets deleted in S10).

S1–S11 give you a working register → login → protected-route loop. S12–S15 add roles, refresh
tokens, CORS and tests.

S1. - [ ] **Get the project compiling.** It does not compile right now, and the Husky pre-commit hook
      will block every commit until it does.
      - [ ] `AuthServices.login()` declares a return type but has an empty body. Delete the method
            for now; it comes back in S9.
      - [ ] `LoginRequestDto`: rename the field `title` → `email`, fix both messages (they were copied
            from the book DTO; password is 8–72 characters), remove the unused `Book`/`User` imports.
      - [ ] One repository per entity: make `UserRepository extends JpaRepository<User, Long>`, delete
            `AuthRepository`, and inject `UserRepository` into `AuthServices`.
      ✔ `cd book && ./mvnw -q compile` prints nothing and exits 0.

S2. - [ ] **Add the dependencies** to `pom.xml`.
      - [ ] `spring-boot-starter-security`
      - [ ] `spring-boot-starter-oauth2-resource-server`
      - [ ] `spring-boot-starter-security-test` with `<scope>test</scope>`
      ✔ The app starts, and `curl -i localhost:8080/api/v1/books` now returns **401**. That is
        expected: with Spring Security on the classpath everything is locked until S6 opens it up.

S3. - [ ] **`User` entity and `Role` enum.**
      - [ ] `Role` enum in `model/`: `USER`, `ADMIN`
      - [ ] `@Table(name = "users")`. *(Why: `user` is a reserved word in PostgreSQL, so a table with
            that name fails to create.)*
      - [ ] Fields: `email` (`unique = true, nullable = false`), `passwordHash` (rename `password`),
            `displayName` (rename `fullname`), `role` with `@Enumerated(EnumType.STRING)`,
            `enabled` (default `true`), `createdAt` (`Instant`, set with `@CreationTimestamp`).
            Keep or drop `phone`; it is not in the contract.
      - [ ] Getters, the setters you need, and `equals`/`hashCode` done the same way as `Book`.
      - [ ] Local DB only: `docker compose down -v` once so the tables are recreated cleanly
            (`ddl-auto: update` adds columns but never renames or drops them).
      ✔ `docker compose exec postgres psql -U book -d bookdb -c '\d users'` lists the columns, with a
        unique index on `email`.

S4. - [ ] **`UserRepository` queries.** `Optional<User> findByEmail(String email)` and
      `boolean existsByEmail(String email)`. Emails are compared lower-case, so the service lower-cases
      before saving and before looking up.
      ✔ Compiles; the app still starts.

S5. - [ ] **DTOs as records** (replace the empty classes).
      - [ ] `RegisterRequest`: `email` (`@NotBlank @Email`), `password` (`@NotBlank @Size(min = 8, max = 72)`),
            `displayName` (`@NotBlank @Size(min = 2, max = 50)`). *(Why 72: BCrypt ignores everything
            after 72 bytes.)*
      - [ ] `UserResponse`: `id, email, displayName, role, enabled, createdAt`, with a static
            `from(User)` like `BookResponseDto.from(Book)`. It must **never** contain the password.
      - [ ] `AuthResponse`: `accessToken`, `expiresIn` (seconds, `900`), `user` (`UserResponse`)
      ✔ Compiles.

S6. - [ ] **`SecurityConfig`, first version.** `@Configuration` + `@EnableWebSecurity`, one
      `SecurityFilterChain` bean.
      - [ ] `csrf` disabled and `SessionCreationPolicy.STATELESS`. *(Why: there is no session cookie
            to protect; every request carries its own token.)*
      - [ ] `permitAll()`: `/`, `/actuator/health`, `/api/v1/auth/**`, and `GET /api/v1/books/**`
      - [ ] `anyRequest().authenticated()`
      - [ ] A `PasswordEncoder` bean: `new BCryptPasswordEncoder()`
      - [ ] `DuplicateResourceException` in `exception/`, mapped to **409** in `GlobalExceptionHandler`
            (S7 needs it; T1–T3 reuse it).
      ✔ `curl -i localhost:8080/api/v1/books` is **200** again, and
        `curl -i -X POST localhost:8080/api/v1/books` is **401**.

S7. - [ ] **Register.**
      - [ ] `AuthServices.register(RegisterRequest)`: lower-case the email → `existsByEmail` → throw
            `DuplicateResourceException` → otherwise save with `passwordEncoder.encode(password)`,
            role `USER`, enabled `true` → return `UserResponse`. Mark it `@Transactional`.
      - [ ] New `AuthController` at `/api/v1/auth`: `POST /register` with `@Valid @RequestBody` → **201**

      ```bash
      curl -i -X POST localhost:8080/api/v1/auth/register -H 'Content-Type: application/json' \
        -d '{"email":"asha@example.com","password":"password123","displayName":"Asha"}'
      ```

      ✔ First call → 201 with no `password` key; same call again → 409; `{}` → 400 with `fieldErrors`;
        `select email, password_hash from users;` shows a hash starting with `$2a$`, not the password.

S8. - [ ] **Issue JWTs.**
      - [ ] `application.yaml`: `app.jwt.secret: ${JWT_SECRET:<a dev-only string of 32+ characters>}`.
            HS256 needs a key of at least 256 bits, so a shorter secret fails at startup.
      - [ ] In `SecurityConfig`, build one `SecretKey` (`new SecretKeySpec(secret.getBytes(UTF_8), "HmacSHA256")`)
            and expose two beans from it: `JwtEncoder` (`NimbusJwtEncoder.withSecretKey(key).build()`)
            and `JwtDecoder` (`NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build()`).
      - [ ] `JwtService` (`@Service`) with `String issue(User user)`: claims `sub` = user id as a string,
            `role` = role name, `issuedAt` = now, `expiresAt` = now + 15 minutes. Pass
            `JwsHeader.with(MacAlgorithm.HS256).build()` to `JwtEncoderParameters.from(header, claims)`,
            otherwise the encoder assumes RS256 and fails.
      ✔ The app starts. (You see a real token in S9.)

S9. - [ ] **Login.**
      - [ ] `InvalidCredentialsException` in `exception/`, mapped to **401** in `GlobalExceptionHandler`
            with the message `Invalid email or password`.
      - [ ] `AuthServices.login(LoginRequestDto)`: `findByEmail` (lower-cased) →
            `passwordEncoder.matches(raw, user.getPasswordHash())` → check `enabled` → return
            `new AuthResponse(jwtService.issue(user), 900, UserResponse.from(user))`.
            Unknown email, wrong password and disabled user all throw the **same** exception.
            *(Why: a different message for "no such email" tells an attacker which emails are registered.)*
      - [ ] `AuthController`: `POST /login` → 200 `AuthResponse`

      ```bash
      curl -s -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
        -d '{"email":"asha@example.com","password":"password123"}'
      ```

      ✔ 200 with an `accessToken`; pasted into jwt.io it shows `sub`, `role` and an `exp` 15 minutes
        out. A wrong password and an unknown email return the identical 401 body.

S10. - [ ] **Accept tokens, and answer 401/403 in JSON.**
       - [ ] In the filter chain: `.oauth2ResourceServer(oauth -> oauth.jwt(...))`. It finds your
             `JwtDecoder` bean and verifies signature and expiry on every request.
       - [ ] A `JwtAuthenticationConverter` bean whose `JwtGrantedAuthoritiesConverter` uses
             `setAuthoritiesClaimName("role")` and `setAuthorityPrefix("ROLE_")`, so the claim
             `"role": "ADMIN"` becomes the authority `ROLE_ADMIN`.
       - [ ] Delete the empty `JwtAuthenticationFilter` class.
       - [ ] Custom `AuthenticationEntryPoint` (401) and `AccessDeniedHandler` (403) that write
             `ErrorResponse.of(...)` as JSON with the `ObjectMapper`. Register them in
             `.exceptionHandling(...)` **and** on `.oauth2ResourceServer(...)`.
             *(Why: these errors are raised in the security filters, before any controller runs, so
             `@RestControllerAdvice` never sees them.)*
       ✔ `curl -i localhost:8080/api/v1/users/me` → 401 with the `ErrorResponse` JSON body.
         With `-H "Authorization: Bearer $TOKEN"` → 404 (the route does not exist until S11), not 401.
         With a token you edited by one character → 401.

S11. - [ ] **`GET /api/v1/users/me`** (and `PATCH /users/me` with `{ displayName }`).
       - [ ] `UserService` with `getById(Long)` and `updateDisplayName(Long, String)`; a missing user
             throws `ResourceNotFoundException`.
       - [ ] `UserController`: take `@AuthenticationPrincipal Jwt jwt` and use
             `Long.valueOf(jwt.getSubject())`. Never read the user id from the URL or the body.
       - [ ] `UpdateProfileRequest` record with the same `displayName` rule as register.

       ```bash
       TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
         -d '{"email":"asha@example.com","password":"password123"}' | jq -r .accessToken)
       curl -s localhost:8080/api/v1/users/me -H "Authorization: Bearer $TOKEN"
       ```

       ✔ No token → **401**; with the token → 200 and the logged-in user, with no `password` key.
         **Checkpoint:** register → login → protected route works end to end. Commit and push.

S12. - [ ] **Roles.**
       - [ ] URL rules from the contract (Access rules summary), placed **above** `anyRequest()`:
             `/api/v1/admin/**` and `POST`/`PUT`/`DELETE` on `/api/v1/books/**` and
             `/api/v1/categories/**` → `hasRole("ADMIN")`; `GET /api/v1/categories` → `permitAll()`.
       - [ ] Seed one admin on startup: a `CommandLineRunner` bean that creates the user from
             `ADMIN_EMAIL` / `ADMIN_PASSWORD` if that email does not exist yet. Register always
             creates `USER`, so this is the only way an admin appears.
       ✔ `curl -i -X POST localhost:8080/api/v1/books`: no token → **401**, USER token → **403**,
         ADMIN token → neither (405 until Stage C adds the POST endpoint, then 201).

S13. - [ ] **Refresh tokens** (`POST /auth/refresh`, `POST /auth/logout`).
       - [ ] `RefreshToken` entity: `user` (`@ManyToOne`), `tokenHash` (unique), `expiresAt`,
             `revokedAt`; plus `RefreshTokenRepository.findByTokenHash`.
       - [ ] Token = 32 random bytes from `SecureRandom`, Base64-URL encoded. Store only its SHA-256
             hash. *(Why: if the database leaks, the hashes cannot be used to log in.)*
       - [ ] Login also sets a cookie built with `ResponseCookie`: name `refresh_token`, `HttpOnly`,
             `Secure` (off locally, on in prod), `SameSite=Lax`, `Path=/api/v1/auth`, max age 7 days.
       - [ ] `POST /auth/refresh`: read the cookie with `@CookieValue`, hash it, look it up, reject if
             missing, expired, revoked or the user is disabled (401). Otherwise revoke it, issue a new
             refresh token and a new access token (**rotation**), and return `AuthResponse`.
       - [ ] `POST /auth/logout`: revoke the token, clear the cookie (max age 0), return **204**.
       ✔ `curl -c jar.txt` on login, then `curl -b jar.txt -c jar.txt -X POST …/auth/refresh` → 200
****         with a new `accessToken`. Replaying the **old** cookie value → 401. After logout, refresh → 401.

S14. - [ ] **CORS** for the frontend.
       - [ ] A `CorsConfigurationSource` bean: origins from `app.cors.origins: ${APP_CORS_ORIGINS:http://localhost:5173}`,
             methods `GET, POST, PUT, PATCH, DELETE, OPTIONS`, headers `Authorization` and
             `Content-Type`, `allowCredentials = true`.
       - [ ] `.cors(Customizer.withDefaults())` in the filter chain.
       ✔ `curl -i -X OPTIONS localhost:8080/api/v1/auth/login -H 'Origin: http://localhost:5173' -H 'Access-Control-Request-Method: POST'`
         returns `Access-Control-Allow-Origin: http://localhost:5173` and
         `Access-Control-Allow-Credentials: true`.

S15. - [ ] **Tests and ship.**
       - [ ] Security tests with MockMvc: 401 / 403 / 200 for one public, one user and one admin
             endpoint (`spring-security-test` gives you `jwt()` to fake a logged-in caller).
       - [ ] Unit-test `AuthServices` with mocks: duplicate email → exception; wrong password and
             unknown email → the same exception.
       - [ ] Render env vars: `JWT_SECRET` (long and random, **not** the dev default), `ADMIN_EMAIL`,
             `ADMIN_PASSWORD`, `APP_CORS_ORIGINS`.
       - [ ] Add `"auth": "/api/v1/auth"` and `"me": "/api/v1/users/me"` to the links map in `RootController`.
       - [ ] Update `PROJECT_GUIDE.md` §1 Status and §2 (the new `security/` package).
       ✔ `./mvnw test` passes, and register → login → `/users/me` works against the Render URL.

## 📚 After auth: categories, genres, authors

Three small APIs with the same shape, so build **categories** end to end first, then copy the
pattern. The JSON shape for categories is in the frontend repo: `../book/docs/BACKEND_TASKS.md` →
Part 2. **Genres and authors are not in that contract yet**, so the shapes below are a proposal; once
they are settled, add them to the contract so the frontend can use them.

Each of T1–T3 needs: entity → repository → request/response DTO (with a static `from(...)`) →
service → controller. Reuse `ResourceNotFoundException` for 404s and the `DuplicateResourceException`
(409) you created in S6. Writes are admin-only: the `/categories/**` rule is already in S12; add the
same rule for `/genres/**` and `/authors/**` when you build them.

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

T5. - [ ] **Link them.** Add `categories`, `genres` and `authors` to the links map in `RootController`.

## 📌 Stage 0: Git hooks with Husky (done)

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
33. - [ ] Restrict Actuator exposure: only `/actuator/health` is public (S6); expose nothing else, or make the rest admin-only.
34. - [X] Spring Security: moved to **Stage S** at the top of this file.

---

## 🏁 Definition of done

- [ ] All endpoints work locally **and** on Render with correct status codes
- [ ] Data persists across restarts; schema is owned by Flyway with `ddl-auto: validate`
- [ ] No Entity crosses the HTTP boundary; the service layer has zero web imports
- [ ] Every dependency is a `final` field injected through the constructor
- [ ] Errors are structured JSON, never a stack trace
- [ ] Service unit tests run without a database
- [ ] You can explain, out loud, where each SOLID principle lives in your code
