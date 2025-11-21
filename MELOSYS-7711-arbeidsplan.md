# MELOSYS-7711: Rette opp saker berørt av feil oppdatering av MEDL-periode

## Bakgrunn

### Relaterte Jira-saker
- **MELOSYS-7711**: Finne og rette opp i saker berørt av feil oppdatering av MEDL-periode
- **MELOSYS-7668**: Feil håndtering av X008 fører til feil i saks- og beh.status og MEDL-periode

## Problembeskrivelse

### Root Cause
`AvsluttArt13BehandlingJobb` (cron job) gjør følgende feil:
- Endrer IKKE på behandlingsresultat når det er satt til `HENLEGGELSE` (fra X006 eller X008)
- Setter saksstatus til `LOVVALG_AVKLART` fra `ANNULLERT`
- Gjør MEDL-perioder til "endelig" som burde vært avvist/ugyldig

### Scenario 1: Annullerte A003-behandlinger blir gjort gyldige igjen
**Normalt forløp ved X008:**
1. A003 behandles → behandlingsresultat: "Midlertidig lovvalgsbeslutning" (artikkel 13)
2. X008 mottas (ugyldiggjør A003)
   - MEDL-periode → avvist
   - Saksstatus → `ANNULLERT`
   - Behandlingsresultat → `HENLEGGELSE`
   - Behandlingsstatus → **forblir** `MIDLERTIDIG_LOVVALGSBESLUTNING` ❌ (burde vært `AVSLUTTET`)

**Etter 2 måneder:**
- `AvsluttArt13BehandlingJobb` kjører
- Finner behandlinger med status `MIDLERTIDIG_LOVVALGSBESLUTNING`
- Setter behandlingsstatus → `AVSLUTTET`
- Setter MEDL-periode → endelig
- Setter saksstatus → `LOVVALG_AVKLART` ❌

**Resultat:** Avvist MEDL-periode blir plutselig gyldig igjen!

### Scenario 2: Ny vurdering overskriver oppdatert periode
**Normalt forløp:**
1. A003 behandles → "Midlertidig lovvalgsbeslutning"
2. Ny vurdering startes (f.eks. pga A008)
3. Periode endres og godkjennes → ny MEDL-periode
4. Den første behandlingen forblir med status `MIDLERTIDIG_LOVVALGSBESLUTNING` ❌

**Etter 2 måneder:**
- `AvsluttArt13BehandlingJobb` kjører
- Gjør den **første** (gamle) perioden "endelig"
- Den **oppdaterte** perioden blir erstattet med den **gamle** perioden ❌

**Resultat:** Feil periode i MEDL!

## Konsekvens
- Konsumenter av MEDL får feil informasjon om unntaksperioder
- Feil saksstatus i MEDL
- Muligens lagret lovvalgsperioder som ikke er riktige

## SQL-spørring for å finne berørte saker

```sql
SELECT 
    f.SAKSNUMMER, 
    f.STATUS as FAGSAK_STATUS, 
    b.ID as BEHANDLING_ID, 
    b.BEH_TYPE as BEHANDLING_TYPE, 
    b.STATUS as BEHANDLING_STATUS, 
    b.BEH_TEMA, 
    b.REGISTRERT_DATO, 
    b.ENDRET_DATO 
FROM FAGSAK f 
JOIN BEHANDLING b ON b.SAKSNUMMER = f.SAKSNUMMER 
JOIN BEHANDLINGSRESULTAT br ON br.BEHANDLING_ID = b.ID 
WHERE f.STATUS = 'LOVVALG_AVKLART' 
  AND br.RESULTAT_TYPE = 'HENLEGGELSE' 
  AND b.BEH_TEMA = 'BESLUTNING_LOVVALG_ANNET_LAND' 
ORDER BY b.ENDRET_DATO DESC;
```

### Fellesnevnere for berørte saker

**A003 som har blitt ugyldiggjort:**
- Beh.tema: `BESLUTNING_LOVVALG_ANNET_LAND`
- Mottatt X008 eller X006
- Behandlingsresultat: `HENLEGGELSE`
- Saksstatus: `LOVVALG_AVKLART` (burde vært `ANNULLERT`)

**A003 hvor det er gjort ny vurdering:**
- Beh.tema: `BESLUTNING_LOVVALG_ANNET_LAND` eller `BESLUTNING_NORSK_LOVVALG`
- Minst én behandling type "Førstegangsbehandling" som er godkjent
- Minst én behandling type "Ny vurdering" som er godkjent
- Første behandling har fortsatt status som gjør at cron job kan behandle den

## Teknisk løsning

### Implementasjonsmønster
- Bruk **JobMonitor** for å lage en asynkron job
- Følg mønsteret fra `FinnSakerForÅrsavregning` som eksempel
- **Må først kjøre dry run** for å finne problemer

### Implementasjonsplan

#### 1. Opprett asynkron job med JobMonitor
Filnavn: `RettOppFeilOppdaterteMedlPerioderJob.kt`

**Job skal:**
- Finne alle berørte saker basert på SQL-kriterier
- Logge antall saker som må fikses
- I dry-run modus: kun rapportere hva som ville blitt endret
- I prod-modus: utføre rettinger

