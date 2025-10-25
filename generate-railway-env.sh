#!/bin/bash

# Railway Environment Variables Setup Script
# Copy the output and paste into Railway Dashboard → Your Project → Variables

echo "===================================================================="
echo "RAILWAY ENVIRONMENT VARIABLES - COPY & PASTE"
echo "===================================================================="
echo "Copy these variables to Railway Dashboard → Your Project → Variables"
echo "===================================================================="
echo ""

echo "# Spring Profile (CRITICAL)"
echo "SPRING_PROFILES_ACTIVE=railway"
echo ""

echo "# Database Configuration (Railway provides these automatically)"
echo "DATABASE_URL=\${{MYSQL_URL}}"
echo ""

echo "# JWT Configuration"
echo "SECURITY_JWT_SECRET=SOh/scquqx1GjtKfCF1KDI2X5eG/tyeuWTED8eCxCGG7xPTKmVPm+3ybiI+vqBsjX4yHDG1MwI3nuhjZWy5jRg=="
echo "JWT_EXPIRATION=86400000"
echo "JWT_EXPIRATION_MS=86400000"
echo "JWT_REFRESH_EXPIRATION_MS=604800000"
echo ""

echo "# Socket.IO Configuration"
echo "SOCKETIO_HOST=0.0.0.0"
echo "SOCKETIO_PORT=9092"
echo "SOCKETIO_ALLOWED_ORIGINS=*"
echo ""

echo "# File Upload Configuration"
echo "MAX_FILE_SIZE=10MB"
echo "MAX_REQUEST_SIZE=10MB"
echo ""

echo "===================================================================="
echo "IMPORTANT: Make sure you have MySQL service added to your Railway project!"
echo "Railway should automatically provide MYSQL_URL, MYSQLHOST, etc."
echo "===================================================================="
