# ErPeriode Kotlin-konvertering - Two-Tier Period Architecture

## Sammendrag

**Problem:**
- ErPeriode-interface konvertert til Kotlin med `fom: LocalDate` (non-null)
- Gir type-sikkerhet, men utfordringer ved konvertering fra DTOs/JSON med nullable datoer
- Databasen krever `@Column(nullable = false)` på fom for alle periode-entiteter
- Anonyme objekter brukte Java-stil getters som ikke fungerer med Kotlin properties

**Løsning - To-lags arkitektur:**

1. **MuligPeriode** (Transport/DTO-lag)
   - `fom: LocalDate?` og `tom: LocalDate?` (begge nullable)
   - Brukes for JSON, eksterne APIer, uvaliderte data
   - Konverteringsmetoder:
     - `tilErPeriode()` → `ErPeriode?` (returnerer null hvis fom mangler)
     - `hentErPeriode()` → `ErPeriode` (kaster exception hvis fom mangler)

2. **ErPeriode** (Domene/Persistens-lag)
   - `fom: LocalDate` (non-null) og `tom: LocalDate?`
   - Brukes for domene-entiteter, forretningslogikk, database-lagring
   - Type-systemet garanterer at fom alltid er tilstede

3. **SimpleErPeriodeAdapter**
   - Konkret implementasjon av ErPeriode
   - Erstatter anonyme objekter: `object : ErPeriode { ... }`
   - Enkel bruk: `SimpleErPeriodeAdapter(fomDate, tomDate)`

**Fordeler:**
- Type-sikkerhet: Hvis du har `ErPeriode`, vet du at `fom` er non-null
- Tydelig separasjon mellom validert (ErPeriode) og uvalidert (MuligPeriode) data
- Tryggere database-operasjoner
- Idiomatisk Kotlin-kode uten boilerplate

---

## 1. Two-Tier Period Architecture

```mermaid
%%{init: {'theme':'base', 'themeVariables': { 'primaryColor':'#ffcc00','primaryTextColor':'#000','primaryBorderColor':'#ff9900','lineColor':'#666','secondaryColor':'#90EE90','tertiaryColor':'#ADD8E6'}}}%%
graph TB
    subgraph "Transport Layer (DTO/JSON)"
        MP[MuligPeriode<br/>fom: LocalDate?<br/>tom: LocalDate?]
    end

    subgraph "Domain/Persistence Layer (Database)"
        EP[ErPeriode<br/>fom: LocalDate ✓<br/>tom: LocalDate?]
        DB[(Database<br/>@Column nullable=false)]
    end

    MP -->|tilErPeriode| EP
    MP -->|hentErPeriode| EP

    JSON[External APIs<br/>JSON/DTOs] --> MP
    UI[Frontend] --> MP

    EP --> Integration[Integration<br/>JSON to external parties]
    EP --> Entities[Domain Entities<br/>Medlemskapsperiode<br/>Lovvalgsperiode]
    Entities --> DB

    classDef transportClass stroke:#f59e0b,stroke-width:3px
    classDef domainClass stroke:#10b981,stroke-width:3px
    classDef dbClass stroke:#3b82f6,stroke-width:3px

    class MP transportClass
    class EP domainClass
    class DB,Entities dbClass
```

**Forklaring:**
- **Transport Layer**: Håndterer data fra eksterne kilder hvor datoer kan mangle
- **Domain Layer**: Garanterer at fom alltid er tilstede før data brukes i forretningslogikk
- **Konvertering**: Eksplisitt validering ved overgang mellom lagene

---

## 2. The Problem: Anonymous Objects Before Kotlin Conversion

```mermaid
%%{init: {'theme':'neutral'}}%%
graph LR
    subgraph "Before (Java)"
        J[ErPeriode interface<br/>getFom: LocalDate<br/>getTom: LocalDate]
        A1[Anonymous Object 1<br/>new ErPeriode]
        A2[Anonymous Object 2<br/>new ErPeriode]
        A3[Anonymous Object 3<br/>new ErPeriode]
    end

    subgraph "After Kotlin Conversion"
        K[ErPeriode interface<br/>var fom: LocalDate<br/>var tom: LocalDate?]
        E1[❌ object : ErPeriode<br/>override fun getFom]
        E2[❌ object : ErPeriode<br/>override fun getFom]
        E3[❌ object : ErPeriode<br/>override fun getFom]
    end

    J --> K
    A1 -.broke.-> E1
    A2 -.broke.-> E2
    A3 -.broke.-> E3

    classDef javaClass stroke:#6b7280,stroke-width:2px
    classDef errorClass stroke:#ef4444,stroke-width:3px

    class J,A1,A2,A3 javaClass
    class K,E1,E2,E3 errorClass
```

