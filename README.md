# Spring Boot Event-Driven Sample

[![Java CI with Gradle](https://github.com/hendisantika/spring-boot-event-driven-sample/actions/workflows/gradle.yml/badge.svg)](https://github.com/hendisantika/spring-boot-event-driven-sample/actions/workflows/gradle.yml)
![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?logo=springboot)
![Gradle](https://img.shields.io/badge/Gradle-9.3.1-02303A?logo=gradle)

A comprehensive event-driven microservice architecture sample using Spring Boot, Apache Kafka, PostgreSQL, and
monitoring tools. This project demonstrates how to build a robust order management system with event sourcing patterns.

## 🚀 Features

- **Event-Driven Architecture**: Complete order lifecycle with Kafka events
- **RESTful APIs**: Full CRUD operations for order management
- **Database Integration**: PostgreSQL with Spring Data JPA
- **Event Processing**: Kafka publishers and consumers
- **Monitoring**: Kafka UI for event tracking
- **Database Management**: pgAdmin for database administration
- **Docker Support**: Complete containerized environment
- **Testing**: Testcontainers-based integration tests (PostgreSQL & Kafka) plus API testing scripts
- **CI/CD**: GitHub Actions build on JDK 25 with Dependabot keeping dependencies fresh

## 🏗️ Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Order API     │    │   Kafka Broker  │    │  PostgreSQL DB  │
│   (REST)        │───▶│   (Events)      │───▶│   (Persistence) │
└─────────────────┘    └─────────────────┘    └─────────────────┘
        │                       │                       │
        ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ Order Service   │    │ Event Consumer  │    │ Order Repository│
│ (Business Logic)│    │ (Processing)    │    │ (Data Access)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 🛠️ Tech Stack

- **Backend**: Spring Boot 4.1.1, Java 25
- **JSON**: Jackson 3 (`tools.jackson`)
- **Database**: PostgreSQL 17
- **Message Broker**: Apache Kafka (Confluent Platform 7.5.0)
- **Build Tool**: Gradle 9.3.1 (wrapper) with Java toolchains
- **Containerization**: Docker & Docker Compose
- **Monitoring**: Kafka UI, pgAdmin
- **Testing**: JUnit 6, Spring Boot Test, Testcontainers 2
- **CI**: GitHub Actions + Dependabot

## 📋 Prerequisites

- Java 25 (Gradle toolchains will use a locally installed JDK 25)
- Docker and Docker Compose (also required to run the Testcontainers-based tests)
- Git
- curl or Postman (for API testing)

## 🚀 Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/hendisantika/spring-boot-event-driven-sample.git
cd spring-boot-event-driven-sample
```

### 2. Start Infrastructure Services

```bash
# Start all services (Kafka, PostgreSQL, Kafka UI, pgAdmin)
docker-compose up -d

# Check if all services are running
docker-compose ps
```

### 3. Run the Application

```bash
# Using Gradle wrapper
./gradlew bootRun

# Or using IDE
# Import the project and run SpringBootEventDrivenSampleApplication.java
```

### 4. Test the Application

```bash
# Make the test script executable
chmod +x test-api.sh

# Run API tests
./test-api.sh
```

## 🌐 Service Endpoints

| Service         | URL                   | Description             |
|-----------------|-----------------------|-------------------------|
| Spring Boot App | http://localhost:8080 | Main application        |
| Kafka UI        | http://localhost:8080 | Kafka monitoring        |
| pgAdmin         | http://localhost:8081 | Database administration |
| PostgreSQL      | localhost:5432        | Database server         |

### pgAdmin Login

- **Email**: admin@example.com
- **Password**: admin

### Database Connection (pgAdmin)

- **Host**: postgres
- **Port**: 5432
- **Database**: orderdb
- **Username**: orderuser
- **Password**: orderpass

## 🔗 API Endpoints

### Order Management

| Method | Endpoint                            | Description                  |
|--------|-------------------------------------|------------------------------|
| POST   | `/api/orders`                       | Create a new order           |
| GET    | `/api/orders`                       | Get all orders               |
| GET    | `/api/orders/{orderNumber}`         | Get order by number          |
| GET    | `/api/orders/customer/{email}`      | Get orders by customer email |
| PUT    | `/api/orders/{orderNumber}/confirm` | Confirm order                |
| PUT    | `/api/orders/{orderNumber}/ship`    | Ship order                   |
| PUT    | `/api/orders/{orderNumber}/deliver` | Mark order as delivered      |
| PUT    | `/api/orders/{orderNumber}/cancel`  | Cancel order                 |

### Sample API Requests

#### Create Order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "John Doe",
    "customerEmail": "john.doe@example.com",
    "productName": "Laptop",
    "quantity": 2,
    "unitPrice": 999.99
  }'
```

#### Get All Orders

```bash
curl http://localhost:8080/api/orders
```

#### Confirm Order

```bash
curl -X PUT http://localhost:8080/api/orders/{orderNumber}/confirm
```

## 📊 Event Flow

The application follows this event-driven flow:

1. **Order Created** → `ORDER_CREATED` event published
2. **Order Confirmed** → `ORDER_CONFIRMED` event published
3. **Order Shipped** → `ORDER_SHIPPED` event published
4. **Order Delivered** → `ORDER_DELIVERED` event published
5. **Order Cancelled** → `ORDER_CANCELLED` event published

Each event is:

- Published to `order-events` Kafka topic
- Consumed by `OrderEventConsumer`
- Processed asynchronously
- Logged for monitoring

## 🗄️ Database Schema

### Orders Table

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    order_number VARCHAR(255) NOT NULL UNIQUE,
    customer_name VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);
```

### Order Status Enum

- `CREATED`
- `CONFIRMED`
- `PROCESSING`
- `SHIPPED`
- `DELIVERED`
- `CANCELLED`

## 🧪 Testing

### Automated Testing

```bash
# Run all tests (requires a running Docker daemon for Testcontainers)
./gradlew test

# Full build (compile + tests + bootJar)
./gradlew build
```

| Test class                                    | What it covers                                                        |
|-----------------------------------------------|-----------------------------------------------------------------------|
| `SpringBootEventDrivenSampleApplicationTests` | Application context loads against PostgreSQL & Kafka containers       |
| `TestContainersValidationTest`                | PostgreSQL Testcontainer wiring                                       |
| `BasicOrderIntegrationTest`                   | Order service & persistence (Kafka publisher mocked)                  |
| `OrderIntegrationTest`                        | Full REST → service → DB → Kafka flow using `TestRestTemplate`        |

### Manual API Testing

Use the provided `test-api.sh` script for comprehensive API testing:

```bash
./test-api.sh
```

The script tests:

- Order creation
- Order retrieval
- Order status transitions
- Customer order lookup
- Order cancellation

## 📁 Project Structure

```
src/
├── main/
│   ├── java/id/my/hendisantika/eventdrivensample/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST controllers
│   │   ├── dto/            # Data transfer objects
│   │   ├── event/          # Event handling classes
│   │   ├── model/          # JPA entities
│   │   ├── repository/     # Data access layer
│   │   └── service/        # Business logic
│   └── resources/
│       └── application.properties
├── test/                   # Test classes
compose.yaml               # Docker services
test-api.sh               # API testing script
build.gradle              # Build configuration
```

## ⚙️ Configuration

### Application Properties

Key configurations in `application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/orderdb
spring.datasource.username=orderuser
spring.datasource.password=orderpass
# Kafka
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=order-processing-group
# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

### Docker Services

- **Zookeeper**: Kafka coordination
- **Kafka**: Message broker
- **Kafka UI**: Event monitoring
- **PostgreSQL**: Database
- **pgAdmin**: Database management

## 🐛 Troubleshooting

### Common Issues

1. **Port Conflicts**
   ```bash
   # Check if ports are in use
   lsof -i :8080  # Spring Boot
   lsof -i :9092  # Kafka
   lsof -i :5432  # PostgreSQL
   ```

2. **Docker Issues**
   ```bash
   # Restart all services
   docker-compose down
   docker-compose up -d
   
   # View logs
   docker-compose logs -f
   ```

3. **Application Won't Start**
   ```bash
   # Check Java version
   java -version
   
   # Clean build
   ./gradlew clean build
   ```

## 📈 Monitoring

### Kafka UI Features

- View topics and partitions
- Monitor consumer groups
- Browse messages
- View broker configurations

### Application Logs

```bash
# Follow application logs
docker-compose logs -f spring-app

# View Kafka logs
docker-compose logs -f kafka
```

## 🚀 Deployment

### Production Considerations

1. **Security**: Use proper authentication and authorization
2. **Monitoring**: Add APM tools (Micrometer, Prometheus)
3. **Scaling**: Configure multiple Kafka partitions
4. **Database**: Use connection pooling
5. **Logging**: Centralized logging system

### Environment Variables

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=orderdb
DB_USER=orderuser
DB_PASS=orderpass

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

## 🔄 Continuous Integration & Dependency Updates

### GitHub Actions

The [`Java CI with Gradle`](.github/workflows/gradle.yml) workflow runs on every push and pull request to `main`:

- **build** – sets up Temurin JDK 25, then runs `./gradlew build` (including Testcontainers integration tests)
- **dependency-submission** – submits the Gradle dependency graph so Dependabot alerts cover all transitive dependencies

### Dependabot

[`.github/dependabot.yml`](.github/dependabot.yml) keeps the project up to date:

| Ecosystem      | Schedule                         | Grouping                                                                 | Commit prefix   |
|----------------|----------------------------------|--------------------------------------------------------------------------|-----------------|
| Gradle         | Daily, 05:00 Asia/Jakarta        | `spring-boot`, `testcontainers`, other minor/patch updates                | `build(deps)`   |
| GitHub Actions | Daily, 05:00 Asia/Jakarta        | All actions in a single PR                                               | `ci(deps)`      |
| Docker Compose | Weekly (Mon), 05:00 Asia/Jakarta | Images in `compose.yaml` (Confluent major upgrades ignored)              | `build(docker)` |

Pull requests are labelled (`dependencies` + ecosystem) and assigned to the maintainer.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Hendi Santika**

- 🔗 Link: [s.id/hendisantika](https://s.id/hendisantika)
- 📧 Email: hendisantika@yahoo.co.id
- 📱 Telegram: [@hendisantika34](https://t.me/hendisantika34)

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- Apache Kafka for reliable messaging
- PostgreSQL for robust database solution
- Docker for containerization support

---

⭐ **Star this repository if you find it helpful!**