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
%%{init: {'theme':'base', 'themeVariables': {
  'primaryColor':'#fbbf24',
  'primaryTextColor':'#000000',
  'primaryBorderColor':'#f59e0b',
  'lineColor':'#9ca3af',
  'secondaryColor':'#34d399',
  'tertiaryColor':'#60a5fa',
  'fontSize':'16px',
  'fontFamily':'system-ui',
  'clusterBkg':'transparent',
  'clusterBorder':'#6b7280',
  'edgeLabelBackground':'#374151',
  'mainBkg':'transparent',
  'nodeBorder':'#374151',
  'titleColor':'#e5e7eb',
  'textColor':'#e5e7eb'
}}}%%

graph LR
    subgraph External["🌐 External Layer"]
        direction TB
        JSON["External APIs<br/><small>JSON/DTOs</small>"]
        UI["Frontend<br/><small>User Interface</small>"]
    end

    subgraph Transport["📦 Transport Layer"]
        direction TB
        MP["<b>MuligPeriode</b><br/>fom: LocalDate?<br/>tom: LocalDate?<br/><small><i>Nullable for flexibility</i></small>"]
    end

    subgraph Domain["⚙️ Domain Layer"]
        direction TB
        EP["<b>ErPeriode</b><br/>fom: LocalDate ✓<br/>tom: LocalDate?<br/><small><i>fom guaranteed non-null</i></small>"]
        Entities["<b>Domain Entities</b><br/>• Medlemskapsperiode<br/>• Lovvalgsperiode"]
    end

    subgraph Persistence["💾 Persistence Layer"]
        direction TB
        DB[("Database<br/><small>fom: NOT NULL</small><br/><small>tom: NULLABLE</small>")]
    end

    JSON -->|"inbound"| MP
    UI -->|"request"| MP
    MP -->|"tilErPeriode()"| EP
    MP -.->|"hentErPeriode()"| EP
    EP -->|"maps to"| Entities
    Entities -->|"persists"| DB
    EP -->|"outbound"| Integration["Integration Layer<br/><small>JSON to external parties</small>"]

    classDef externalStyle fill:#fbbf24,stroke:#f59e0b,stroke-width:2px,color:#000000
    classDef transportStyle fill:#fbbf24,stroke:#f59e0b,stroke-width:3px,color:#000000
    classDef domainStyle fill:#34d399,stroke:#10b981,stroke-width:3px,color:#000000
    classDef persistenceStyle fill:#60a5fa,stroke:#3b82f6,stroke-width:3px,color:#000000

    class JSON,UI externalStyle
    class MP transportStyle
    class EP,Entities domainStyle
    class DB,Integration persistenceStyle
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
%%{init: {'theme':'base', 'themeVariables': {
  'primaryColor':'#34d399',
  'primaryTextColor':'#000000',
  'primaryBorderColor':'#10b981',
  'lineColor':'#9ca3af',
  'secondaryColor':'#86efac',
  'tertiaryColor':'#60a5fa',
  'fontSize':'16px',
  'fontFamily':'system-ui',
  'clusterBkg':'transparent',
  'clusterBorder':'#6b7280',
  'edgeLabelBackground':'#374151',
  'mainBkg':'transparent',
  'nodeBorder':'#374151',
  'titleColor':'#e5e7eb',
  'textColor':'#e5e7eb'
}}}%%

graph TB
    subgraph Solution["💡 Solution"]
        SEA["<b>SimpleErPeriodeAdapter</b><br/><br/>class SimpleErPeriodeAdapter<br/>override var fom: LocalDate<br/>override var tom: LocalDate?<br/><br/><small><i>Implements ErPeriode interface</i></small>"]
    end

    subgraph Usage["🔧 Usage Examples"]
        U1["<b>Quick Construction</b><br/>SimpleErPeriodeAdapter<br/>dagensDato, periode.tom"]
        U2["<b>Conversion Helper</b><br/>MuligPeriode.tilErPeriode<br/>→ returns SimpleErPeriodeAdapter"]
        U3["<b>Refactoring Win</b><br/>Replace 20+ anonymous objects<br/>with SimpleErPeriodeAdapter"]
    end

    subgraph Benefits["✨ Benefits"]
        B1["🔒 Type-safe"]
        B2["♻️ Reusable"]
        B3["🚀 No boilerplate"]
        B4["🎯 Idiomatic Kotlin"]
    end

    SEA -->|"enables"| U1
    SEA -->|"enables"| U2
    SEA -->|"enables"| U3

    U1 --> B1
    U2 --> B2
    U3 --> B3
    U1 --> B4
    U2 --> B4
    U3 --> B4

    classDef solutionClass fill:#34d399,stroke:#10b981,stroke-width:4px,color:#000000
    classDef usageClass fill:#86efac,stroke:#22c55e,stroke-width:2px,color:#000000
    classDef benefitClass fill:#60a5fa,stroke:#3b82f6,stroke-width:2px,color:#000000

    class SEA solutionClass
    class U1,U2,U3 usageClass
    class B1,B2,B3,B4 benefitClass
