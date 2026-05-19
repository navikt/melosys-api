# Fagsak Actors (Aktører)

Each Fagsak has a collection of Aktører representing the parties involved in the case.

## Actor Roles (Aktoersroller)

| Role | Description | Identifier | Max per case |
|------|-------------|------------|--------------|
| `BRUKER` | Citizen/applicant | aktørId (from PDL) | 1 |
| `VIRKSOMHET` | Company/business | orgnr | 1 |
| `ARBEIDSGIVER` | Employer | orgnr | 1 |
| `TRYGDEMYNDIGHET` | Foreign social security authority | institusjonID or land | Multiple |
| `FULLMEKTIG` | Representative with power of attorney | aktørId | Multiple |
| `REPRESENTANT` | Representative | aktørId | Multiple |

## Aktoer Entity

**Location**: `domain/src/main/java/no/nav/melosys/domain/Aktoer.java`

```java
@Entity
public class Aktoer extends RegistreringsInfo {
    private Aktoersroller rolle;
    private String aktoerId;      // For persons (from PDL)
    private String orgnr;         // For organizations
    private String institusjonID; // For EU/EEA authorities (format: "landkode:institusjonskode")
    private Land_iso2 trygdemyndighetLand;  // For treaty authorities

    @OneToMany
    private Set<Fullmakt> fullmakter;  // Powers of attorney
}
```

## Common Operations

### Adding Actors
```kotlin
// Add citizen
fagsak.addAktoer(Aktoer().apply {
    rolle = Aktoersroller.BRUKER
    aktoerId = "2512489212185"
})

// Add employer
fagsak.addAktoer(Aktoer().apply {
    rolle = Aktoersroller.ARBEIDSGIVER
    orgnr = "912345678"
})

// Add EU/EEA authority
fagsak.addAktoer(Aktoer().apply {
    rolle = Aktoersroller.TRYGDEMYNDIGHET
    institusjonID = "SE:FK"  // Swedish Försäkringskassan
})
```

### Retrieving Actors
```kotlin
// Citizen
val bruker: Optional<Aktoer> = fagsak.hentBruker()
val aktørId: String = fagsak.hentBrukersAktørID()  // throws if not found
val aktørId: Optional<String> = fagsak.finnBrukersAktørID()

// Company/Employer
val virksomhet: Aktoer = fagsak.hentVirksomhet()  // throws if not found
val orgnr: Optional<String> = fagsak.finnVirksomhetsOrgnr()
val arbeidsgiver: Optional<Aktoer> = fagsak.hentUnikArbeidsgiver()

// Authorities
val myndigheter: List<Aktoer> = fagsak.hentMyndigheter()
val landkode: String = aktoer.hentMyndighetLandkode()  // parses institusjonID
```

## EU/EEA vs Treaty Authorities

### EU/EEA Authorities
Use `institusjonID` with format `"landkode:institusjonskode"`:
```kotlin
fagsakService.oppdaterMyndigheterForEuEos(fagsak, listOf("SE:FK", "DK:ATP"))
```

### Bilateral Treaty Authorities
Use `trygdemyndighetLand` with Land_iso2 enum:
```kotlin
fagsakService.oppdaterMyndighetForTrygdeavtale(fagsak, Land_iso2.US)
```

## Validation Rules

1. **Single actor per role** (except TRYGDEMYNDIGHET, FULLMEKTIG, REPRESENTANT)
   - Throws: `"Det finnes mer enn en aktør med rollen X for sak Y"`

2. **Identifier requirements**:
   - BRUKER requires `aktoerId`
   - VIRKSOMHET/ARBEIDSGIVER requires `orgnr`
   - TRYGDEMYNDIGHET requires `institusjonID` OR `trygdemyndighetLand`

## Debugging

### Find all actors for a case
```sql
SELECT a.id, a.rolle, a.aktoer_id, a.orgnr, a.institusjon_id, a.trygdemyndighet_land
FROM aktoer a
WHERE a.fagsak_saksnummer = 'MEL-12345';
```

### Find cases by actor
```sql
-- By person
SELECT f.* FROM fagsak f
JOIN aktoer a ON a.fagsak_saksnummer = f.saksnummer
WHERE a.rolle = 'BRUKER' AND a.aktoer_id = '2512489212185';

-- By organization
SELECT f.* FROM fagsak f
JOIN aktoer a ON a.fagsak_saksnummer = f.saksnummer
WHERE a.rolle = 'ARBEIDSGIVER' AND a.orgnr = '912345678';
```

### Find cases with duplicate roles (problematic)
```sql
SELECT fagsak_saksnummer, rolle, COUNT(*) as count
FROM aktoer
WHERE rolle NOT IN ('TRYGDEMYNDIGHET', 'FULLMEKTIG', 'REPRESENTANT')
GROUP BY fagsak_saksnummer, rolle
HAVING COUNT(*) > 1;
```