#### 2. Repository-metode
Opprett metode for å finne berørte saker:
```kotlin
interface BehandlingRepository : JpaRepository<Behandling, Long> {
    @Query("""
        SELECT b FROM Behandling b
        JOIN b.fagsak f
        JOIN b.behandlingsresultat br
        WHERE f.status = 'LOVVALG_AVKLART'
        AND br.resultatType = 'HENLEGGELSE'
        AND b.behandlingsTema = 'BESLUTNING_LOVVALG_ANNET_LAND'
        ORDER BY b.endretDato DESC
    """)
    fun finnBehandlingerMedFeilOppdaterteMedlPerioder(): List<Behandling>
}
```

#### 3. Rettingslogikk
For hver berørt sak:
1. Verifiser at det faktisk er en feil (sjekk X008/X006 mottak)
2. Hvis behandlingen skulle vært annullert:
   - Sett saksstatus tilbake til `ANNULLERT`
   - Sett MEDL-periode til avvist
3. Hvis det er gjort ny vurdering:
   - Finn nyeste gyldige behandling
   - Bruk periode fra denne behandlingen
   - Sett gamle behandlinger til riktig status

#### 4. Testing
- Skriv unit tester for logikken
- Test dry-run på prod-data (via kopi)
- Verifiser at antall saker stemmer
- Manuell verifikasjon av noen tilfeller før prod-kjøring

#### 5. Deployment
1. Deploy med dry-run aktivert
2. Kjør job i prod og analyser resultater
3. Manuell verifikasjon av utvalgte saker
4. Kjør med faktisk retting
5. Verifiser at MEDL-perioder er riktige

## Akseptansekriterier

- [ ] Job finner alle berørte saker basert på SQL-kriterier
- [ ] Dry-run kjører uten å endre data
- [ ] Dry-run logger alle saker som ville blitt endret
- [ ] Prod-kjøring retter opp saksstatus for annullerte saker
- [ ] Prod-kjøring retter opp MEDL-perioder
- [ ] Ingen saker med `LOVVALG_AVKLART` + `HENLEGGELSE` + `BESLUTNING_LOVVALG_ANNET_LAND`
- [ ] Verifisert at konsumenter av MEDL får riktig informasjon

## Datamodell (antatt)

```kotlin
// Behandling
data class Behandling(
    val id: Long,
    val saksnummer: String,
    val behType: BehandlingType,
    val status: BehandlingStatus,
    val behTema: BehandlingsTema,
    val registrertDato: LocalDateTime,
    val endretDato: LocalDateTime
)

enum class BehandlingStatus {
    MIDLERTIDIG_LOVVALGSBESLUTNING,
    AVSLUTTET,
    // ...
}

enum class BehandlingsTema {
    BESLUTNING_LOVVALG_ANNET_LAND,
    BESLUTNING_NORSK_LOVVALG,
    // ...
}

// Fagsak
enum class SaksStatus {
    LOVVALG_AVKLART,
    ANNULLERT,
    // ...
}

// Behandlingsresultat
enum class ResultatType {
    HENLEGGELSE,
    MIDLERTIDIG_LOVVALGSBESLUTNING,
    // ...
}
```

## Notater
- Feilen kommer fra `AvsluttArt13BehandlingJobb` som ikke endrer behandlingsresultat
- `b.STATUS = 'AVSLUTTET'` i SQL er egentlig ikke nødvendig
- Må være forsiktig med å ikke introdusere nye feil mens vi retter opp gamle

---

## Implementasjonsanalyse (Claude Code)

### Observasjoner fra kodebasen

**Fix allerede på plass i `AvsluttArt13BehandlingService`:**
- `erFagsakStatusGyldigForAutomatiskAvslutting()` sjekker at kun `OPPRETTET` status tillates
- `finnesNyereRelevantLovvalgBehandling()` hindrer at gamle perioder overskriver nye

**Mønster å følge (fra `FinnSakerForÅrsavregning`):**
1. Bruk `JobMonitor<T>` for statistikk og feilhåndtering
2. `@Async("taskExecutor")` for asynkron kjøring
3. `@Synchronized` for thread safety
4. Inner class `JobStatus` som implementerer `JobMonitor.Stats`
5. Repository med custom `@Query` for å finne saker
6. Controller for å trigge jobben og hente status

### Implementasjonsstruktur

```
service/src/main/kotlin/no/nav/melosys/service/behandling/jobb/
├── RettOppFeilMedlPerioderJob.kt          # Hovedjobb med JobMonitor
├── RettOppFeilMedlPerioderController.kt   # REST API
└── RettOppFeilMedlPerioderRepository.kt   # Custom queries (kan være i eksisterende repo)
```

### Detaljert flyt

1. **Finn berørte behandlinger** via SQL:
   - `fagsak.status = LOVVALG_AVKLART`
   - `behandlingsresultat.type = HENLEGGELSE`
   - `behandling.tema = BESLUTNING_LOVVALG_ANNET_LAND`

2. **For hver berørt behandling:**
   - Sjekk om det har kommet X008/X006 (behandlingsresultat = HENLEGGELSE bekrefter dette)
   - **Scenario 1 (annullert):** Sett saksstatus tilbake til `ANNULLERT`, avvis MEDL-periode
   - **Scenario 2 (ny vurdering):** Finn nyeste gyldige behandling, ikke gjør noe med gammel

3. **Dry-run vs prod-modus:**
   - Dry-run: Kun logg hva som ville blitt endret
   - Prod: Utfør endringer

### Neste steg
- [ ] Implementer `RettOppFeilMedlPerioderJob.kt`
- [ ] Implementer repository-metode
- [ ] Implementer controller
- [ ] Skriv tester
