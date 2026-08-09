# Myth Cloth Market Service

Backend service for collecting Myth Cloth market data from supported online stores.  
It crawls retailer listings, normalizes figurine information, schedules recurring sync jobs, and publishes results to RabbitMQ for downstream processing.

## Project overview

- **Framework**: Spring Boot 4.1 (Java 25)
- **Messaging**: RabbitMQ
- **Scheduling**: Quartz
- **HTML extraction**: Jsoup + Playwright
- **Build tool**: Gradle Wrapper (`./gradlew`)
- **API base URL**: `http://localhost:8080/api/v1`
- **Health endpoint**: `http://localhost:8080/api/v1/actuator/health`
- **RabbitMQ Management UI**: `http://localhost:15672`

This service does not persist data in a database. Its main responsibility is to crawl store listings and publish each normalized listing to RabbitMQ.

## Supported stores

The API and scheduler currently support these store keys:

- `NIN_NIN_GAME`
- `MANDARAKE`
- `LUNA_PARK`
- `MY_KOMBINI`
- `MYTH_SUPPLIES`
- `LOGAN_STORE`
- `MYTH_FACTORY`

## Messaging topology

Listings are published through RabbitMQ using this topology:

| Component           | Value              |
|---------------------|--------------------|
| Exchange            | `crawler.exchange` |
| Queue               | `crawler.queue`    |
| Binding routing key | `crawler.#`        |
| Publish routing key | `crawler.job`      |

## Environment variables

The service reads RabbitMQ and crawler schedule configuration from Spring profiles and environment variables.

### Required in production

| Variable                | Purpose               |
|-------------------------|-----------------------|
| `RABBITMQ_HOST`         | RabbitMQ host         |
| `RABBITMQ_PORT`         | RabbitMQ port         |
| `RABBITMQ_USERNAME`     | RabbitMQ username     |
| `RABBITMQ_PASSWORD`     | RabbitMQ password     |
| `RABBITMQ_VIRTUAL_HOST` | RabbitMQ virtual host |

### Optional in production

These variables override the default Quartz schedules used by the `prod` profile:

| Variable             | Purpose                            | Default        |
|----------------------|------------------------------------|----------------|
| `NIN_NIN_GAME_CRON`  | Schedule for Nin-Nin-Game crawler  | `0 0 7 * * ?`  |
| `MANDARAKE_CRON`     | Schedule for Mandarake crawler     | `0 0 8 * * ?`  |
| `LUNA_PARK_CRON`     | Schedule for Luna Park crawler     | `0 0 9 * * ?`  |
| `MY_KOMBINI_CRON`    | Schedule for My Kombini crawler    | `0 0 10 * * ?` |
| `MYTH_SUPPLIES_CRON` | Schedule for Myth Supplies crawler | `0 0 11 * * ?` |
| `LOGAN_STORE_CRON`   | Schedule for Logan Store crawler   | `0 0 12 * * ?` |
| `MYTH_FACTORY_CRON`  | Schedule for Myth Factory crawler  | `0 0 13 * * ?` |

### Local profile defaults

When running with `local`, RabbitMQ already defaults to:

- `RABBITMQ_HOST=localhost`
- `RABBITMQ_PORT=5672`
- `RABBITMQ_USERNAME=mythcloth`
- `RABBITMQ_PASSWORD=mythcloth`
- `RABBITMQ_VIRTUAL_HOST=/`
- RabbitMQ SSL disabled
- All crawler jobs disabled by default

### Profiles used by this project

- `application.yaml`: always loaded; defines `/api/v1`, actuator exposure, RabbitMQ listener behavior, and Quartz scheduler settings.
- `local`: uses local RabbitMQ defaults, disables SSL, and keeps all crawler jobs disabled.
- `prod`: reads RabbitMQ settings from environment variables, enables SSL, and enables all crawler jobs.

---

## Local setup (new machine) - step by step

### 1. Install prerequisites

This project targets **Java 25** and uses Docker Compose for local RabbitMQ.

On an Ubuntu-based distro:

```sh
sudo apt update
sudo apt install -y ca-certificates curl gnupg git openjdk-25-jdk docker.io docker-compose-v2
```

Check versions:

```sh
git --version
java -version
docker --version
docker compose version
```

### 2. Clone the repository

```sh
git clone https://github.com/mythcloth-hub/myth-cloth-market-service.git
cd myth-cloth-market-service
```

### 3. Start RabbitMQ with Docker Compose

This project already includes `compose.yml` with:

- AMQP port: `5672`
- Management UI: `15672`
- Username: `mythcloth`
- Password: `mythcloth`

Start RabbitMQ:

```sh
docker compose up -d
```

Verify the container:

```sh
docker compose ps
docker compose logs -f rabbitmq
```

Open the management UI:

`http://localhost:15672`

Login with:

- Username: `mythcloth`
- Password: `mythcloth`

### 4. Run the API

Start the app with the local profile:

```sh
./gradlew bootRun --args='--spring.profiles.active=local'
```

When startup is complete, the API runs at:

`http://localhost:8080/api/v1`

### 5. Verify it is running

Health endpoint:

```sh
curl http://localhost:8080/api/v1/actuator/health
```

### 6. Trigger a manual crawl

The API exposes an on-demand sync endpoint:

`POST /api/v1/markets/{storeName}/sync`

Example:

```sh
curl -X POST http://localhost:8080/api/v1/markets/NIN_NIN_GAME/sync
```

If accepted, the endpoint returns `202 Accepted`.

### 7. Run checks locally

Run the test task:

```sh
./gradlew test
```

Run formatting checks:

```sh
./gradlew spotlessCheck
```

Run the standard verification lifecycle:

```sh
./gradlew check
```

Run mutation tests:

```sh
./gradlew pitest
```

---

## Important local behavior

- `server.servlet.context-path` is always `/api/v1`.
- Actuator is exposed under `/actuator`, so the health URL is `/api/v1/actuator/health`.
- The `local` profile disables every scheduled crawler job by default.
- The `prod` profile enables every scheduled crawler job by default.
- RabbitMQ SSL is disabled in `local` and enabled in `prod`.
- Some crawlers use Playwright to fetch fully rendered HTML for stores that require browser execution.

---

## API notes

- There is currently one HTTP endpoint: `POST /markets/{storeName}/sync`
- The endpoint triggers the same synchronization flow used by the Quartz jobs
- The service retrieves listings from the selected store and publishes them to RabbitMQ
- No Swagger/OpenAPI endpoint is configured in this project at the moment
- No Spring Security module is configured in this project at the moment

---

## Useful local commands

Start RabbitMQ:

```sh
docker compose up -d
```

Stop RabbitMQ:

```sh
docker compose down
```

Stop RabbitMQ and delete the volume:

```sh
docker compose down -v
```

Run the app:

```sh
./gradlew bootRun --args='--spring.profiles.active=local'
```

Run a clean build:

```sh
./gradlew clean build
```

Apply formatting:

```sh
./gradlew spotlessApply
```
