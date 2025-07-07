# Wallet Service

A mission-critical wallet service that manages users' money with support for deposits, withdrawals, and transfers between users. Built with Java Spring Boot using hexagonal architecture and Domain-Driven Design principles.

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+
- Docker & Docker Compose

### 1. Clone and Run
```bash
# Clone the repository
git clone <repository-url>
cd wallet-service

# Start all services (app + monitoring stack)
docker-compose up -d

# Create DynamoDB tables (including audit tables)
./scripts/create-tables.sh
```

### 2. Verify Everything is Working
```bash
# Check application health
curl http://localhost:8080/actuator/health

# Run functional tests
./scripts/test-wallet-api.sh

# Run load tests (optional). Params - [wallets] [deposits] [transfers] [withdrawals] [delay]
./scripts/load-test-simple.sh 10 50 25 25 0.1
```

### 3. Access Monitoring
- **Application**: http://localhost:8080
- **Grafana Dashboard**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090
- **DynamoDB Admin**: http://localhost:8001

## 🔒 Audit and Compliance

The service implements a robust audit system to meet regulatory requirements in transactional financial services:

### Immutable Audit Logs
- **Complete Audit Trail**: Each transaction is recorded with details of who/when/where
- **Guaranteed Immutability**: Hash chain implementation to detect any changes in records
- **Complete Context**: Capture of IP, user-agent, timestamp and contextual information

### Reconciliation Snapshots
- **Periodic Snapshots**: Automatic generation of snapshots of all wallets for reconciliation
- **Integrity Verification**: Periodic verification of transaction chain integrity
- **Asynchronous Auditing**: Processing in dedicated thread pool to avoid performance impact

### Configuration
The following environment variables control the audit system behavior:
- `AUDIT_SNAPSHOT_ENABLED`: Enable/disable snapshots (default: true)
- `AUDIT_SNAPSHOT_BATCH_SIZE`: Batch size for processing (default: 100)
- `AUDIT_SNAPSHOT_CRON`: Cron expression for snapshots (default: daily at midnight)
- `AUDIT_INTEGRITY_CRON`: Cron expression for integrity checks (default: daily at 1 AM)

## 📋 API Documentation

The service provides a complete OpenAPI specification available at:
- **OpenAPI Spec**: [openapi.yaml](./openapi.yaml)

### Key Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/wallets` | Create a new wallet |
| GET | `/api/v1/wallets/{id}` | Get wallet details |
| POST | `/api/v1/wallets/{id}/deposit` | Deposit money |
| POST | `/api/v1/wallets/{id}/withdraw` | Withdraw money |
| POST | `/api/v1/wallets/transfer` | Transfer between wallets |
| GET | `/api/v1/wallets/{id}/balance/historical` | Get historical balance |

## 🏗️ Architecture

This project implements **Hexagonal Architecture** (Ports & Adapters) with **Domain-Driven Design** principles.

### Project Structure
```
src/main/java/com/wallet/
├── WalletServiceApplication.java
├── domain/                          # 🔵 Domain Layer (Core Business Logic)
│   ├── model/                       # Entities, Value Objects, Aggregates
│   │   ├── AuditInfo.java
│   │   ├── Money.java
│   │   ├── Transaction.java
│   │   ├── Wallet.java
│   │   └── WalletId.java
│   ├── repositories/                # Domain Repositories (Interfaces)
│   │   ├── TransactionRepository.java
│   │   └── WalletRepository.java
│   └── service/                     # Domain Services
│       ├── AuditService.java
│       ├── ImmutableAuditService.java
│       ├── PeriodicSnapshotService.java
│       └── WalletDomainService.java
├── application/                     # 🟡 Application Layer (Use Cases)
│   ├── dto/                        # Data Transfer Objects
│   │   ├── AmountRequest.java
│   │   ├── CreateWalletRequest.java
│   │   ├── TransactionRequest.java
│   │   ├── TransferRequest.java
│   │   └── WalletResponse.java
│   └── usecase/                    # Application Services
│       └── WalletUseCase.java
└── infrastructure/                  # 🟢 Infrastructure Layer (Adapters)
    ├── adapter/
    │   ├── web/                    # REST Controllers (Primary Adapters)
    │   │   ├── GlobalExceptionHandler.java
    │   │   └── WalletController.java
    │   └── persistence/            # Database Adapters (Secondary Adapters)
    │       ├── AuditLogEntity.java
    │       ├── TransactionEntity.java
    │       ├── WalletEntity.java
    │       ├── WalletSnapshotEntity.java
    │       └── repositories/
    │           ├── DynamoDbTransactionRepository.java
    │           └── DynamoDbWalletRepository.java
    ├── config/                     # Configuration
    │   ├── AuditConfig.java
    │   ├── DynamoDbConfig.java
    │   ├── LoggingInterceptor.java
    │   └── WebConfig.java
    └── metrics/                    # Observability
        └── WalletMetrics.java
```

