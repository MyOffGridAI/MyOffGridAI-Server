# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy Maven wrapper and POM first for dependency caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source and build
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S myoffgridai && adduser -S myoffgridai -G myoffgridai

# Copy the built JAR
COPY --from=build /app/target/*.jar app.jar

# Create directories for knowledge storage and logs
RUN mkdir -p /var/myoffgridai/knowledge /var/log/myoffgridai \
    && chown -R myoffgridai:myoffgridai /var/myoffgridai /var/log/myoffgridai /app

# Switch to non-root user
USER myoffgridai

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/system/status || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
