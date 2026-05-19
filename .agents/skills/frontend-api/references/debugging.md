# Frontend API Debugging Guide

## Common Issues

### 1. 401 Unauthorized

**Symptom**: Endpoint returns 401

**Cause**: Missing or invalid JWT token

**Debug**:
```kotlin
// Check if @Protected is present
@Protected
@RestController
class MyController

// Check token in request
Authorization: Bearer <token>

// Verify token claims
val userId = SubjectHandler.getInstance().getUserID()
log.info("User: $userId")
```

### 2. 403 Forbidden

**Symptom**: `SikkerhetsbegrensningException`

**Cause**: User doesn't have access to resource

**Debug**:
```kotlin
// Check Tilgangsmaskinen result
aksesskontroll.autoriserSakstilgang(saksnummer)

// Verify person access
aksesskontroll.autoriserFolkeregisterIdent(fnr)
```

### 3. 404 Not Found

**Symptom**: `IkkeFunnetException`

**Cause**: Resource doesn't exist

**Debug**:
```sql
-- Check database
SELECT * FROM fagsak WHERE saksnummer = 'MEL-123';
SELECT * FROM behandling WHERE id = 456;
```

### 4. 400 Bad Request

**Symptom**: `FunksjonellException` or `ValideringException`

**Cause**: Invalid input or business rule violation

**Debug**:
```kotlin
// Check error message in response
{
  "status": 400,
  "error": "Bad Request",
  "message": "Error description",
  "feilkoder": ["CODE1", "CODE2"]
}
```

### 5. 500 Internal Server Error

**Symptom**: Unexpected exception

**Cause**: Bug or external service failure

**Debug**:
```bash
# Check logs
grep "API kall feilet" /var/log/melosys.log

# Look for correlationId
grep "correlationId" /var/log/melosys.log
```

## Logging

### Request Logging

```kotlin
private val log = KotlinLogging.logger { }

@GetMapping("/{id}")
fun get(@PathVariable("id") id: Long): ResponseEntity<MyDto> {
    log.info("Henter ressurs med id: $id")
    // ...
}
```

### User Context Logging

```kotlin
log.info(
    "Saksbehandler {} ber om å endre fagsak {} med sakstype {}",
    SubjectHandler.getInstance().getUserID(),
    saksnummer,
    sakstype
)
```

## Testing Endpoints

### cURL Examples

```bash
# Get with auth
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8080/fagsaker/MEL-123

# Post with body
curl -X POST \
     -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"ident": "12345678901"}' \
     http://localhost:8080/fagsaker/sok
```

### Integration Test

```kotlin
@Test
fun `should return fagsak`() {
    val token = mockOAuth2Server.issueToken(
        issuerId = "aad",
        subject = "Z123456"
    ).serialize()

    mockMvc.perform(
        get("/fagsaker/MEL-123")
            .header("Authorization", "Bearer $token")
    )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.saksnummer").value("MEL-123"))
}
```

## Common Patterns

### Empty List vs 404

```kotlin
// Return empty list if nothing found (don't throw 404)
@PostMapping("/sok")
fun search(@RequestBody dto: SearchDto): List<ResultDto> {
    return service.search(dto.query)
        .map { ResultDto.from(it) }
        // Returns [] if no results
}
```

### Optional Resource

```kotlin
// Return null if not found, let caller handle
fun findOptional(id: Long): MyDto? =
    repository.findById(id)
        .map { MyDto.from(it) }
        .orElse(null)
```

### Audit Logging

```kotlin
// Always audit when accessing person data
aksesskontroll.auditAutoriserFolkeregisterIdent(
    fnr,
    "Søk på person med ident. Oversikt over saker og behandlinger."
)
```

## Swagger/OpenAPI

### Access Swagger UI

```
http://localhost:8080/swagger-ui/
```

### Check API Spec

```
http://localhost:8080/v3/api-docs
```

## Response Structure

### Success with Data

```json
{
  "saksnummer": "MEL-123",
  "sakstype": "EU_EOS",
  "behandlinger": [...]
}
```

### Success without Data

```
HTTP 204 No Content
(empty body)
```

### Error Response

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Description of error",
  "correlationId": "uuid-...",
  "feilkoder": ["CODE1"]
}
```

## Related Skills

- **security**: Token validation issues
- **testing**: Integration test setup
- **saksflyt**: Process errors
