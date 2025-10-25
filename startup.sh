#!/bin/bash

# Startup script for Railway deployment
echo "Starting Morago Backend Application..."

# Set default values
export PORT=${PORT:-8080}
export SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-railway}

echo "Port: $PORT"
echo "Spring Profile: $SPRING_PROFILES_ACTIVE"

# Check if database URL is set
if [ -z "$DATABASE_URL" ] && [ -z "$MYSQL_URL" ]; then
    echo "WARNING: No database URL found. Checking individual MySQL variables..."
    if [ -z "$MYSQLHOST" ] || [ -z "$MYSQLUSER" ] || [ -z "$MYSQLPASSWORD" ]; then
        echo "ERROR: Required MySQL environment variables are missing!"
        echo "Required: MYSQLHOST, MYSQLUSER, MYSQLPASSWORD"
        echo "Optional: MYSQLPORT, MYSQLDATABASE"
        exit 1
    fi
fi

# Wait for database to be ready (if needed)
echo "Waiting for database connection..."
sleep 10

# Start the application
echo "Starting Java application..."
exec java \
    -Dserver.port=$PORT \
    -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE \
    -Xmx512m \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -Djava.security.egd=file:/dev/./urandom \
    -jar app.jar
