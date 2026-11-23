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

# curl isn't in this base image by default. MoxfieldDeckService shells out
# to it (instead of using Java's own HTTP client) because Moxfield's API
# sits behind Cloudflare, which blocks every plain Java HTTP client's TLS
# fingerprint regardless of headers, but not curl's.
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /build/target/app.jar ./app.jar
RUN mkdir -p /app/data && chown app:app /app/data
USER app

EXPOSE 8080

# Sign-ups and deck reservations persist here as JSON (see liga.data.dir
# in application.properties, default ./data relative to WORKDIR /app).
# Mount a volume at this path so they survive a container recreation —
# without one, this is just as ephemeral as the old in-memory-only
# behavior, since the image itself has no durable storage.
VOLUME ["/app/data"]

# Secrets (GOOGLE_API_KEY, LIGA_ADMIN_TOKEN, etc. — see .env.example) are
# never baked into the image; the host supplies them as real environment
# variables at `docker run` / in its dashboard.
#
# Most free hosts tell the container which port to listen on via $PORT
# (Render, Fly, Heroku-style); others let you fix it yourself via
# $SERVER_PORT (see application.properties) or just use the 8080 default.
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-${SERVER_PORT:-8080}}"]
