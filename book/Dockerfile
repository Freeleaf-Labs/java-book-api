### 4.2 the version of docker to be used

# -------------------------- STAGE 1 BUILD -----------------------------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Dependencies and their caching ways
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

# Sources changs on every commit, so it comes last
COPY src/ src/
RUN ./mvnw -B -ntp clean package -DskipTests

# -------------------------- STAGE 2 RUNTIME -----------------------------
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