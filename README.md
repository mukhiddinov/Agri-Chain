# AgriChain Digital Procurement Platform

A blockchain-enabled procurement platform for agricultural supply chain management, built with microservices architecture, gRPC, Kafka, and Ethereum smart contracts.

## Architecture Overview

### Monorepo Structure

```
agrichain-platform/
├── common-proto/              # Shared Protobuf/gRPC contracts
├── api-gateway/               # API Gateway with WebFlux & GraphQL
├── order-service/             # Order management REST API
├── inventory-service/         # Inventory management gRPC service
├── payment-service/           # Blockchain payment integration
├── catalog-service/           # Product catalog service
├── orchestrator-service/      # Saga orchestration service
├── blockchain/contracts/      # Foundry smart contracts
└── docker-compose.yml         # Local infrastructure
```

### Services

- **Order Service** (Port 8081): REST API for order creation, publishes `order.created` events to Kafka
- **Inventory Service** (Port 8082, gRPC 9090): gRPC server for inventory reservation/release
- **Payment Service** (Port 8083): Web3j integration with Escrow smart contract on Sepolia
- **Catalog Service** (Port 8085): Product catalog with MongoDB
- **Orchestrator Service** (Port 8084): Saga coordinator listening to Kafka events
- **API Gateway** (Port 8080): WebFlux-based gateway (placeholder for GraphQL)

### Technology Stack

- **Backend**: Spring Boot 3.2.1, Java 21
- **Communication**: gRPC (inventory), REST (orders, catalog, payment), Kafka (events)
- **Databases**: PostgreSQL (orders, inventory), MongoDB (catalog), Redis (cache)
- **Blockchain**: Ethereum Sepolia testnet, Web3j, Foundry
- **Message Broker**: Redpanda (Kafka-compatible)

## Quick Start

### Prerequisites

- Java 21
- Maven 3.8+
- Docker & Docker Compose
- Foundry (for smart contracts)

### 1. Start Infrastructure

```bash
# Start all infrastructure services
docker-compose up -d

# Verify services are running
docker-compose ps

# Check logs if needed
docker-compose logs -f
```

Infrastructure includes:
- Redpanda (Kafka): localhost:19092
- PostgreSQL: localhost:5432
- MongoDB: localhost:27017
- Redis: localhost:6379

### 2. Build Services

```bash
# Build all services (skip tests for quick start)
mvn clean package -DskipTests

# Or build with tests
mvn clean package
```

### 3. Run Services

Each service can be started independently:

```bash
# Order Service
cd order-service
mvn spring-boot:run

# Inventory Service
cd inventory-service
mvn spring-boot:run

# Payment Service
cd payment-service
mvn spring-boot:run

# Catalog Service
cd catalog-service
mvn spring-boot:run

# Orchestrator Service
cd orchestrator-service
mvn spring-boot:run

# API Gateway
cd api-gateway
mvn spring-boot:run
```

Or start services as JAR files:

```bash
# From root directory
java -jar order-service/target/order-service-1.0.0-SNAPSHOT.jar &
java -jar inventory-service/target/inventory-service-1.0.0-SNAPSHOT.jar &
java -jar payment-service/target/payment-service-1.0.0-SNAPSHOT.jar &
java -jar catalog-service/target/catalog-service-1.0.0-SNAPSHOT.jar &
java -jar orchestrator-service/target/orchestrator-service-1.0.0-SNAPSHOT.jar &
java -jar api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar &
```

## Environment Variables

### Payment Service (Blockchain Integration)

```bash
# Sepolia RPC URL (get from Infura, Alchemy, or other provider)
export SEPOLIA_RPC_URL=https://sepolia.infura.io/v3/YOUR_PROJECT_ID

# Deployed Escrow contract address (after deployment)
export ESCROW_CONTRACT_ADDRESS=0xYourContractAddressHere
```

### Service Configuration

```bash
# Kafka Bootstrap Servers
export KAFKA_BOOTSTRAP_SERVERS=localhost:19092

# PostgreSQL
export POSTGRES_HOST=localhost
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=postgres

# MongoDB
export MONGO_URI=mongodb://localhost:27017/catalog
```

## Blockchain Smart Contracts

### Setup Foundry

