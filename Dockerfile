# ---------- Build stage ----------
FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /app

# Copy Gradle wrapper and config first to leverage Docker layer cache
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

RUN chmod +x gradlew && ./gradlew --version

# Warm up the dependency cache
RUN ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true

# Copy sources and build the fat jar (skip tests — Railway runs a fresh build)
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre-jammy AS runtime

WORKDIR /app

# Non-root user for safety
RUN groupadd -r app && useradd -r -g app app

COPY --from=build /app/build/libs/*.jar app.jar

RUN chown -R app:app /app
USER app

# Railway injects PORT dynamically; Spring reads it via server.port=${PORT}
EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
