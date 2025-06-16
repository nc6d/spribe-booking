# Booking System

A Spring Boot-based booking system that allows users to manage accommodation units, make bookings, and process payments.

## Features

- Unit management (create, search, book)
- Booking management with automatic cancellation after 15 minutes without payment
- Caching system for available units statistics
- RESTful API with OpenAPI documentation
- PostgreSQL database with Liquibase migrations
- Comprehensive test coverage

## Prerequisites

- Java 21 or later
- Docker and Docker Compose
- Gradle 8.x or later

## Quick Start

1. Clone the repository:
```bash
git clone <repository-url>
cd booking
```

2. Start the required services using Docker Compose:
```bash
docker compose up -d
```

3. Build and run the application:
```bash
./gradlew build
./gradlew bootRun
```

The application will be available at `http://localhost:8080`

## API Documentation

Once the application is running, you can access the OpenAPI documentation at `http://localhost:8080/swagger-ui.html`

## Database

The application uses PostgreSQL as its database. The database schema is managed by Liquibase migrations located in `src/main/resources/db/changelog/`.

Initial data includes:
- 10 predefined units with specific properties
- 90 randomly generated units with varying parameters

## Testing

Run the test suite:
```bash
./gradlew test
```

The project includes:
- Unit tests for models, controllers, and services
- Integration tests using Testcontainers
- Functional tests for API endpoints

## Project Structure

```
src/
├── main/
│   ├── java/org/spribe/
│   │   ├── controller/    # REST controllers
│   │   ├── service/      # Business logic
│   │   ├── repository/   # Data access
│   │   ├── model/        # Domain models
│   │   ├── dto/          # Data transfer objects
│   │   └── config/       # Configuration classes
│   └── resources/
│       └── db/
│           └── changelog/ # Liquibase migrations
└── test/                 # Test classes
```

## Caching

The application implements a caching system for storing the number of available units. The cache is:
- Updated with each unit status change
- Recoverable after system crashes
- Accessible via a dedicated endpoint

## Docker Services

The following services are configured in `docker-compose.yml`:
- PostgreSQL database
- Redis cache (if configured)

## Building

Create an executable JAR:
```bash
./gradlew bootJar
```

The JAR file will be created in `build/libs/`. 