---
name: frontend-api
description: |
  Expert knowledge of REST endpoint patterns in melosys-api's frontend-api module.
  Use when: (1) Creating new REST endpoints for melosys-web,
  (2) Understanding controller patterns and Swagger documentation,
  (3) Working with DTOs and request/response objects,
  (4) Understanding exception handling and HTTP status codes,
  (5) Adding access control to endpoints.
  Triggers: "new endpoint", "REST controller", "@RestController", "add a DTO",
  "ResponseEntity", "Aksesskontroll", "access control on endpoint",
  "Swagger/@Operation", "return 404/403/401/400", "@Protected", "frontend-api",
  "search endpoint", "/sok".
---

# Frontend API Skill

Expert knowledge of REST endpoint patterns in the frontend-api module.

## Quick Reference

### Module Location

```
frontend-api/src/main/kotlin/no/nav/melosys/tjenester/gui/
```

### Package Structure

**Convention: the package path mirrors the API path.** A controller mapped to
`/fagsaker/{saksnummer}/notater` belongs in `gui/fagsaker/notater/`. Its DTOs are placed
alongside it in the same package, not in a shared DTO package.

```
/fagsaker/{saksnummer}/trygdeavgift        ->  gui/fagsaker/trygdeavgift/
/behandlinger/{behandlingID}/trygdeavgift  ->  gui/behandlinger/trygdeavgift/
/fagsaker/{saksnummer}/notater             ->  gui/fagsaker/notater/
```

Rationale: most DTOs serve exactly one controller, so a shared DTO package provides little reuse
while inviting accidental coupling between unrelated endpoints. Keeping an endpoint's controller,
DTOs and tests in one package means everything that changes together lives together.

Note this is **not** package-by-feature. The packages follow the REST resource hierarchy, not the
domain concept. Where the two disagree, the path wins: annual reconciliation is one domain feature
but two resources, so it belongs in `gui/behandlinger/aarsavregninger/` and
`gui/fagsaker/aarsavregninger/` rather than a single `gui/aarsavregning/`.

### DTOs in the controller file

Declare a controller's DTOs as top-level types **below the controller class in the same file**,
as long as the file stays readable. A request DTO with two fields does not earn its own file.

```kotlin
@RestController
@RequestMapping("/fagsaker")
class BehandlingsnotatController(/* ... */) {
    // endpoints
}

data class BehandlingsnotatPostDto(
    val tekst: String,
    val behandlingId: Long? = null,
)
```

Existing examples: `BehandlingsnotatController.kt` (105 lines, 2 DTOs) and
`PensjonsopptjeningController.kt` (60 lines, 2 DTOs).

**Split the DTOs into their own files once the controller file grows past roughly 150 lines, or
holds more than a handful of DTOs.** `aarsavregning/ÅrsavregningController.kt` shows the failure
mode: 368 lines where the controller ends around line 290 and the rest is 11 data classes. At that
point the DTOs should move to a `dto/` subpackage next to the controller, as
`ftrl/medlemskapsperiode/dto/` does. Keep them in the controller's own package either way — the
choice is file granularity, not package.

| Package | Purpose |
|---------|---------|
| `gui/fagsaker/` | Case endpoints (`/fagsaker/**`) |
| `gui/behandlinger/` | Treatment endpoints (`/behandlinger/**`) |
| `gui/ftrl/` | FTRL (folketrygdloven) endpoints |
| `gui/brev/` | Letter endpoints |
| `gui/saksflyt/` | Process flow endpoints |
| `gui/kodeverk/` | Code registry endpoints |
| `gui/kontroll/` | Validation endpoints |
| `gui/saksbehandling/` | Case processing endpoints |
| `gui/avklartefakta/` | Clarified facts endpoints |
| `gui/graphql/` | GraphQL endpoints, DTOs and mappers |
| `gui/unntakshandtering/` | Exception handling (`ExceptionMapper`) |
| `gui/config/` | Module configuration (OpenAPI, Jackson) |

Known deviations, listed so they are not copied:

| Package | Actual path | Where the rule would put it |
|---------|-------------|------------------------------|
| `gui/aarsavregning/` | `/behandlinger/{id}/aarsavregninger`, `/fagsaker/{saksnummer}/aarsavregninger` | `gui/behandlinger/aarsavregninger/`, `gui/fagsaker/aarsavregninger/` |
| `gui/pensjonsopptjening/` | `/behandlinger/{id}/pensjonsopptjening` | `gui/behandlinger/pensjonsopptjening/` |
| `gui/helseutgiftdekkesperiode/` | `/behandlinger/{id}/helseutgift-dekkes-perioder` | `gui/behandlinger/helseutgiftdekkesperioder/` |

These are named after the domain concept rather than the resource — the package-by-feature instinct.

Two legacy placements exist and should **not** be treated as patterns to follow:

- `gui/` root holds 23 older controllers (mostly Java) that predate the convention.
- `gui/dto/` holds ~105 DTOs from the same era. Do not add new DTOs here.

When you touch code in either location, moving it to the path-mirroring package is a welcome
cleanup — but keep it in a separate commit from any behavioural change.

## Controller Pattern

The examples below use generic placeholders (`MyController`, `MyDto`, `MyService`). For a real,
canonical implementation — covering the annotations, the search-dispatch `when {}` block, and the
audit calls — read `frontend-api/src/main/kotlin/no/nav/melosys/tjenester/gui/fagsaker/FagsakController.kt`.

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

`Aksesskontroll` is a Java interface in `service/src/main/java/no/nav/melosys/service/tilgang/Aksesskontroll.java`.
The live implementation is `TilgangsmaskinenAksesskontroll`
(`service/src/main/kotlin/no/nav/melosys/service/tilgangsmaskinen/TilgangsmaskinenAksesskontroll.kt`),
which delegates authorization to Tilgangsmaskinen. Controllers inject the interface; when access
control misbehaves, look at the implementation.

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

## Debugging

For HTTP-status triage (401/403/404/400/500), curl recipes, request/user-context logging, and
MockMvc integration-test setup, see [`references/debugging.md`](references/debugging.md).

## Related Skills

- **security**: Authentication and authorization
- **fagsak**: Case management
- **behandling**: Treatment lifecycle
- **saksflyt**: Process orchestration