```

**Bruksområder:**
1. Direkte instansiering når man trenger en ErPeriode
2. Intern implementasjon i `MuligPeriode.tilErPeriode()`
3. Erstatter alle anonyme ErPeriode-objekter i kodebasen

---

## 4. Conversion Flow

```mermaid
%%{init: {'theme':'base', 'themeVariables': {
  'primaryColor':'#fbbf24',
  'primaryTextColor':'#000000',
  'primaryBorderColor':'#f59e0b',
  'lineColor':'#9ca3af',
  'secondaryColor':'#34d399',
  'tertiaryColor':'#60a5fa',
  'fontSize':'16px',
  'fontFamily':'system-ui',
  'clusterBkg':'transparent',
  'clusterBorder':'#6b7280',
  'edgeLabelBackground':'#374151',
  'mainBkg':'transparent',
  'nodeBorder':'#374151',
  'titleColor':'#e5e7eb',
  'textColor':'#e5e7eb'
}}}%%

flowchart LR
    subgraph Input["📥 Input Layer"]
        JSON["<b>JSON from API</b><br/>fom: 2024-01-01<br/>tom: null"]
        DTO["<b>DTO Object</b><br/><small>nullable dates</small>"]
    end

    subgraph Validation["✅ Validation Layer"]
        MP["<b>MuligPeriode</b><br/>fom: LocalDate?<br/>tom: LocalDate?"]
        V{"<b>fom != null?</b>"}
    end

    subgraph Domain["⚙️ Domain Layer"]
        SEA["<b>SimpleErPeriodeAdapter</b><br/>fom: LocalDate ✓<br/>tom: LocalDate?"]
        EP["<b>ErPeriode</b><br/><small><i>Type-safe guarantee</i></small>"]
    end

    subgraph Persistence["💾 Persistence Layer"]
        ENT["<b>Domain Entity</b><br/>Medlemskapsperiode"]
        DB[("<b>Database</b><br/><small>fom: NOT NULL</small><br/><small>tom: NULLABLE</small>")]
    end

    JSON --> DTO
    DTO --> MP
    MP --> V
    V -->|"✓ Yes"| SEA
    V -->|"✗ No"| ERR["<b>⚠️ Error</b><br/>fom required"]
    SEA --> EP
    EP --> ENT
    ENT --> DB

    classDef inputClass fill:#fbbf24,stroke:#f59e0b,stroke-width:2px,color:#000000
    classDef validationClass fill:#fbbf24,stroke:#f59e0b,stroke-width:2px,color:#000000
    classDef domainClass fill:#34d399,stroke:#10b981,stroke-width:3px,color:#000000
    classDef dbClass fill:#60a5fa,stroke:#3b82f6,stroke-width:2px,color:#000000
    classDef errorClass fill:#fca5a5,stroke:#ef4444,stroke-width:3px,color:#000000

    class JSON,DTO,MP inputClass
    class V validationClass
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
%%{init: {'theme':'base', 'themeVariables': {
  'primaryColor':'#34d399',
  'primaryTextColor':'#000000',
  'primaryBorderColor':'#10b981',
  'lineColor':'#9ca3af',
  'secondaryColor':'#60a5fa',
  'tertiaryColor':'#86efac',
  'fontSize':'16px',
  'fontFamily':'system-ui',
  'clusterBkg':'transparent',
  'clusterBorder':'#6b7280',
  'edgeLabelBackground':'#374151',
  'mainBkg':'transparent',
  'nodeBorder':'#374151',
  'titleColor':'#e5e7eb',
  'textColor':'#e5e7eb'
}}}%%

graph TB
    subgraph TypeSystem["🔒 Type System Guarantees"]
        direction TB
        EP["<b>ErPeriode interface</b>"]
        FOM["<b>fom: LocalDate</b><br/><small>✓ Non-null guaranteed</small>"]
        TOM["<b>tom: LocalDate?</b><br/><small>? Nullable explicit</small>"]
        EP --> FOM
        EP --> TOM
    end

    subgraph CompileTime["⚡ Compile-Time Safety"]
        C1["<b>No NullPointerException</b><br/>on fom access"]
        C2["<b>Database constraints</b><br/>enforced by types"]
        C3["<b>Clear intent</b><br/>ErPeriode = valid period"]
    end

    subgraph Runtime["🚀 Runtime Benefits"]
        R1["<b>Earlier error detection</b><br/><small>Fail fast at boundaries</small>"]
        R2["<b>No defensive checks</b><br/><small>fom is always safe</small>"]
        R3["<b>Simplified logic</b><br/><small>Focus on business rules</small>"]
    end

    FOM -->|"ensures"| C1
    TOM -->|"aligns with"| C2
    EP -->|"communicates"| C3

    C1 -->|"enables"| R1
    C2 -->|"enables"| R2
    C3 -->|"enables"| R3

    classDef typeClass fill:#34d399,stroke:#10b981,stroke-width:3px,color:#000000
    classDef compileClass fill:#60a5fa,stroke:#3b82f6,stroke-width:2px,color:#000000
    classDef runtimeClass fill:#86efac,stroke:#22c55e,stroke-width:2px,color:#000000

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
