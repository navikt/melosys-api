---
name: frontend-api
description: |
  Expert knowledge of REST endpoint patterns in melosys-api's frontend-api module.
  Use when: (1) Creating new REST endpoints for melosys-web,
  (2) Understanding controller patterns and Swagger documentation,
  (3) Working with DTOs and request/response objects,
  (4) Understanding exception handling and HTTP status codes,
  (5) Adding access control to endpoints.
---

# Frontend API Skill

Expert knowledge of REST endpoint patterns in the frontend-api module.

## Quick Reference

### Module Location

```
frontend-api/src/main/kotlin/no/nav/melosys/tjenester/gui/
```

### Package Structure

| Package | Purpose |
|---------|---------|
| `gui/` | Root controllers |
| `gui/dto/` | Request/response DTOs |
| `gui/fagsaker/` | Case-related endpoints |
| `gui/behandlinger/` | Treatment endpoints |
| `gui/ftrl/` | FTRL (folketrygdloven) endpoints |
| `gui/brev/` | Letter endpoints |
| `gui/saksflyt/` | Process flow endpoints |
| `gui/aarsavregning/` | Annual reconciliation endpoints |
| `gui/unntakshandtering/` | Exception handling |

## Controller Pattern

### Basic Controller

```kotlin
@Protected
@RestController
@RequestMapping("/my-resource")
@Tag(name = "my-resource")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
class MyController(
    private val myService: MyService,
    private val aksesskontroll: Aksesskontroll
) {
    private val log = KotlinLogging.logger { }

    @GetMapping("/{id}")
    @Operation(
        summary = "Short description",
        description = "Longer description of what this endpoint does."
    )
    fun get(@PathVariable("id") id: Long): ResponseEntity<MyDto> {
        aksesskontroll.autoriserSkriv(id)
        val result = myService.hent(id)
        return ResponseEntity.ok(MyDto.from(result))
    }

    @PostMapping
    @Operation(summary = "Create resource")
    fun create(@RequestBody dto: CreateDto): ResponseEntity<Void> {
        myService.create(dto)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update resource")
    fun update(
        @PathVariable("id") id: Long,
        @RequestBody dto: UpdateDto
    ): ResponseEntity<Void> {
        aksesskontroll.autoriserSkriv(id)
        myService.update(id, dto)
        return ResponseEntity.noContent().build()
    }
}
```

### Key Annotations

| Annotation | Purpose |
|------------|---------|
| `@Protected` | Requires JWT token |
| `@RestController` | REST endpoint |
| `@RequestMapping` | Base path |
| `@Tag` | Swagger grouping |
| `@Scope(REQUEST)` | Per-request instance |
| `@Operation` | Swagger documentation |

## DTO Patterns

### Response DTO

```kotlin
data class MyDto(
    val id: Long,
    val status: String,
    val perioder: List<PeriodeDto>
) {
    companion object {
        fun from(entity: MyEntity) = MyDto(
            id = entity.id,
            status = entity.status.kode,
            perioder = entity.perioder.map { PeriodeDto.from(it) }
        )
    }
}
```

### Request DTO

```kotlin
data class CreateMyDto(
    val saksnummer: String,
    val behandlingId: Long,
    val fom: LocalDate,
    val tom: LocalDate?
)
```

### Nested DTOs

```kotlin
data class FagsakDto(
    val saksnummer: String,
    val sakstype: Sakstyper,
    val behandlinger: List<BehandlingOversiktDto>
)

data class BehandlingOversiktDto(
    val behandlingID: Long,
    val tittel: String,
    val behandlingsstatus: Behandlingsstatus,
    val behandlingstype: Behandlingstyper
)
```

## Exception Handling

### Exception Types

| Exception | HTTP Status | Use Case |
|-----------|-------------|----------|
| `IkkeFunnetException` | 404 | Resource not found |
| `FunksjonellException` | 400 | Business rule violation |
| `ValideringException` | 400 | Validation failure |
| `SikkerhetsbegrensningException` | 403 | Access denied |
| `JwtTokenUnauthorizedException` | 401 | Not authenticated |

