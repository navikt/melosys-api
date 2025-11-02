# E2E Support API

## Overview

The E2E Support API provides endpoints for automated end-to-end testing of melosys-api. These endpoints are **only available** when running with the `local-mock` profile.

## Endpoints

### Base URL
```
http://localhost:8080/internal/e2e
```

### 1. Clear Caches

Clears all application caches after database reset.

**Endpoint:** `POST /internal/e2e/caches/clear`

**Usage:**
```bash
curl -X POST http://localhost:8080/internal/e2e/caches/clear
```

**Response:**
```json
{
  "jpa-first-level-cache": "cleared",
  "jpa-second-level-cache": "cleared",
  "spring-caches": "cleared: [tilgangsmaskinen, kodeverk]"
}
```

**What it clears:**
- JPA first-level cache (EntityManager persistence context)
- JPA second-level cache (Hibernate L2 cache)
- Spring caches (e.g., `@Cacheable` caches)

---

### 2. Await Process Instances

Waits for all saga process instances to complete, with failure detection.

**Endpoint:** `GET /internal/e2e/process-instances/await`

**Parameters:**
- `timeoutSeconds` (optional, default: 30) - Maximum time to wait
- `expectedInstances` (optional) - Minimum number of process instances expected. Useful for explicit coordination when you know exactly how many instances will be created.

**Usage:**
```bash
# Wait up to 30 seconds (default)
curl http://localhost:8080/internal/e2e/process-instances/await

# Wait up to 60 seconds
curl http://localhost:8080/internal/e2e/process-instances/await?timeoutSeconds=60

# Wait for at least 2 specific instances to be created and completed
curl http://localhost:8080/internal/e2e/process-instances/await?expectedInstances=2&timeoutSeconds=60
```

#### Response Scenarios

##### ✅ Success (HTTP 200)
All process instances completed successfully:
```json
{
  "status": "COMPLETED",
  "message": "All process instances completed successfully",
  "totalInstances": 10,
  "elapsedSeconds": 5
}
```

##### ❌ Failure (HTTP 500)
One or more process instances failed:
```json
{
  "status": "FAILED",
  "message": "Found 2 failed process instance(s)",
  "failedInstances": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "type": "MOTTAK_SED",
      "status": "FEILET",
      "sistFullførtSteg": "OPPRETT_OPPGAVE",
      "error": {
        "type": "ValidationException",
        "steg": "OPPRETT_OPPGAVE",
        "melding": "Validation failed: missing required field",
        "dato": "2025-01-15T10:30:00"
      }
    }
  ],
  "elapsedSeconds": 12
}
```

##### ⏱️ Timeout (HTTP 408)
Timeout reached before completion:
```json
{
  "status": "TIMEOUT",
  "message": "Timeout after 30s waiting for process instances to complete",
  "activeThreads": 2,
  "queueSize": 5,
  "totalInstances": 20,
  "notFinished": 7,
  "notFinishedIds": ["uuid1", "uuid2", ...],
  "elapsedSeconds": 30
}
```

---

## Architecture

### Location
```
saksflyt/src/main/kotlin/no/nav/melosys/saksflyt/e2esupport/E2ESupportController.kt
```

### Security
- **Profile-restricted**: `@Profile("local-mock")` - endpoints only exist in local-mock mode
- **Unprotected**: `@Unprotected` - no authentication required (for E2E automation)
- **Not in production**: These endpoints are completely unavailable in production deployments

### Monitoring
The endpoint monitors:
1. **Thread Pool**: Active threads in `saksflytThreadPoolTaskExecutor`
2. **Queue Size**: Pending process instances in executor queue
3. **Database**: Recent `Prosessinstans` records (created within last 60s) and their statuses
4. **Event History**: `ProsessinstansHendelse` records for failure details

### Race Condition Protection
The endpoint includes multiple safeguards to prevent false-positives:
1. **Initial settling delay** (200ms) - allows database transactions to commit and tasks to be submitted to thread pool
2. **Recent instance filtering** - only monitors instances created within the last 60 seconds, ignoring stale test data
3. **Active instance tracking** - must observe at least one active instance before claiming completion (unless `expectedInstances` is specified)
4. **Expected count verification** - when `expectedInstances` is specified, ensures at least that many instances were created

---

## E2E Test Workflow

Typical E2E test sequence:

```bash
# 1. Reset database (external script)
./reset-database.sh

# 2. Clear all caches
curl -X POST http://localhost:8080/internal/e2e/caches/clear

# 3. Trigger test scenario (e.g., send kafka message)
./trigger-test-scenario.sh

# 4. Wait for processing to complete
curl http://localhost:8080/internal/e2e/process-instances/await?timeoutSeconds=60

# 5. Verify results in database
./verify-test-results.sh
```

---

## OpenAPI/Swagger

The endpoints are documented in OpenAPI under the tag:
```
Tag: "e2e-support"
Description: "E2E test support endpoints (local-mock profile only)"
```

Access the Swagger UI at:
```
http://localhost:8080/swagger-ui.html
```

---

## Design Decisions

### Why `/internal/e2e`?
- Groups with other operational endpoints (`/internal/health`, `/internal/prometheus`)
- Clearly separates from business API (`/api/*`)
- Indicates these are infrastructure/support endpoints

### Why `/process-instances/await`?
- Follows REST controller pattern (resource + action)
- `process-instances` is the resource being monitored
- `await` clearly indicates blocking/waiting behavior
- More intuitive than Norwegian term "prosessinstanser"

### Why not pure REST?
For operational endpoints, **clarity and developer experience** trump REST purism. Similar patterns are used by:
- GitHub API: `POST /repos/:owner/:repo/merges`
- Stripe API: `POST /charges/:id/capture`
- Kubernetes API: `POST /api/v1/namespaces/{ns}/pods/{name}/exec`

---

## Troubleshooting

### Endpoint returns 404
**Cause**: Not running with `local-mock` profile

**Solution**:
```bash
# Check active profile
curl http://localhost:8080/internal/health | jq .profiles

# Should include "local-mock"
```

### "All instances are UNDER_BEHANDLING but not progressing"
**Cause**: Thread pool might be stuck or database locks

**Solution**:
```bash
# Check logs
docker-compose logs -f melosys-api

# Check database locks
# Connect to Oracle and check v$locked_object
```

### Timeout even though everything looks complete
**Cause**: Might be checking too early, processes still in queue

**Solution**: Increase timeout or check thread pool status manually:
```bash
# Longer timeout
curl "http://localhost:8080/internal/e2e/process-instances/await?timeoutSeconds=120"
```

---

## Implementation Notes

### Polling Configuration
- **Polling interval**: 500ms - balances responsiveness with system load
- **Initial settling delay**: 200ms - allows transactions to commit before first check
- **Recent instance cutoff**: 60 seconds - only monitors recently created instances

### Error Message Truncation
Error messages from `ProsessinstansHendelse` are truncated to 500 characters to prevent huge responses.

### Thread Safety
The endpoint is thread-safe and can handle concurrent calls, though only one should be active per test scenario.

---

## Migration from Old Endpoints

If you have existing E2E tests using the old paths:

**Old:**
```bash
POST /api/test/clear-caches
GET /api/test/wait-for-prosessinstanser
```

**New:**
```bash
POST /internal/e2e/caches/clear
GET /internal/e2e/process-instances/await
```

Update your test scripts accordingly.
