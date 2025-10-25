#!/bin/bash

# Startup script for Railway deployment
echo "Starting Morago Backend Application..."

# Set default values
export PORT=${PORT:-8080}
export SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-railway,railway-minimal}

echo "Port: $PORT"
echo "Spring Profile: $SPRING_PROFILES_ACTIVE"

# Check database configuration
echo "Database configuration check:"
if [ -n "$MYSQL_URL" ]; then
    echo "✓ MYSQL_URL is set (Railway preferred)"
    echo "  URL: ${MYSQL_URL:0:50}..." # Show first 50 chars for debugging
elif [ -n "$DATABASE_URL" ]; then
    echo "✓ DATABASE_URL is set"
    echo "  URL: ${DATABASE_URL:0:50}..." # Show first 50 chars for debugging
elif [ -n "$DB_USERNAME" ] && [ -n "$DB_PASSWORD" ]; then
    echo "✓ DB_USERNAME and DB_PASSWORD are set"
    echo "  Username: $DB_USERNAME"
elif [ -n "$MYSQLHOST" ] && [ -n "$MYSQLUSER" ] && [ -n "$MYSQLPASSWORD" ]; then
    echo "✓ MYSQLHOST, MYSQLUSER, and MYSQLPASSWORD are set"
    echo "  Host: $MYSQLHOST"
    echo "  Username: $MYSQLUSER"
else
    echo "WARNING: No database configuration found. Spring Boot will use defaults."
    echo "Available database-related environment variables:"
    env | grep -E "(MYSQL|DATABASE|DB_)" || echo "No database-related variables found"
fi

# Set default JWT secret if not provided
if [ -z "$SECURITY_JWT_SECRET" ]; then
    echo "WARNING: SECURITY_JWT_SECRET not set, using default"
    export SECURITY_JWT_SECRET="w+oiaKX07gE4mbCpViNX1+BHTLCW8mxvOks91kyGHj4="
fi

# Wait for database to be ready (if needed)
echo "Waiting for database connection..."
sleep 5

# Start the application
echo "Starting Java application..."
echo "JAR file location: $(ls -la app.jar 2>/dev/null || echo 'app.jar not found!')"
echo "Working directory: $(pwd)"
echo "Java version: $(java -version 2>&1 | head -1)"

exec java \
    -Dserver.port=$PORT \
    -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE \
    -Xmx512m \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.output.ansi.enabled=always \
    -jar app.jar
