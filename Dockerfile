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

# Set working directory
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/morago-backend-0.0.1-SNAPSHOT.jar app.jar

# Expose default port (Railway overrides $PORT)
EXPOSE 8080

# Set Spring profile to railway for Railway deployment
ENV SPRING_PROFILES_ACTIVE=railway

# Run Spring Boot jar
ENTRYPOINT ["sh", "-c", "java -Dserver.port=$PORT -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -jar app.jar"]