### Key Design Decisions
- **DDD Value Objects**: `Money` and `WalletId` ensure type safety and encapsulate business rules
- **Aggregate Root**: `Wallet` maintains consistency and enforces business invariants
- **Event Sourcing Pattern**: Transaction history enables full audit trail and historical balance queries
- **DynamoDB Transactions**: Ensures ACID properties for critical operations like transfers
- **Immutable Value Objects**: Prevents accidental state mutations
- **Immutable Audit Trail**: Hash chain implementation ensures tamper-evident audit logs
- **Periodic Snapshots**: Automated reconciliation support for regulatory compliance

For detailed architecture decisions, see [DESIGN.md](./DESIGN.md).

## 📊 Monitoring & Observability

The service includes comprehensive monitoring with structured logging, metrics, and dashboards.

### Grafana Dashboard
Access the pre-configured dashboard at http://localhost:3000 (admin/admin) with:

#### Key Metrics Panels:
- **Service Availability**: Overall system health percentage
- **Success Rate Gauges**: Deposit, Withdrawal, and Transfer success rates
- **Transaction Rates**: Operations per second
- **Transaction Totals**: Cumulative counters
- **Latency Percentiles**: P50, P90, P99 for all operations
- **Error Rates**: Business and database error tracking

#### Metrics Categories:
- **Business Metrics**: Transaction counts, success rates, availability
- **Performance Metrics**: Latency percentiles, throughput rates
- **Infrastructure Metrics**: Database operation timings, error rates
- **Reliability Metrics**: Success/failure ratios, system availability
- **Audit Metrics**: Audit log counts, snapshot counts, integrity verification

### Prometheus Metrics
Raw metrics available at http://localhost:9090 include:
- `wallet_created_total` - Total wallets created
- `wallet_deposits_total` - Total deposits processed
- `wallet_withdrawals_total` - Total withdrawals processed
- `wallet_transfers_total` - Total transfers processed
- `wallet_*_duration_seconds` - Operation latency histograms
- `wallet_business_errors_total` - Business logic errors
- `wallet_database_errors_total` - Database operation errors
- `wallet_audit_logs_total` - Total audit logs created
- `wallet_snapshots_total` - Total wallet snapshots created
- `wallet_audit_errors_total` - Audit-related errors

### Structured Logging
All logs are in JSON format with:
- **Request correlation IDs** for tracing
- **Contextual information** (walletId, userId, amounts)
- **Performance metrics** (duration, status codes)
- **Error details** with stack traces

## 🧪 Testing

The project maintains **>80% test coverage** with comprehensive unit and integration tests.

### Run Tests
```bash
# Unit tests
mvn test

# Test coverage report
mvn jacoco:report
# View: target/site/jacoco/index.html

# tests with code coverage target verification
mvn verify
```

### Test Scripts
```bash
# Functional API tests
./scripts/test-wallet-api.sh

# Load testing (configurable)
./scripts/load-test-simple.sh [wallets] [deposits] [transfers] [withdrawals] [delay]

# Examples:
./scripts/load-test-simple.sh 5 20 10 10 0.1    # Light load
./scripts/load-test-simple.sh 50 200 100 100 0.05  # Heavy load
```

## 🔧 Configuration

### Environment Variables
- `DYNAMODB_ENDPOINT`: DynamoDB endpoint (empty for AWS)
- `AWS_REGION`: AWS region (default: us-east-1)
- `AWS_ACCESS_KEY_ID`: AWS access key
- `AWS_SECRET_ACCESS_KEY`: AWS secret key
- `AUDIT_SNAPSHOT_ENABLED`: Enable/disable wallet snapshots (default: true)
- `AUDIT_SNAPSHOT_BATCH_SIZE`: Batch size for snapshot processing (default: 100)
- `AUDIT_SNAPSHOT_CRON`: Cron expression for snapshots (default: daily at midnight)
- `AUDIT_INTEGRITY_CRON`: Cron expression for integrity checks (default: daily at 1 AM)

## 📄 License

This project is licensed under the MIT License.