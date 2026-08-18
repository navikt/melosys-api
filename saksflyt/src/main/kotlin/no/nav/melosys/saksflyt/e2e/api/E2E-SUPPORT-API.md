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

### 2. Process Instance Marker

Returns a server timestamp to use as the `after` parameter on `/await`. Take it **before** the
action that starts a process.

**Endpoint:** `GET /internal/e2e/process-instances/marker`

```bash
curl http://localhost:8080/internal/e2e/process-instances/marker
# {"marker":"2026-07-31T14:35:19.249748"}
```

The value is truncated to microseconds to match the precision of the `REGISTRERT_DATO` column
(Oracle `TIMESTAMP(6)`), so an instance saved just after the marker can never round below it.

---

### 3. Await Process Instances

Waits for saga process instances to complete, with failure detection.

**Endpoint:** `GET /internal/e2e/process-instances/await`

**Parameters:**
- `timeoutSeconds` (optional, default: 30) - Maximum time to wait. Also bounds the server-side wait.
- `after` (optional) - A marker from `/process-instances/marker`, taken **before** the action.
  Only instances registered strictly after it count as the caller's own work.
- `expectedNew` (optional, default: 1 when `after` is given) - How many post-marker instances must
  exist. `0` means "drain": everything registered after the marker must be finished, but nothing new
  is required.
There is no longer an `expectedInstances` parameter. It counted instances in the whole 60-second
window, which a previous test step satisfies just as well as the caller's own work — so it never
coordinated on the action you had just triggered. Its last caller (`tests/core/sed-mottak.spec.ts`)
moved to `after`, and the parameter was removed. A stale caller still sending it gets no error:
Spring ignores unknown query parameters, so the wait silently degrades to the legacy contract.

**Two contracts:**

| | Without `after` (legacy) | With `after` (recommended) |
|---|---|---|
| What is waited for | everything registered in the last 60 s | only instances registered after the marker |
| Can the previous step's work satisfy it? | **yes — this is the race** | no |
| Initial settling delay | 200 ms (see config below) | skipped when `expectedNew > 0` |
| Poll cadence | fixed 500 ms | 25 ms, backing off to 500 ms |

**Usage:**
```bash
# Legacy: wait up to 30 seconds (default)
curl http://localhost:8080/internal/e2e/process-instances/await

# Legacy: wait up to 60 seconds
curl "http://localhost:8080/internal/e2e/process-instances/await?timeoutSeconds=60"

# Race-free: marker BEFORE the action, then wait for what the action started
MARKER=$(curl -s http://localhost:8080/internal/e2e/process-instances/marker | jq -r .marker)
# ... trigger the action ...
curl "http://localhost:8080/internal/e2e/process-instances/await?after=$MARKER&expectedNew=1"

# Drain between tests: everything since the marker must be finished, nothing new required
curl "http://localhost:8080/internal/e2e/process-instances/await?after=$MARKER&expectedNew=0"
```

**Known limitations of the marker contract:**
- Unfinished work registered *before* the marker is not waited for at all.
- `expectedNew` is a minimum (`>=`). An action that fans out to N instances needs `expectedNew=N`;
  there is no way to say "and nothing more will arrive".
- A restarted instance (`ProsessStatus.RESTARTET`) reuses its row, and `REGISTRERT_DATO` is
  `updatable = false`, so a restart is invisible to the marker and will time out with
  "only 0 of 1 expected new process instance(s)".
- The marker excludes the *previous step's* work, not any unrelated instance registered after it.
  That is exact only because melosys-e2e-tests runs `workers: 1`.

#### Response Scenarios

##### ✅ Success (HTTP 200)
All process instances completed successfully:
```json
{
  "status": "COMPLETED",
  "message": "All process instances completed successfully",
  "totalInstances": 10,
  "newInstances": 1,
  "elapsedSeconds": 5
}
```
`newInstances` is the number of instances registered after `after`; it is `0` on the legacy contract.

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
  "newInstances": 0,
  "notFinished": 7,
  "notFinishedIds": ["uuid1", "uuid2", ...],
  "elapsedSeconds": 30
}
```
With `after`, a timeout caused by too few new instances says so instead:
`"Timeout after 30s: only 0 of 1 expected new process instance(s) were registered after <marker>"`.

##### 🚫 Bad request (HTTP 400)
Invalid parameter combinations are rejected rather than silently falling back to the racy path:
blank `after`, `expectedNew` without `after`, negative `expectedNew`, a marker carrying a timezone
or offset (the container clock is not the caller's), a marker in the future, and any `after` that is
not the exact format handed out by `/process-instances/marker`.
```json
{
  "status": "BAD_REQUEST",
  "message": "'expectedNew' requires 'after' — without a marker there is nothing to count new instances from"
}
```

---

## Configuration

| Property | Default | Purpose |
|---|---|---|
| `melosys.e2e.initial-settling-delay-ms` | `200` | How long `/await` waits before its first look on the legacy contract (and on `expectedNew=0` drains). Setting it to `0` makes the race the marker contract fixes reproducible on demand. |

---

## Architecture

### Location
```
saksflyt/src/main/kotlin/no/nav/melosys/saksflyt/e2e/api/E2ESupportController.kt
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

**With a marker (`after`)** — the marker *is* the coordination. Instances registered before the
caller's action cannot satisfy the wait, and neither can an empty database, so `expectedNew`
subsumes the safeguards below.

**Without a marker (legacy)** the endpoint falls back to heuristics, and they are heuristics:
1. **Initial settling delay** (200 ms) - allows database transactions to commit and tasks to be submitted to the thread pool
2. **Recent instance filtering** - only monitors instances created within the last 60 seconds, ignoring stale test data
3. **Active instance tracking** - must observe at least one active instance, or at least one recent
   instance, before claiming completion

All three are what the marker replaces: the settling delay was in practice the only thing
separating the caller's own work from the previous step's, and in a multi-step e2e test the
60-second window is never empty, so 3 is trivially satisfied.

The legacy contract is still sound for one job — draining trailing work (oppgave, brev) before an
assertion, where there is no single action to take a marker around. It must not be used to wait on
a specific action.

**Sampling order matters:** `checkProcessStatus` reads the thread pool *before* the database.
The reverse order can miss work entirely — a task that is running at the database read, then
registers a child instance and finishes before the thread count is read, yields "nothing pending
in the database" and "executor idle" at the same time.

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
- **Polling interval, legacy contract**: fixed 500 ms. Deliberately *not* faster: polling the legacy
  path more often makes its race worse, since it more often samples the short window where the
  previous step's work looks finished.
- **Polling interval, marker contract**: 25 ms, doubling up to 500 ms. The marker wait starts before
  the instance is even registered and usually resolves in tens of milliseconds.
- **Initial settling delay**: 200 ms, configurable — skipped when `expectedNew > 0`, since demanding
  new instances subsumes it
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