### ExceptionMapper

```kotlin
@ControllerAdvice
class ExceptionMapper {

    @ExceptionHandler(IkkeFunnetException::class)
    fun håndter(e: IkkeFunnetException, request: HttpServletRequest) =
        håndter(e, request, HttpStatus.NOT_FOUND, Level.WARN)

    @ExceptionHandler(FunksjonellException::class)
    fun håndter(e: FunksjonellException, request: HttpServletRequest) =
        håndter(e, request, HttpStatus.BAD_REQUEST, Level.WARN)
}
```

### Throwing Exceptions

```kotlin
// Business rule violation
throw FunksjonellException("BrukerID eller organisasjonsnummer trengs for å opprette en sak.")

// Not found
throw IkkeFunnetException("Fant ikke behandling med id $behandlingId")

// Validation failure
throw ValideringException("Ugyldig periode", listOf("FOM_ETTER_TOM"))
```

## Access Control

### Aksesskontroll Service

```kotlin
// Check access to person
aksesskontroll.autoriserFolkeregisterIdent(fnr)

// Check access with audit log
aksesskontroll.auditAutoriserFolkeregisterIdent(fnr, "Reason for access")

// Check access to case
aksesskontroll.autoriserSakstilgang(saksnummer)
aksesskontroll.auditAutoriserSakstilgang(fagsak, "Reason for access")

// Check write access to treatment
aksesskontroll.autoriserSkriv(behandlingId)
```

### User Context

```kotlin
// Get current user
val userId = SubjectHandler.getInstance().getUserID()

// Log with user
log.info("Saksbehandler {} ber om å hente behandling {}", userId, behandlingId)
```

## Response Patterns

### Returning Data

```kotlin
// Single object
return ResponseEntity.ok(dto)

// List
return ResponseEntity.ok(dtoList)

// Empty list on not found
return ResponseEntity.ok(emptyList())
```

### No Content

```kotlin
// Successful mutation without body
return ResponseEntity.noContent().build()
```

### Conditional Response

```kotlin
return if (result != null) {
    ResponseEntity.ok(ResultDto.from(result))
} else {
    ResponseEntity.ok(EmptyResultDto())
}
```

## OpenAPI/Swagger

### Configuration

Swagger UI available at: `/swagger-ui/`

### Annotations

```kotlin
@Tag(name = "fagsaker")  // Group endpoints
@Operation(
    summary = "Short description",
    description = "Longer description"
)
@GetMapping("/{id}")
fun get(@PathVariable("id") id: Long): ResponseEntity<MyDto>
```

## Search Endpoints

### Pattern

```kotlin
@PostMapping("/sok")
@Operation(summary = "Search for resources")
fun search(@RequestBody searchDto: SearchDto): List<ResultDto> = when {
    StringUtils.isNotEmpty(searchDto.ident) -> {
        aksesskontroll.auditAutoriserFolkeregisterIdent(
            searchDto.ident, "Search description"
        )
        service.searchByIdent(searchDto.ident).map { ResultDto.from(it) }
    }
    StringUtils.isNotEmpty(searchDto.saksnummer) -> {
        service.searchBySaksnummer(searchDto.saksnummer)?.let {
            aksesskontroll.auditAutoriserSakstilgang(it, "Search description")
            listOf(ResultDto.from(it))
        } ?: emptyList()
    }
    else -> emptyList()
}
```

## Feature Toggles

```kotlin
@Autowired
private lateinit var unleash: Unleash

fun doSomething() {
    if (unleash.isEnabled(ToggleName.MY_FEATURE)) {
        // New behavior
    } else {
        // Old behavior
    }
}
```

## Related Skills

- **security**: Authentication and authorization
- **fagsak**: Case management
- **behandling**: Treatment lifecycle
- **saksflyt**: Process orchestration