**Problemet:**
- I Java hadde ErPeriode getter-metoder: `getFom()`, `getTom()`
- Anonyme objekter implementerte disse metodene
- Ved konvertering til Kotlin ble interface endret til properties: `var fom`, `var tom`
- Anonyme objekter med `override fun getFom()` kompilerer ikke lenger
- Løsningen er å bruke `SimpleErPeriodeAdapter` i stedet

**Eksempel på feil:**
```kotlin
// ❌ Fungerer ikke lenger:
object : ErPeriode {
    override fun getFom(): LocalDate = dagensDato.withDayOfYear(1)
    override fun getTom(): LocalDate? = periode.tom
}

// ✅ Riktig løsning:
SimpleErPeriodeAdapter(dagensDato.withDayOfYear(1), periode.tom)
```

---

## 3. The Solution: SimpleErPeriodeAdapter

```mermaid
%%{init: {'theme':'neutral'}}%%
graph TB
    subgraph "Solution"
        SEA[SimpleErPeriodeAdapter<br/>class SimpleErPeriodeAdapter<br/>override var fom: LocalDate<br/>override var tom: LocalDate?]
    end

    subgraph "Usage Examples"
        U1[SimpleErPeriodeAdapter<br/>dagensDato, periode.tom]
        U2[MuligPeriode.tilErPeriode<br/>returns SimpleErPeriodeAdapter]
        U3[Replace 20+ anonymous objects<br/>with SimpleErPeriodeAdapter]
    end

    SEA --> U1
    SEA --> U2
    SEA --> U3

    Benefits[✓ Type-safe<br/>✓ Reusable<br/>✓ No boilerplate<br/>✓ Idiomatic Kotlin]

    U1 --> Benefits
    U2 --> Benefits
    U3 --> Benefits

    classDef solutionClass stroke:#10b981,stroke-width:4px
    classDef usageClass stroke:#22c55e,stroke-width:2px
    classDef benefitClass stroke:#3b82f6,stroke-width:2px

    class SEA solutionClass
    class U1,U2,U3 usageClass
    class Benefits benefitClass
```

**SimpleErPeriodeAdapter implementasjon:**
```kotlin
class SimpleErPeriodeAdapter(
    override var fom: LocalDate,
    override var tom: LocalDate?
) : ErPeriode
```

**Bruksområder:**
1. Direkte instansiering når man trenger en ErPeriode
2. Intern implementasjon i `MuligPeriode.tilErPeriode()`
3. Erstatter alle anonyme ErPeriode-objekter i kodebasen

---

## 4. Conversion Flow

```mermaid
%%{init: {'theme':'neutral'}}%%
flowchart LR
    subgraph Input
        JSON[JSON from API<br/>fom: 2024-01-01<br/>tom: null]
        DTO[DTO Object<br/>nullable dates]
    end

    subgraph Validation
        MP[MuligPeriode<br/>fom: LocalDate?<br/>tom: LocalDate?]

        V{fom != null?}
    end

    subgraph Domain
        SEA[SimpleErPeriodeAdapter<br/>fom: LocalDate ✓<br/>tom: LocalDate?]
        EP[ErPeriode<br/>Type-safe guarantee]
    end

    subgraph Persistence
        ENT[Domain Entity<br/>Medlemskapsperiode]
        DB[(Database<br/>NOT NULL constraint)]
    end

    JSON --> DTO
    DTO --> MP
    MP --> V
    V -->|Yes| SEA
    V -->|No| ERR[❌ Error:<br/>fom required]
    SEA --> EP
    EP --> ENT
    ENT --> DB

    classDef inputClass stroke:#f59e0b,stroke-width:2px
    classDef domainClass stroke:#10b981,stroke-width:2px
    classDef dbClass stroke:#3b82f6,stroke-width:2px
    classDef errorClass stroke:#ef4444,stroke-width:3px

    class JSON,DTO,MP inputClass
    class SEA,EP domainClass
    class ENT,DB dbClass
    class ERR errorClass
```

**Flyten:**
1. **Input**: Data kommer fra eksterne kilder (JSON, APIer)
2. **Validation**: `MuligPeriode` sjekker om `fom` er tilstede
3. **Conversion**: Hvis valid, opprett `SimpleErPeriodeAdapter`
4. **Domain**: Brukes som `ErPeriode` i forretningslogikk
5. **Persistence**: Lagres i database med NOT NULL constraint på fom

**Kodeeksempel:**
```kotlin
// Fra JSON til MuligPeriode
val dto: PeriodeDTO = fromJson(...)
val muligPeriode: MuligPeriode = dto

// Validering og konvertering
val erPeriode: ErPeriode = muligPeriode.hentErPeriode() // kaster exception hvis fom == null
// eller
val erPeriode: ErPeriode? = muligPeriode.tilErPeriode() // returnerer null hvis fom == null

// Bruk i domene
medlemskapsperiode.periode = erPeriode
```

