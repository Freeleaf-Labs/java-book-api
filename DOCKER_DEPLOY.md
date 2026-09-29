****# Book API — JAR, Docker & Render Deployment

Companion to `PROJECT_GUIDE.md`. This replaces the placeholder **Section 9 — Deployment**.

> **Read order:** Sections 1–2 are *understanding* (what a JAR is, what Docker is).
> Section 3 onward is *doing*. Section 7 answers "do I need to update the Dockerfile
> every time?" — short answer: almost never, and the table there says exactly when.

---

## 1. Why Docker at all

Render has native runtimes for Node, Python, Ruby, Go, Rust, Elixir and PHP. **Java is not
one of them.** Render *does* deploy any Docker image, so the route for a Spring Boot app is:

```
your source  →  a JAR  →  a Docker image  →  a running container on Render
```

Everything below is just making each of those three arrows explicit and repeatable.

---

## 2. What a JAR actually is

### 2.1 From `.java` to a running process

```
Book.java          your source, human-readable
   ↓  javac (the compiler)
Book.class         bytecode — instructions for the JVM, not for your CPU
   ↓  java (the JVM)
a running process
```

The JVM is the thing that turns bytecode into real CPU instructions at runtime. That is why
the same `.class` file runs unchanged on Linux, macOS and Windows — the *JVM* differs per
platform, your compiled code does not.

| Term | What it is |
|---|---|
| **JVM** | Java Virtual Machine — executes bytecode. The runtime engine. |
| **JRE** | JVM + the standard library. Enough to *run* Java. Cannot compile. |
| **JDK** | JRE + `javac`, `jar`, `jlink`, debugger. Needed to *build*. |
| **Bytecode** | The `.class` instruction format the JVM understands |
| **Classpath** | The list of places the JVM searches for classes at runtime |

**Consequence you will use in Section 4:** the build stage needs a **JDK**; the stage that
actually runs your app only needs a **JRE**. That is the whole argument for multi-stage
builds.

### 2.2 The JAR file itself

A JAR (**J**ava **AR**chive) is *literally a ZIP file* with a required internal layout. Rename
`book.jar` to `book.zip` and your file manager will open it.

```bash
jar tf target/book-0.0.1-SNAPSHOT.jar | head -20   # list contents
unzip -p target/book-0.0.1-SNAPSHOT.jar META-INF/MANIFEST.MF   # read the manifest
```

It contains:

| Inside the JAR | What it is |
|---|---|
| `*.class` files | Your compiled code, in folders matching the package name |
| `application.yaml`, static files | Everything from `src/main/resources` |
| `META-INF/MANIFEST.MF` | A small text file of key–value metadata |

The manifest is what makes a JAR *runnable*. If it contains a `Main-Class:` line, then
`java -jar thefile.jar` works. Without it you get the classic error:

```
no main manifest attribute, in book-0.0.1-SNAPSHOT.jar
```

That error means "this is a library JAR, not an executable one".

### 2.3 The Spring Boot "fat JAR"

Plain Java has a problem: your app depends on ~60 other JARs (Spring, Hibernate, Jackson,
Tomcat…). Normally you would have to ship all 60 and list them on the classpath by hand.

The `spring-boot-maven-plugin` — already in your `pom.xml` — solves this in a step called
**repackaging**. After Maven builds the ordinary JAR, the plugin rewrites it into an
*executable* JAR:

```
book-0.0.1-SNAPSHOT.jar
├── META-INF/MANIFEST.MF
│     Main-Class:  org.springframework.boot.loader.launch.JarLauncher   ← Boot's launcher
│     Start-Class: np.com.milapmagar.book.BookApplication               ← YOUR main()
├── org/springframework/boot/loader/...   the launcher's own classes
└── BOOT-INF/
    ├── classes/    ← your compiled code + resources
    └── lib/        ← all ~60 dependency JARs, nested whole
```

So when you run `java -jar book.jar`:

1. The JVM reads `Main-Class` and starts **JarLauncher**, not your code.
2. JarLauncher installs a custom classloader that can read JARs *nested inside* a JAR
   (standard Java cannot do this — this is Boot's trick).
3. It then calls the `main()` of `Start-Class` — your `BookApplication`.
4. Spring Boot starts the embedded Tomcat that was sitting in `BOOT-INF/lib/`.

**That last point is the important one for deployment:** the web server is *inside* the JAR.
There is no Tomcat to install on the server. One file, one `java -jar`, and you have an HTTP
service. This is exactly what makes containerising a Spring Boot app so simple.

You will also find `book-0.0.1-SNAPSHOT.jar.original` in `target/`. That is the plain,
pre-repackaging JAR that Maven built first. **Never deploy the `.original`** — it has no
launcher and no dependencies.

### 2.4 The commands

```bash
./mvnw clean package              # compile, run tests, build + repackage the jar
./mvnw clean package -DskipTests  # same, skipping test execution
ls -lh target/*.jar               # the artifact, ~25-50 MB for this app

java -jar target/book-0.0.1-SNAPSHOT.jar
java -jar target/book-0.0.1-SNAPSHOT.jar --server.port=9090
java -jar target/book-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

The file name comes straight from your `pom.xml`:

```xml
<artifactId>book</artifactId>          →  book
<version>0.0.1-SNAPSHOT</version>      →  -0.0.1-SNAPSHOT.jar
```

Remember that — it matters in Section 7.

> **`SNAPSHOT`** means "this version is still moving". Maven treats snapshot versions as
> re-downloadable at any time. A release version has no suffix (`1.0.0`).

### 2.5 JAR vs WAR

| | JAR (what you have) | WAR |
|---|---|---|
| Contains a web server | Yes, embedded Tomcat | No |
| How it runs | `java -jar app.jar` | Deployed *into* an external Tomcat/WildFly |
| Docker-friendly | Very | Awkward |
| Modern default | Yes | Legacy / corporate app-server estates |

You want the JAR. Nothing in this project should ever produce a WAR.

---

## 3. Docker vocabulary

The same relationship you have been learning in OOP:

| OOP | Docker |
|---|---|
| Class — a definition | **Image** — a read-only filesystem snapshot |
| Object — a live instance | **Container** — a running process from that image |
| Constructor | `docker run` |

| Term | Meaning |
|---|---|
| **Dockerfile** | A text recipe. Each instruction produces one image **layer**. |
| **Layer** | A cached filesystem diff. Unchanged layers are reused — this is why instruction *order* matters. |
| **Build context** | The directory you point `docker build` at. Docker can only `COPY` files from inside it. |
| **Tag** | A name for an image: `book-api:latest`, `eclipse-temurin:25-jre` |
| **Registry** | Where images are stored (Docker Hub, GHCR). Render builds yours itself, so you do not need one. |
| **Base image** | The `FROM` line — someone else's image you build on top of |

---

## 4. The Dockerfile

### 4.1 Where it goes

```
book-api/                 ← git repo root
├── PROJECT_GUIDE.md
├── DOCKER_DEPLOY.md
└── book/                 ← the Maven project
    ├── pom.xml
    ├── mvnw
    ├── .mvn/
    ├── src/
    ├── Dockerfile        ← HERE, next to pom.xml
    └── .dockerignore     ← and here
```

It must sit beside `pom.xml` because the build context will be `book/`, and `COPY` cannot reach outside the context. Since your repo root is `book-api/` but the app is in `book/`,
you will tell Render **Root Directory = `book`** (Section 6).

### 4.2 The version to use

```dockerfile
# syntax=docker/dockerfile:1

# ───────────────────────── Stage 1: build ─────────────────────────
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Copy ONLY the build descriptor first. These files change rarely, so the
# expensive dependency download below stays cached across most rebuilds.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

# Source changes on every commit, so it comes last.
COPY src/ src/
RUN ./mvnw -B -ntp clean package -DskipTests

# ───────────────────────── Stage 2: runtime ───────────────────────
FROM eclipse-temurin:25-jre
WORKDIR /app

# Never run as root inside a container.
RUN groupadd --system spring && \
    useradd  --system --gid spring --uid 10001 spring

# Only the finished artifact crosses from the build stage.
# The JDK, Maven, and ~500 MB of source and .m2 cache are left behind.
COPY --from=build --chown=spring:spring /workspace/target/*.jar app.jar

USER spring

# Documentation only — it publishes nothing by itself.
EXPOSE 8080

# Exec form (no shell): java becomes PID 1 and receives SIGTERM directly,
# so Spring's graceful shutdown actually runs when Render stops the container.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
```

### 4.3 Line by line

| Instruction | Why it is there |
|---|---|
| `FROM eclipse-temurin:25-jdk AS build` | A full JDK — you cannot compile with a JRE. `AS build` names the stage so stage 2 can reach into it. Temurin is the Eclipse Foundation's OpenJDK build; `25` matches `<java.version>25</java.version>` in your pom. |
| `WORKDIR /workspace` | Sets the working directory (creates it if missing). Everything after is relative to it. |
| `COPY .mvn/ .mvn/` + `COPY mvnw pom.xml ./` | Your wrapper is `distributionType=only-script`, so `.mvn/wrapper/maven-wrapper.properties` is what tells `mvnw` which Maven to download. Both are needed before any build runs. |
| `chmod +x mvnw` | Git does not always preserve the executable bit. Without this: `./mvnw: Permission denied`. |
| `dependency:go-offline` | Downloads every dependency **before** the source is copied, so this whole layer is cached and skipped when only your Java changes. This is the single biggest build-time win. |
| `-B -ntp` | Batch mode, no transfer progress. Stops thousands of download-progress lines flooding the build log. |
| `COPY src/ src/` | Last, because it changes most often. Everything after a changed layer is rebuilt. |
| `package -DskipTests` | Builds + repackages the fat JAR. Tests are skipped — see the note below. |
| `FROM eclipse-temurin:25-jre` | **Stage 2 starts from scratch.** Nothing from stage 1 exists here unless explicitly copied. A JRE image is roughly a third the size of a JDK one, and ships no compiler for an attacker to use. |
| `useradd … spring` / `USER spring` | Containers run as root by default. A root process that escapes the container is a much worse day than a non-root one. |
| `COPY --from=build …/target/*.jar app.jar` | The one thing that crosses the stage boundary. The wildcard is deliberate — see Section 7. |
| `EXPOSE 8080` | Metadata for humans and tooling. It does not open a port; `-p` and Render's config do that. |
| `-XX:MaxRAMPercentage=75.0` | Tells the JVM to size its heap from the *container's* memory limit rather than the host's. Without it, on Render's 512 MB free tier, the JVM can size a heap larger than the container is allowed and get OOM-killed. |
| `ENTRYPOINT [...]` | JSON/exec form. The shell form (`ENTRYPOINT java -jar app.jar`) wraps it in `/bin/sh`, which becomes PID 1, swallows SIGTERM, and your app gets killed hard after a timeout instead of shutting down cleanly. |

> **On `-DskipTests`:** tests belong in CI, not in the deploy build. Running them here means
> a flaky test blocks a production deploy, and every deploy pays the test runtime. Your
> `BookApplicationTests` also needs a database context, which is not available during an
> image build. Keep `./mvnw test` as a thing you run locally and in CI.

> **If `eclipse-temurin:25-jre` does not exist** when you try it (JRE variants sometimes lag
> the JDK for a new release), use `eclipse-temurin:25-jdk` for stage 2 as well. The image is
> bigger, nothing else changes. Check with: `docker pull eclipse-temurin:25-jre`

### 4.4 `.dockerignore`

Same idea as `.gitignore`, but for the build context. Without it, Docker uploads your entire
`target/`, `.git/` and `.idea/` to the build daemon on every single build.

Create `book/.dockerignore`:

```
target/
.git/
.gitignore
.idea/
*.iml
*.log
HELP.md
compose.yaml
Dockerfile
.dockerignore
```

Excluding `target/` is not just about speed — it also guarantees the JAR in your image was
built *inside* the image, never a stale one from your laptop.

### 4.5 Why layer order is the whole game

Docker caches each instruction. On a rebuild it reuses layers until it hits the first one
whose inputs changed, then rebuilds that one **and everything after it**.

```
COPY .mvn/ .mvn/            ← changes ~never       │ cached
COPY mvnw pom.xml ./        ← changes on new dep   │ cached
RUN ./mvnw dependency:...   ← 60+ downloads, slow  │ cached  ← the prize
COPY src/ src/              ← changes every commit │ rebuilt
RUN ./mvnw package          ← compile only, fast   │ rebuilt
```

Flip it — `COPY . .` as the first instruction — and every one-character change to a
controller re-downloads the whole dependency tree. Same result, several minutes slower,
every time.

---

## 5. Making the app container-ready

The Dockerfile is only half of it. Three things in the app itself must change.

### 5.1 The port

Render assigns your service a port at runtime and passes it in the **`PORT`** environment
variable. Your app must bind to it, or Render reports *"No open ports detected"* and kills
the deploy.

In `application.yaml`:

```yaml
server:
  port: ${PORT:8080}
```

Read that as: *use `$PORT` if it is set, otherwise 8080*. Locally nothing changes; on Render
it just works. Do not hardcode `8080`.

Spring binds to `0.0.0.0` by default, which is correct here. `localhost` inside a container
means *that container only* and would be unreachable.

### 5.2 Credentials and the database

**Never bake credentials into the image.** An image is a file — anyone who pulls it can read
every layer. Configuration comes in as environment variables at *run* time.

Spring Boot's relaxed binding maps env vars to properties automatically:

| Environment variable | Property it sets |
|---|---|
| `SPRING_DATASOURCE_URL` | `spring.datasource.url` |
| `SPRING_DATASOURCE_USERNAME` | `spring.datasource.username` |
| `SPRING_DATASOURCE_PASSWORD` | `spring.datasource.password` |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `spring.jpa.hibernate.ddl-auto` |
| `SPRING_PROFILES_ACTIVE` | `spring.profiles.active` |

The rule: uppercase, dots and dashes become underscores. So `application.yaml` holds only
safe defaults for local development, and Render supplies the real values.

```yaml
spring:
  application:
    name: book
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/bookdb}
    username: ${SPRING_DATASOURCE_USERNAME:book}
    password: ${SPRING_DATASOURCE_PASSWORD:book}
  jpa:
    hibernate:
      ddl-auto: ${SPRING_JPA_HIBERNATE_DDL_AUTO:update}
    show-sql: false
server:
  port: ${PORT:8080}
```

> **The trap that catches everyone.** Render's Postgres dashboard gives you a connection
> string like:
>
> ```
> postgresql://book:secret@dpg-abc123-a.oregon-postgres.render.com/bookdb
> ```
>
> That is a **libpq** URL. JDBC cannot parse it — it needs a `jdbc:` prefix and it will not
> read the user and password out of the URL. Split it by hand:
>
> ```
> SPRING_DATASOURCE_URL       jdbc:postgresql://dpg-abc123-a/bookdb
> SPRING_DATASOURCE_USERNAME  book
> SPRING_DATASOURCE_PASSWORD  secret
> ```
>
> Use Render's **Internal Database URL** host when the database and the web service are in
> the same Render region — it does not leave Render's network and is faster and free.

### 5.3 `ddl-auto` in production

`PROJECT_GUIDE.md` §5.3 already warns about this. In a deployed environment it matters more:
a container restarts on its own schedule, and `create-drop` would wipe your data silently.
Set `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` on Render once Flyway (Phase 7) owns the schema.
Until then `update` is survivable but you are accepting silent drift on a real database.

### 5.4 Health check

Actuator is already a dependency and exposes `/actuator/health` by default. Give that path to
Render as the health check — it will not route traffic to the container until it returns 200,
which prevents a broken deploy from replacing a working one.

---

## 6. Build and run locally — before you touch Render

Never debug a Dockerfile through a cloud deploy log. Everything below runs on your machine.

```bash
cd book-api/book

docker build -t book-api:local .        # the trailing "." is the build context
docker images book-api                  # check the size — expect ~250-400 MB

docker run --rm -p 8080:8080 book-api:local
```

`-p 8080:8080` is `hostPort:containerPort`. `--rm` deletes the container when it stops.

Then, in another terminal:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/books
```

Simulate Render's port injection:

```bash
docker run --rm -e PORT=10000 -p 8080:10000 book-api:local
# Render sets PORT; the container listens on 10000; you still reach it on localhost:8080
```

Useful while debugging:

```bash
docker ps                          # what is running
docker logs -f <container-id>      # follow the logs
docker exec -it <container-id> sh  # a shell inside the running container
docker run --rm -it --entrypoint sh book-api:local   # a shell instead of the app
docker build --no-cache -t book-api:local .          # ignore the layer cache
docker image prune                                   # reclaim disk
```

Inside that shell, `ls -l /app` should show exactly one file: `app.jar`.

### 6.1 Talking to your local Postgres

`localhost` inside the container is the container itself, not your laptop. Two fixes:

```bash
# Option A — reach the host from the container
docker run --rm -p 8080:8080 \
  --add-host=host.docker.internal:host-gateway \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/bookdb \
  book-api:local
```

Option B — add the app to the `compose.yaml` you already have, so both containers share a
network and can address each other by service name:

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
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U book -d bookdb"]
      interval: 5s
      retries: 10

  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/bookdb
      SPRING_DATASOURCE_USERNAME: book
      SPRING_DATASOURCE_PASSWORD: book
    depends_on:
      postgres:
        condition: service_healthy

volumes:
  book-pgdata:
```

```bash
docker compose up --build       # builds the image and starts both
docker compose down
```

Note `jdbc:postgresql://postgres:5432/...` — the hostname is the *service name*. Compose runs
a DNS server on the shared network that resolves it. This is the closest local mirror of how
Render wires your web service to your database.

---

## 7. Deploying to Render

1. Commit `Dockerfile` and `.dockerignore`, and push to GitHub.
2. Render → **New** → **PostgreSQL**. Note the Internal Database URL, user and password.
3. Render → **New** → **Web Service** → connect the repo.
4. **Runtime: Docker.** Render detects the Dockerfile and hides the build/start commands —
   the Dockerfile *is* the build and start command now.
5. **Root Directory: `book`** — because your Dockerfile and pom are one level down. Get this
   wrong and the build fails with "Dockerfile not found".
6. **Health Check Path: `/actuator/health`**
7. Environment variables:

   | Key | Value |
   |---|---|
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<internal-host>/bookdb` |
   | `SPRING_DATASOURCE_USERNAME` | from the Render database page |
   | `SPRING_DATASOURCE_PASSWORD` | from the Render database page |
   | `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` now, `validate` after Flyway |
   | `SPRING_PROFILES_ACTIVE` | `prod` |

   Do **not** set `PORT` — Render sets it for you.
8. Deploy, and watch the log. You are looking for `Tomcat started on port(s): 10000` followed
   by `Started BookApplication in N seconds`.

**Free tier realities:** the service spins down after ~15 minutes of no traffic, and a JVM
cold start is slow — expect 30–60 s on the first request after idling. The filesystem is
ephemeral, so anything written to disk inside the container is lost on restart. That is fine
here, because your state lives in Postgres.

---

## 8. Do you have to update the Dockerfile every time? — **No.**

This is the question worth being precise about.

A Dockerfile is a **recipe, not a snapshot**. It says *"take a JDK, copy whatever source is
here, build it"*. It never names a specific version of your code. So when you push new
commits, Render re-runs the same recipe against the new source and gets a new image. The
Dockerfile itself is untouched.

Write it once. Then:

| What changed | Rebuild happens | Edit the Dockerfile? |
|---|---|---|
| Java source — new controller, service, entity | Automatic on push | **No** |
| New dependency added to `pom.xml` | Automatic on push | **No** |
| `application.yaml` edited | Automatic on push | **No** |
| Project version `0.0.1-SNAPSHOT` → `0.0.2` | Automatic on push | **No** — *because you used `target/*.jar`* |
| An environment variable's value | Render restarts the container | **No** |
| Java 25 → Java 26 | Yes | **Yes** — both `FROM` lines, and `<java.version>` in the pom |
| App needs a runtime tool (`curl`, `tzdata`, fonts) | Yes | **Yes** — add a `RUN apt-get install` to stage 2 |
| Switch Maven → Gradle | Yes | **Yes** — the whole build stage |
| Need JVM flags (heap, GC, timezone) | Yes | **Yes** — edit `ENTRYPOINT` (or set `JAVA_TOOL_OPTIONS` as an env var instead, no edit needed) |
| Build got slow / caching wrong | Yes | **Yes** — reorder the `COPY` lines |

So in practice: **you edit it when the *shape* of the build or the runtime environment
changes — not when your code changes.** For a project like this, that is a handful of times
over its whole life.

### The one thing that breaks this

If you hardcode the JAR name:

```dockerfile
COPY --from=build /workspace/target/book-0.0.1-SNAPSHOT.jar app.jar   # fragile
```

then the day you bump `<version>` to `0.0.2-SNAPSHOT`, the build fails with
*"file not found"* and you must remember to edit the Dockerfile in lockstep. The wildcard
avoids that entirely:

```dockerfile
COPY --from=build /workspace/target/*.jar app.jar                     # version-proof
```

It works because `spring-boot-maven-plugin` leaves exactly one `.jar` in `target/` (the
plain build output is renamed to `.jar.original`, which the glob does not match).

The alternative, if you prefer an explicit name, is to pin it in `pom.xml`:

```xml
<build>
  <finalName>app</finalName>
  ...
</build>
```

Then `COPY --from=build /workspace/target/app.jar app.jar` is stable forever. Either is fine
— pick one and do not mix them.

---

## 9. Failure modes

| Symptom | Cause | Fix |
|---|---|---|
| `no main manifest attribute` | You copied `.jar.original`, or `spring-boot-maven-plugin` did not run | Use `target/*.jar`; confirm the plugin is in `pom.xml` |
| `COPY failed: no source files were specified` | Wrong path, or `target/` was excluded from the *build stage* by `.dockerignore` — note stage 1 builds it fresh, so this is normally a path typo | Check `WORKDIR` matches the `COPY --from` path |
| `./mvnw: Permission denied` | Lost executable bit | `chmod +x mvnw` in the Dockerfile, and `git update-index --chmod=+x mvnw` |
| Render: *"No open ports detected"* | App bound to 8080, Render expected `$PORT` | `server.port: ${PORT:8080}` |
| Container exits immediately, code 137 | OOM-killed | `-XX:MaxRAMPercentage=75.0`, or a larger instance |
| `Connection refused: localhost:5432` | `localhost` inside a container is the container | Use `host.docker.internal`, a compose service name, or Render's internal host |
| `The connection attempt failed` / driver error on Render | `postgresql://` URL passed to JDBC | Prefix with `jdbc:`, split out user and password |
| Every build takes 5+ minutes | `COPY . .` before the dependency step | Restore the layer order in §4.2 |
| Build fails in `dependency:go-offline` | A plugin resolves oddly offline | Delete that `RUN` line — you lose the cache win, not correctness |
| App starts, then `Table "BOOKS" not found` | `ddl-auto: none`/`validate` with no migrations | `update` while learning, Flyway after Phase 7 |

---

## 10. Checklist

- [X] `book/Dockerfile` created, multi-stage, non-root, exec-form `ENTRYPOINT`
- [X] `book/.dockerignore` created
- [X] `server.port: ${PORT:8080}` in `application.yaml`
- [X] Datasource values read from environment variables with local defaults
- [X] No password committed anywhere
- [X] `docker build -t book-api:local .` succeeds
- [X] `docker run --rm -p 8080:8080 book-api:local` serves `/actuator/health`
- [X] Container reaches Postgres (compose or `host.docker.internal`)
- [X] `docker run -e PORT=10000 -p 8080:10000` still works
- [X] Pushed; Render web service created with Runtime=Docker, Root Directory=`book`
- [X] Health check path set to `/actuator/health`
- [ ] Deployed URL returns real data from Render Postgres — lands with Phase 6 (no data endpoints yet)
- [X] You can explain why the build stage uses a JDK and the run stage a JRE

---

## 11. Integrated into `PROJECT_GUIDE.md` ✅

Done on 2026-09-29, ahead of the original plan (deployment was scheduled after Phase 10):

- `PROJECT_GUIDE.md` §9 now points here and lists only the post-Phase-10 leftovers
  (profiles, Actuator lock-down, Security, `ddl-auto: validate`).
- A new **Phase D** in the guide records this work as done.
- The Mistakes-table row about committed passwords is marked as handled.
- Definition of done includes *"Runs as a container image, configured entirely by
  environment variables."*

From here on this file is reference only — API work (Phases 2–10) needs no Dockerfile or
Render changes. Push, and Render rebuilds.
