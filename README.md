# Offer Service

A Java 21 and Spring Boot application that manages a sports betting offer using in-memory storage and Kafka message consumers.

The application loads initial events and markets from JSON files, processes updates from Kafka, and exposes the current offer through REST API endpoints.

## Technology Stack

- Java 21
- Spring Boot 3.5.7
- Maven
- Apache Kafka 3.9.0
- Docker Compose
- Spring Kafka
- Jackson
- Springdoc OpenAPI / Swagger UI
- JUnit 5
- Spring Boot Test

## Architecture

The application is organized into the following layers:

- **Controller** – exposes REST API endpoints.
- **Service** – implements offer retrieval, filtering, merging, and cleanup logic.
- **Repository** – stores events and markets in concurrent in-memory maps.
- **Kafka Consumers** – process market and event messages.
- **Data Loader** – loads initial data from JSON files during application startup.
- **Scheduler** – periodically removes events that have already started.
- **Configuration** – configures Kafka topics, consumers, producers, and error handling.

### Data Storage

The application uses an in-memory repository based on `ConcurrentHashMap`.

Initial data is loaded from:

- `src/main/resources/data/events.json`
- `src/main/resources/data/markets.json`

No external database is required.

Data received through Kafka is stored in memory and is not persisted across application restarts. Only Kafka broker data is persisted through the configured Docker volume.

## Getting Started

### Prerequisites

Make sure the following tools are installed:

- Java 21
- Docker Desktop with Docker Compose
- Git

### 1. Start Kafka

From the project root directory, run:

```bash
docker compose up -d
```

This starts:

- Apache Kafka in KRaft mode
- Redpanda Console for Kafka monitoring

The Kafka broker is available at:

```text
localhost:9092
```

Redpanda Console:

```text
http://localhost:8080
```

### 2. Run the Application

From the project root directory:

```powershell
.\mvnw.cmd spring-boot:run
```

The application starts on port `8081`.

### 3. Stop the Services

To stop the Docker services:

```bash
docker compose down
```

To remove the Kafka data volume as well:

```bash
docker compose down -v
```

**Warning:** Removing the volume deletes the persisted Kafka broker data.

## REST API

Base URL:

```text
http://localhost:8081
```

### Get the Complete Offer

```http
GET /api/v1/offer
```

Returns the current offer, including eligible events, their active markets, and active outcomes.

Only events with status `1` and a future start time are included.

Markets and outcomes are included only when both the event-specific entry and the corresponding market or outcome are active.

### Get Upcoming Events

```http
GET /api/v1/offer/events
```

Returns upcoming events.

Optional query parameter:

| Parameter | Description | Format |
|---|---|---|
| `startDate` | Filters events by their calendar start date | `YYYY-MM-DD` |

Example:

```http
GET /api/v1/offer/events?startDate=2032-05-01
```

An invalid date format results in a `400 Bad Request` response.

### Get All Markets

```http
GET /api/v1/offer/markets
```

Returns all markets stored in the repository.

## Kafka Integration

The application consumes messages from two Kafka topics.

### Topics

| Topic | Partitions | Purpose |
|---|---:|---|
| `market` | 1 | Market updates |
| `event` | 2 | Event updates |

Both topics use one replica in the local Docker setup.

Kafka consumers use the consumer group:

```text
offer-service-group
```

The application connects to Kafka through:

```text
localhost:9092
```

### Market Consumer

The `MarketConsumer` listens to the `market` topic.

It receives a complete Market JSON message, deserializes it into a `Market` object, and updates the in-memory repository.

### Event Consumer

The `EventConsumer` listens to the `event` topic.

It processes event messages as JSON and supports both complete and partial event updates.

The service merges received fields into the existing event rather than replacing the entire event.

Supported event fields include:

- `id`
- `name`
- `startsAt`
- `status`
- `markets`

Market updates can include:

- `id`
- `marketId`
- `status`
- `outcomes`

Outcome data can include:

- `id`
- `outcomeId`
- `status`
- `odds`

When an event does not exist, a new event is created using the received ID.

Market updates are matched by their IDs. Existing markets not included in the update are retained.

When an update contains an `outcomes` array, the existing outcomes for that market are replaced by the received array. Individual outcomes are not merged.

### Kafka Error Handling

The application uses Spring Kafka's `DefaultErrorHandler` and `DeadLetterPublishingRecoverer` to handle message processing failures.

**Retry policy:**

- Failed messages are retried up to 3 times after the initial attempt.
- The delay between retry attempts is 1 second.
- After all attempts fail, the message is published to the corresponding Dead Letter Topic (DLT).

**Dead Letter Topics:**

| Source Topic | DLT | Partitions |
|---|---|---:|
| `market` | `market.DLT` | 1 |
| `event` | `event.DLT` | 2 |

Messages are routed to the corresponding DLT using the original partition number.

The following exceptions are considered non-retryable and are sent directly to the DLT:

- `JsonProcessingException`
- `IllegalArgumentException`

This prevents malformed JSON messages and invalid event updates from being retried unnecessarily.

## Automatic Event Cleanup

The application automatically removes events that have already started.

The cleanup process:

- Runs automatically every 60 seconds.
- Uses UTC time for comparison.
- Removes events whose `startsAt` is earlier than or equal to the current time.
- Retains events without a `startsAt` value.

The cleanup is implemented using Spring's `@Scheduled` mechanism.

## API Documentation

Swagger UI:

```text
http://localhost:8081/swagger-ui/index.html
```

OpenAPI specification:

```text
http://localhost:8081/v3/api-docs
```

## Testing

The project includes unit and integration tests covering:

- Application context startup
- Repository operations
- Offer service business logic
- Global exception handling
- Kafka market consumer
- Kafka event consumer
- Event cleanup scheduler
- Scheduled cleanup execution

Run the complete test suite:

```powershell
.\mvnw.cmd clean verify
```

The Maven build runs the tests and packages the application.

The Kafka retry and Dead Letter Topic behavior has also been manually verified using a running Kafka broker.

## Project Structure

```text
src
├── main
│   ├── java/com/sportradar/offer_service
│   │   ├── config
│   │   ├── controller
│   │   ├── dto
│   │   ├── exception
│   │   ├── kafka
│   │   ├── loader
│   │   ├── model
│   │   ├── repository
│   │   ├── scheduler
│   │   ├── service
│   │   └── OfferServiceApplication.java
│   └── resources
│       ├── data
│       │   ├── events.json
│       │   └── markets.json
│       └── application.properties
└── test
    └── java/com/sportradar/offer_service
        ├── exception
        ├── kafka
        ├── repository
        ├── scheduler
        ├── service
        └── OfferServiceApplicationTests.java
```