---

## 5. Type Safety Benefits

```mermaid
%%{init: {'theme':'neutral'}}%%
graph TB
    subgraph "Type System Guarantees"
        direction TB
        EP[ErPeriode interface]
        FOM[fom: LocalDate<br/>✓ Non-null guaranteed]
        TOM[tom: LocalDate?<br/>✓ Nullable explicit]

        EP --> FOM
        EP --> TOM
    end

    subgraph "Compile-Time Safety"
        C1[✓ No NullPointerException<br/>on fom access]
        C2[✓ Database constraints<br/>enforced by types]
        C3[✓ Clear intent:<br/>ErPeriode = valid period]
    end

    subgraph "Runtime Benefits"
        R1[✓ Earlier error detection]
        R2[✓ No defensive null checks<br/>needed for fom]
        R3[✓ Simplified business logic]
    end

    FOM --> C1
    TOM --> C2
    EP --> C3

    C1 --> R1
    C2 --> R2
    C3 --> R3

    classDef typeClass stroke:#10b981,stroke-width:3px
    classDef compileClass stroke:#3b82f6,stroke-width:2px
    classDef runtimeClass stroke:#22c55e,stroke-width:2px

    class EP,FOM,TOM typeClass
    class C1,C2,C3 compileClass
    class R1,R2,R3 runtimeClass
```

**Type-sikkerhetsfordeler:**

### Compile-Time (kompileringstid):
- **Ingen NPE på fom**: Kompilatoren garanterer at `fom` aldri er null
- **Database constraints**: Typesystemet matcher database-constraints
- **Klar intensjon**: `ErPeriode` betyr alltid en gyldig periode med fom

### Runtime (kjøretid):
- **Tidligere feiloppdagelse**: Feil oppdages ved validering, ikke når data brukes
- **Ingen defensive sjekker**: Slipper `if (periode.fom != null)` i forretningslogikk
- **Enklere kode**: Mindre boilerplate, tydeligere intensjon

**Kodesammenligning:**
```kotlin
// ❌ Med nullable fom (må sjekke overalt):
fun beregnTrygdeavgift(periode: MuligPeriode) {
    if (periode.fom == null) {
        throw IllegalArgumentException("fom er påkrevd")
    }
    val fom = periode.fom!! // unsafe, men nødvendig
    // ... resten av logikken
}

// ✅ Med ErPeriode (type-sikker):
fun beregnTrygdeavgift(periode: ErPeriode) {
    val fom = periode.fom // alltid safe, garantert non-null
    // ... resten av logikken
}
```

---

## Implementerte Endringer

### 1. Ny klasse: SimpleErPeriodeAdapter
```kotlin
// domain/src/main/kotlin/no/nav/melosys/domain/SimpleErPeriodeAdapter.kt
class SimpleErPeriodeAdapter(
    override var fom: LocalDate,
    override var tom: LocalDate?
) : ErPeriode
```

### 2. Oppdatert MuligPeriode interface
```kotlin
interface MuligPeriode {
    val fom: LocalDate?
    val tom: LocalDate?

    fun tilErPeriode(): ErPeriode? = fom?.let { fomDate ->
        SimpleErPeriodeAdapter(fomDate, tom)
    }

    fun hentErPeriode(): ErPeriode = tilErPeriode()
        ?: error("Kan ikke opprette ErPeriode: fom-dato er påkrevd men er null")
}
```

### 3. Erstattet anonyme objekter
**Før:**
```kotlin
val periode = object : ErPeriode {
    override fun getFom(): LocalDate = dagensDato.withDayOfYear(1)
    override fun getTom(): LocalDate? = periode.tom
}
```

**Etter:**
```kotlin
val periode = SimpleErPeriodeAdapter(dagensDato.withDayOfYear(1), periode.tom)
```

### 4. Eksempler fra kodebasen
- `TrygdeavgiftsberegningValidator.kt`: Erstattet anonyme ErPeriode-objekter
- `MuligPeriode.kt`: Bruker SimpleErPeriodeAdapter i konverteringsmetoder
- 20+ andre steder i kodebasen

---

## Konklusjon

Løsningen gir:
1. ✅ **Type-sikkerhet**: Kompilator garanterer at fom er non-null
2. ✅ **Enklere kode**: Mindre boilerplate, tydeligere intensjon
3. ✅ **Database-sikkerhet**: Type-system matcher database-constraints
4. ✅ **Bedre feilhåndtering**: Feil oppdages tidlig ved validering
5. ✅ **Idiomatisk Kotlin**: Properties i stedet for getters
6. ✅ **Tydelig arkitektur**: Klar separasjon mellom transport- og domene-lag
