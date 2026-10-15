# Render preparation verification

Verified on 2026-10-15 before publishing this source update.

- `gradlew test bootJar`: passed. 15 active tests, 4 opt-in live checks skipped.
- Local development database credentials moved to ignored `local-db.properties`.
- Working files, staged contents and existing Git history checked against the
  actual service credentials. None found. Private environment files are excluded
  from Git and Docker context.
- Render profile started with a 256 MB maximum Java heap and the same JVM flags
  configured in the Dockerfile. Observed Windows process working set: 292 MB.
  This is a local measurement, not a guarantee of container peak memory.
- Real HTTP checks against the existing Neon catalog:

```text
GET /api/ping                  200
GET /api/health                200
POST /api/recipes/sync/page    60 recipes with separate steps
Repeated sync                 0 additional recipes
Unauthenticated catalog write 401
```

No catalog rows were changed by these live HTTP checks. Schema migrations were
already applied; this deployment preparation introduces no schema migration.

Local Docker Desktop did not become available, so a Linux container build and
execution were not verified on this PC. Render's first Docker build remains
the container acceptance check. Dockerfile base/runtime are Java 17; builds use
the committed Gradle wrapper and no local service credentials.

The public Render service has not been created by this preparation. Follow
README.md, add service credentials through Render Environment and then verify
the public health/sync endpoints before sharing the API URL with Android users.
