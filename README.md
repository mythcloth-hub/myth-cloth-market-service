# Myth Cloth Market Service

Backend service for collecting Myth Cloth market data from supported online stores. It crawls retailer listings, normalizes figurine information, schedules recurring sync jobs, and publishes results to RabbitMQ for downstream processing.

## What it does

- Crawls supported stores on demand or on a schedule
- Normalizes product listings into a common model
- Publishes crawl results to RabbitMQ
- Uses Playwright for sites that need a rendered browser
- Exposes a small REST API for manual syncs

## Project details

- **Framework:** Spring Boot 4.1
- **Language:** Java 25
- **Build tool:** Gradle Wrapper (`./gradlew`)
- **Messaging:** RabbitMQ
- **Scheduling:** Quartz
- **HTML extraction:** Jsoup + Playwright
- **API base path:** `http://localhost:8080/api/v1`
- **Health endpoint:** `http://localhost:8080/api/v1/actuator/health`

This service does not persist data in a database.

## Supported stores

- `NIN_NIN_GAME`
- `MANDARAKE`
- `LUNA_PARK`
- `MY_KOMBINI`
- `MYTH_SUPPLIES`
- `LOGAN_STORE`
- `MYTH_FACTORY`
- `JUNGLE`

## RabbitMQ topology

| Component           | Value              |
|---------------------|--------------------|
| Exchange            | `crawler.exchange` |
| Queue               | `crawler.queue`    |
| Binding routing key | `crawler.#`        |
| Publish routing key | `crawler.job`      |

## Configuration

### `application.yaml`

Always loaded. Sets the servlet context path, actuator exposure, RabbitMQ listener behavior, and Quartz scheduler settings.

### `local` profile

- RabbitMQ host: `localhost`
- RabbitMQ port: `5672`
- Username: `mythcloth`
- Password: `mythcloth`
- Virtual host: `/`
- SSL disabled
- All crawler jobs disabled

### `prod` profile

- RabbitMQ settings come from environment variables
- SSL enabled
- All crawler jobs enabled
- Crawler schedules can be overridden with environment variables

#### Production environment variables

Required:

- `RABBITMQ_HOST`
- `RABBITMQ_PORT`
- `RABBITMQ_USERNAME`
- `RABBITMQ_PASSWORD`
- `RABBITMQ_VIRTUAL_HOST`

Optional cron overrides:

- `NIN_NIN_GAME_CRON`
- `MANDARAKE_CRON`
- `LUNA_PARK_CRON`
- `MY_KOMBINI_CRON`
- `MYTH_SUPPLIES_CRON`
- `LOGAN_STORE_CRON`
- `MYTH_FACTORY_CRON`
- `JUNGLE_CRON`

Optional Playwright settings:

- `PLAYWRIGHT_CHANNEL`
- `PLAYWRIGHT_EXECUTABLE_PATH`

## Local setup

### 1. Install prerequisites

This project targets Java 25 and uses Docker Compose for local RabbitMQ.

### 2. Start RabbitMQ

```sh
docker compose up -d
```

RabbitMQ management UI:

`http://localhost:15672`

Default credentials:

- Username: `mythcloth`
- Password: `mythcloth`

### 3. Run the application

```sh
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 4. Trigger a manual crawl

```sh
curl -X POST http://localhost:8080/api/v1/markets/NIN_NIN_GAME/sync
```

## API

- `POST /api/v1/markets/{storeName}/sync` triggers a manual synchronization for the selected store
- The endpoint returns `202 Accepted` when the request is accepted
- `storeName` must be one of the supported enum values listed above

## Useful commands

```sh
./gradlew test
./gradlew check
./gradlew spotlessCheck
./gradlew spotlessApply
./gradlew clean build
docker compose down
docker compose down -v
```
