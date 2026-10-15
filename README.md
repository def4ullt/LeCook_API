# LeCook API

Java 17 / Spring Boot API for the offline Android recipe book. PostgreSQL and
S3-compatible object storage are provided by Neon. The catalog includes 60
Ukrainian recipes with shared ingredients, quantities, units and separate steps.

## Deploy on Render

Create **Web Service**, connect this repository, and select:

| Setting | Value |
| --- | --- |
| Branch | `master` |
| Language / Runtime | `Docker` |
| Root Directory | empty |
| Dockerfile Path | `./Dockerfile` |
| Instance Type | `Free` |
| Region | `Ohio` (near the existing Neon database) |
| Health Check Path | `/api/ping` |

Leave custom build and Docker commands empty. The image builds the executable
JAR, runs as a non-root user and reads Render's `PORT` environment variable.
It is also possible to create a Blueprint from `render.yaml`; ordinary manual
Web Service creation does not automatically import its environment variables.

Add the variable names from `.env.example` in Render Environment. Use the pooled
Neon URL for `DATABASE_URL`, and the direct URL for `DATABASE_URL_UNPOOLED`.
Supply real values only through Render's environment settings. Keep the admin
token private, at least 32 characters long. Never add server secrets to the APK.

After deployment:

- `GET /api/ping`: process liveness, no database access. Use this for Render
  health checks and optional external uptime pings.
- `GET /api/health`: verifies a real database query.
- `POST /api/recipes/sync/page` with `{"knownIds":[]}`: downloads the catalog.

Catalog writes require `Authorization: Bearer <LECOOK_ADMIN_TOKEN>` in the
render profile. Reads and sync are public. The image does not re-import recipes
on startup; Flyway applies versioned schema migrations without clearing data.

Render Free can sleep after 15 minutes without incoming traffic and can restart.
The Android app must tolerate cold starts and retain its offline book.
See [Render Docker](https://render.com/docs/docker) and
[Free service limits](https://render.com/docs/free).

## Local development

Run `./gradlew bootRun` or the built executable JAR. In the complete local
LeCook workspace the backend reads the parent `.env.local`. For a backend-only
checkout, put service variables in `.env.local` in this directory.

Local PostgreSQL development credentials can be placed in ignored
`local-db.properties` using standard `spring.datasource.*` properties.
Normal tests use a generated isolated schema and remove only that schema.
Set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and
`SPRING_DATASOURCE_PASSWORD` to override local test configuration.

```sh
./gradlew test bootJar
docker build -t lecook-api .
docker run --rm --env-file .env.local -p 8080:8080 lecook-api
```

Secrets and local configurations are excluded from Git and the Docker build.
The first catalog import is explicit: start the JAR with
`--lecook.catalog.import=true --spring.main.web-application-type=none`.
Stable recipe IDs make repeated imports skip existing catalog recipes.
