#!/bin/bash

# Startup script for AgriChain services

set -e

export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 2>/dev/null || export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 2>/dev/null || true
export PATH=$JAVA_HOME/bin:$PATH

# Color codes for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}AgriChain Platform Startup${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if infrastructure is running
echo -e "${YELLOW}Checking infrastructure...${NC}"
if ! docker ps | grep -q "agrichain-"; then
    echo -e "${YELLOW}Infrastructure not running. Start with: docker compose up -d${NC}"
    read -p "Start infrastructure now? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker compose up -d
        echo -e "${GREEN}Waiting for services to be ready...${NC}"
        sleep 15
    else
        echo "Please start infrastructure manually before running services."
        exit 1
    fi
fi

# Check if JARs are built
if [ ! -f "order-service/target/order-service-1.0.0-SNAPSHOT.jar" ]; then
    echo -e "${YELLOW}Services not built. Building...${NC}"
    mvn clean package -DskipTests -q
fi

# Create logs directory
mkdir -p logs

echo -e "${GREEN}Starting services...${NC}"
echo ""

# Start services in background
echo -e "${GREEN}[1/6] Starting API Gateway (port 8080)...${NC}"
java -jar api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar > logs/api-gateway.log 2>&1 &
echo $! > logs/api-gateway.pid
sleep 3

echo -e "${GREEN}[2/6] Starting Order Service (port 8081)...${NC}"
java -jar order-service/target/order-service-1.0.0-SNAPSHOT.jar > logs/order-service.log 2>&1 &
echo $! > logs/order-service.pid
sleep 3

echo -e "${GREEN}[3/6] Starting Inventory Service (port 8082, gRPC 9090)...${NC}"
java -jar inventory-service/target/inventory-service-1.0.0-SNAPSHOT.jar > logs/inventory-service.log 2>&1 &
echo $! > logs/inventory-service.pid
sleep 3

echo -e "${GREEN}[4/6] Starting Payment Service (port 8083)...${NC}"
java -jar payment-service/target/payment-service-1.0.0-SNAPSHOT.jar > logs/payment-service.log 2>&1 &
echo $! > logs/payment-service.pid
sleep 3

echo -e "${GREEN}[5/6] Starting Orchestrator Service (port 8084)...${NC}"
java -jar orchestrator-service/target/orchestrator-service-1.0.0-SNAPSHOT.jar > logs/orchestrator-service.log 2>&1 &
echo $! > logs/orchestrator-service.pid
sleep 3

echo -e "${GREEN}[6/6] Starting Catalog Service (port 8085)...${NC}"
java -jar catalog-service/target/catalog-service-1.0.0-SNAPSHOT.jar > logs/catalog-service.log 2>&1 &
echo $! > logs/catalog-service.pid
sleep 5

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}All services started!${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo "Service URLs:"
echo "  - API Gateway:        http://localhost:8080/api/health"
echo "  - Order Service:      http://localhost:8081/api/orders"
echo "  - Inventory Service:  http://localhost:8082 (gRPC: 9090)"
echo "  - Payment Service:    http://localhost:8083/api/payments"
echo "  - Orchestrator:       http://localhost:8084"
echo "  - Catalog Service:    http://localhost:8085/api/products"
echo ""
echo "Logs are in the 'logs/' directory"
echo ""
echo "To stop services, run: ./scripts/stop-services.sh"
echo ""

# Test health endpoints
echo -e "${YELLOW}Testing endpoints...${NC}"
sleep 5
for port in 8080 8081 8082 8083 8085; do
    if curl -s http://localhost:$port/actuator/health > /dev/null 2>&1 || curl -s http://localhost:$port/api/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Port $port is responding${NC}"
    else
        echo -e "${YELLOW}⚠ Port $port may still be starting...${NC}"
    fi
done
