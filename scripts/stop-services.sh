#!/bin/bash

# Stop script for AgriChain services

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}Stopping AgriChain services...${NC}"
echo ""

# Stop services using PID files
for service in api-gateway order-service inventory-service payment-service orchestrator-service catalog-service; do
    if [ -f "logs/${service}.pid" ]; then
        PID=$(cat "logs/${service}.pid")
        if ps -p $PID > /dev/null 2>&1; then
            echo -e "${GREEN}Stopping ${service} (PID: $PID)${NC}"
            kill $PID 2>/dev/null || true
        fi
        rm -f "logs/${service}.pid"
    fi
done

echo ""
echo -e "${GREEN}All services stopped.${NC}"
echo ""
echo "To stop infrastructure: docker compose down"
