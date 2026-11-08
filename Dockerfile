# --- Build stage: compiles the fat jar, nothing from here ships in the
#     final image. ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Dependencies first, in their own layer, so `docker build` only re-downloads
# them when pom.xml actually changes, not on every source edit.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# --- Runtime stage: just a JRE and the jar. ---
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /build/target/app.jar ./app.jar
USER app

EXPOSE 8080

# Secrets (GOOGLE_API_KEY, LIGA_ADMIN_TOKEN, etc. — see .env.example) are
# never baked into the image; the host supplies them as real environment
# variables at `docker run` / in its dashboard.
#
# Most free hosts tell the container which port to listen on via $PORT
# (Render, Fly, Heroku-style); others let you fix it yourself via
# $SERVER_PORT (see application.properties) or just use the 8080 default.
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-${SERVER_PORT:-8080}}"]
