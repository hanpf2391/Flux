#!/bin/bash

# Flux Application Deployment Script

set -e

echo "🚀 Starting Flux application deployment..."

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

# Copy environment file if it doesn't exist
if [ ! -f .env ]; then
    echo "📋 Creating .env file from template..."
    cp .env.example .env
    echo "⚠️  Please edit .env file with your configuration before running deployment"
    exit 1
fi

# Create SSL directory if it doesn't exist
if [ ! -d ssl ]; then
    echo "📁 Creating SSL directory..."
    mkdir -p ssl
    echo "⚠️  Please add your SSL certificates to the ssl/ directory"
    echo "   - ssl/cert.pem (certificate file)"
    echo "   - ssl/key.pem (private key file)"
fi

# Create logs directory
echo "📁 Creating logs directory..."
mkdir -p flux_back/logs

# Build and start services
echo "🏗️  Building and starting services..."
docker-compose down --remove-orphans
docker-compose build --no-cache
docker-compose up -d

echo "⏳ Waiting for services to start..."
sleep 30

# Check service health
echo "🔍 Checking service health..."
docker-compose ps

# Show logs
echo "📋 Showing recent logs..."
docker-compose logs --tail=50

echo "✅ Deployment completed!"
echo ""
echo "🌐 Application URLs:"
echo "   - Frontend: https://localhost"
echo "   - Backend API: https://localhost/api"
echo "   - WebSocket: wss://localhost/ws"
echo ""
echo "📊 Health endpoints:"
echo "   - Nginx: https://localhost/health"
echo "   - Backend: http://localhost:8080/actuator/health"
echo ""
echo "📝 Useful commands:"
echo "   - View logs: docker-compose logs -f [service]"
echo "   - Stop services: docker-compose down"
echo "   - Restart services: docker-compose restart"
echo "   - Update and restart: docker-compose pull && docker-compose up -d"