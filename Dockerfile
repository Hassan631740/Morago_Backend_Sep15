# ===== Stage 1: Build =====
FROM eclipse-temurin:21-jdk-alpine AS build

# Install Maven
RUN apk add --no-cache maven bash

# Set working directory
WORKDIR /app

# Set Maven options for better performance and reliability
ENV MAVEN_OPTS="-Xmx1024m"

# Copy Maven configuration and pom.xml first for dependency caching
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies with retry logic
RUN mvn dependency:resolve -B --fail-at-end || \
    (sleep 10 && mvn dependency:resolve -B --fail-at-end) || \
    (sleep 20 && mvn dependency:resolve -B --fail-at-end -U)

# Copy source code
COPY src ./src

# Build the jar
RUN mvn clean package -DskipTests -B --fail-at-end

# ===== Stage 2: Runtime =====
FROM eclipse-temurin:21-jdk-alpine

# Install curl for health checks
RUN apk add --no-cache curl

# Set working directory
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/morago-backend-0.0.1-SNAPSHOT.jar app.jar

# Copy startup script
COPY startup.sh startup.sh
RUN chmod +x startup.sh

# Expose default port (Railway overrides $PORT)
EXPOSE 8080

# Set Spring profile to railway for Railway deployment
ENV SPRING_PROFILES_ACTIVE=railway

# Add health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${PORT:-8080}/actuator/health || exit 1

# Run Spring Boot jar with better JVM options
ENTRYPOINT ["./startup.sh"]
