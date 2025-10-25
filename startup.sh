#!/bin/bash

# Startup script for Railway deployment
echo "Starting Morago Backend Application..."

# Set default values
export PORT=${PORT:-8080}
export SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-railway,railway-minimal}

echo "Port: $PORT"
echo "Spring Profile: $SPRING_PROFILES_ACTIVE"

# Check if database URL is set
if [ -z "$DATABASE_URL" ] && [ -z "$MYSQL_URL" ]; then
    echo "WARNING: No database URL found. Checking individual MySQL variables..."
    if [ -z "$MYSQLHOST" ] || [ -z "$MYSQLUSER" ] || [ -z "$MYSQLPASSWORD" ]; then
        echo "ERROR: Required MySQL environment variables are missing!"
        echo "Required: MYSQLHOST, MYSQLUSER, MYSQLPASSWORD"
        echo "Optional: MYSQLPORT, MYSQLDATABASE"
        echo "Available environment variables:"
        env | grep -E "(MYSQL|DATABASE|DB_)" || echo "No MySQL-related variables found"
        exit 1
    fi
fi

# Set default JWT secret if not provided
if [ -z "$SECURITY_JWT_SECRET" ]; then
    echo "WARNING: SECURITY_JWT_SECRET not set, using default"
    export SECURITY_JWT_SECRET="w+oiaKX07gE4mbCpViNX1+BHTLCW8mxvOks91kyGHj4="
fi

# Wait for database to be ready (if needed)
echo "Waiting for database connection..."
sleep 15

# Start the application
echo "Starting Java application..."
exec java \
    -Dserver.port=$PORT \
    -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE \
    -Xmx512m \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.output.ansi.enabled=always \
    -jar app.jar