```bash
cd blockchain/contracts

# Install Foundry dependencies
forge install foundry-rs/forge-std

# Build contracts
forge build

# Run tests
forge test

# Run tests with verbosity
forge test -vvv
```

### Deploy to Sepolia

```bash
# Set environment variables
export SEPOLIA_RPC_URL=https://sepolia.infura.io/v3/YOUR_PROJECT_ID
export PRIVATE_KEY=your_private_key_here

# Deploy Escrow contract
forge create --rpc-url $SEPOLIA_RPC_URL \
  --private-key $PRIVATE_KEY \
  src/Escrow.sol:Escrow

# Note the deployed contract address and set it in payment-service
export ESCROW_CONTRACT_ADDRESS=<deployed_address>
```

## API Examples

### Create Order

```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-123",
    "items": [
      {
        "productId": "prod-001",
        "productName": "Organic Wheat",
        "quantity": 100,
        "price": 25.50
      }
    ]
  }'
```

### Get Order

```bash
curl http://localhost:8081/api/orders/{orderId}
```

### Create Product (Catalog)

```bash
curl -X POST http://localhost:8085/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Organic Wheat",
    "description": "Premium organic wheat from local farms",
    "category": "grains",
    "price": 25.50,
    "unit": "kg",
    "stockQuantity": 1000
  }'
```

### Get All Products

```bash
curl http://localhost:8085/api/products
```

### Process Payment

```bash
curl -X POST http://localhost:8083/api/payments/process \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "order-123",
    "amount": 2550.00
  }'
```

### Gateway Health Check

```bash
curl http://localhost:8080/api/health
```

## Event Flow

1. **Order Creation**: POST to order-service → `order.created` event published to Kafka
2. **Saga Orchestration**: Orchestrator listens to `order.created` → calls inventory-service via gRPC
3. **Inventory Reservation**: Inventory service reserves items (stub implementation)
4. **Payment Processing**: Payment service interacts with Escrow smart contract (stub)
5. **Order Completion**: Status updates propagated through the system

## Development Notes

### Current Implementation Status

✅ **Implemented**:
- Monorepo Maven structure with all modules
- Protobuf contracts for Order and Inventory services
- Order Service: REST controller with Kafka publisher
- Inventory Service: gRPC server with reserve/release endpoints
- Payment Service: Web3j client setup with Sepolia RPC configuration
- Catalog Service: MongoDB-backed product catalog
- Orchestrator Service: Kafka listener for saga coordination
- API Gateway: Basic WebFlux placeholder
- Escrow Smart Contract: Complete with tests
- Docker Compose: Redpanda, PostgreSQL, MongoDB, Redis

🚧 **Stubbed (Ready for Implementation)**:
- Order persistence (JPA entities stubbed)
- Inventory database operations
- Actual payment contract interaction
- Complete saga compensation logic
- GraphQL schema in API Gateway

### Testing

```bash
# Test Java services
mvn test

# Test smart contracts
cd blockchain/contracts
forge test

# Integration testing
# (Start infrastructure with docker-compose, then run services)
```

### Stopping Services

```bash
# Stop infrastructure
docker-compose down

# Stop with volume cleanup
docker-compose down -v

# Stop Java services (if running in background)
pkill -f "spring-boot"
```

## Project Structure Details

### Common Proto Module

Contains shared Protobuf definitions:
- `inventory_service.proto`: ReserveInventory, ReleaseInventory, CheckAvailability
- `order_service.proto`: Order model and internal gRPC operations

Generated code is used by inventory-service (server) and orchestrator-service (client).

### Kafka Topics

- `order.created`: Published by order-service when new orders are created
- Additional topics can be added for order status updates, inventory changes, etc.

### Database Schemas

**PostgreSQL (Orders)**:
- Database: `orders`
- Tables: (to be implemented based on JPA entities)

**PostgreSQL (Inventory)**:
- Database: `inventory`
- Tables: (to be implemented)

**MongoDB (Catalog)**:
- Database: `catalog`
- Collection: `products`

## Contributing

1. Build and test locally before committing
2. Ensure all services start without errors
3. Run `mvn clean verify` to validate build
4. Test Foundry contracts with `forge test`

## License

MIT License

## Support

For issues and questions, please open an issue on GitHub.