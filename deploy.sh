#!/bin/bash

# Nexus Deployment Script
# This script builds the application image and starts the container stack.

echo "--- Starting Nexus Deployment ---"

# 1. Clean and build locally for verification (optional but recommended)
# ./mvnw clean package -DskipTests

# 2. Build and start containers
echo "Building and starting Docker containers..."
docker compose up --build -d

echo "--- Deployment Process Initiated ---"
echo "Check logs with: docker compose logs -f nexus-app"
echo "Application will be available at: http://localhost:8080"
