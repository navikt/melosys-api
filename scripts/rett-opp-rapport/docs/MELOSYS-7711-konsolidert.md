# MELOSYS-7711: Konsolidert dokumentasjon

> Oppdatert: 2025-12-10
> Status: Del 1 ferdig, Del 2 klar for kjøring

---

## 1. Problemet

### 1.1 Rotårsak
`AvsluttArt13BehandlingJobb` kjører 2 måneder etter at en behandling er ferdigstilt og finner behandlinger med status `MIDLERTIDIG_LOVVALGSBESLUTNING`. Jobben setter disse til endelig - **uten å sjekke om behandlingen allerede er annullert**.

### 1.2 To typer feil

| Feil | Beskrivelse |
|------|-------------|
| **Feil 1** | A003 annullert med X008/X006 - perioder som skulle vært avvist ble satt til gyldige |
| **Feil 2** | Ny vurdering overskrevet - oppdaterte perioder ble erstattet med gamle |

### 1.3 Feil 1 - Normal flyt vs. feil flyt

**Normal flyt:**
```
1. A003 mottas fra EESSI → Behandling opprettes
2. MEDL-periode registreres med status GYLD
3. Etter 2 måneder: AvsluttArt13BehandlingJobb kjører
4. MEDL settes til ENDL, saksstatus til LOVVALG_AVKLART
```

**Feil flyt (med X008/X006):**
```
1. A003 mottas → MEDL-periode registreres
2. X008/X006 mottas → Invaliderer A003
   - MEDL settes til AVVI ✓
   - Saksstatus settes til ANNULLERT ✓
   - Behandlingsresultat: HENLEGGELSE ✓
   - MEN: Behandlingsstatus forblir MIDLERTIDIG_LOVVALGSBESLUTNING ← FEIL
3. 2 måneder senere: Jobben kjører
4. Finner behandling med MIDLERTIDIG_LOVVALGSBESLUTNING
5. Setter MEDL til ENDL ← FEIL!
6. Setter saksstatus til LOVVALG_AVKLART ← FEIL!
```

---

## 2. Status per 2025-12-10

### 2.1 Fremdrift

| Del | Beskrivelse | Antall | Status |
|-----|-------------|--------|--------|
| **Del 1** | 1 behandling, 1 A003 | 2,849 | ✅ FERDIG |
| **Del 2a** | Flere behandlinger, siste er REGISTRERT_UNNTAK | 2,228 | Klar for retting |
| **Del 2b** | Flere behandlinger, alle er HENLEGGELSE | 6 | Klar for retting |
| **Del 2 Annet** | Blandet resultat | 13 | Manuell vurdering |
| **UNSAFE** | Flere A003 enn behandlinger | 449 | Manuell vurdering |
| **Ikke invalidert** | Mangler X008/X006 | 23 | Sjekk manuelt |

### 2.2 Totalt gjenværende: 2,719 saker

---

## 3. Kategorisering SAFE vs UNSAFE

### 3.1 Nøkkelregel

> **Antall A003 SEDer > Antall behandlinger = UNSAFE**

Forklaring: En A003 SED skal alltid opprette en behandling. Hvis det er flere A003 enn behandlinger, betyr det at en A003 ikke opprettet behandling (f.eks. kom etter X008).

### 3.2 Kategoriseringslogikk

```
HVIS antall_A003 > antall_behandlinger:
    → UNSAFE (kan IKKE fikses automatisk)

HVIS utfall == "IKKE_INVALIDERT_I_EESSI":
    → IKKE_INVALIDERT (sjekk manuelt)

HVIS antall_behandlinger == 1 OG antall_A003 == 1:
    → DEL1 (enkleste case)

HVIS antall_behandlinger > 1:
    HVIS siste_behandling.resultat == "REGISTRERT_UNNTAK":
        → DEL2A
    HVIS alle_behandlinger.resultat == "HENLEGGELSE":
        → DEL2B
    ELLERS:
        → DEL2_ANNET (manuell vurdering)
```

### 3.3 Hvorfor UNSAFE ikke kan fikses automatisk

**Eksempel - MEL-509129:**
1. A003 nr. 1 mottatt → Behandling opprettet
2. A003 nr. 2 mottatt (kun endret artikkel, samme periode) → INGEN behandling pga. `ArbeidFlereLandSedRuter`
3. X008 mottatt → Ugyldiggjorde A003 nr. 1

Problem: A003 nr. 2 er fortsatt gyldig, så vi kan ikke annullere hele saken.

---

## 4. Rettingsstrategi per kategori

### 4.1 Del 1 (FERDIG)
- **Kriterier:** 1 behandling, 1 A003
- **Aksjon:**
  - Saksstatus: `LOVVALG_AVKLART` → `ANNULLERT`
  - MEDL-periode: `GYLD/ENDL` → `AVVI`

### 4.2 Del 2a (siste behandling er REGISTRERT_UNNTAK)
- **Kriterier:** Flere behandlinger, siste har resultat `REGISTRERT_UNNTAK`
- **Aksjon:**
  - Saksstatus: **IKKE ENDRE** (det finnes en gyldig periode)
  - MEDL kun på HENLEGGELSE-behandlinger: `GYLD/ENDL` → `AVVI`
  - MEDL på REGISTRERT_UNNTAK-behandlinger: **IKKE ENDRE**

### 4.3 Del 2b (alle behandlinger er HENLEGGELSE)
- **Kriterier:** Flere behandlinger, alle har resultat `HENLEGGELSE`
- **Aksjon:**
  - Saksstatus: `LOVVALG_AVKLART` → `ANNULLERT`
  - Alle MEDL-perioder: `GYLD/ENDL` → `AVVI`

### 4.4 UNSAFE
- **Kriterier:** `antall_A003 > antall_behandlinger`
- **Aksjon:** Manuell vurdering - kan IKKE fikses automatisk

### 4.5 Ikke invalidert
- **Kriterier:** Ingen X008/X006 funnet i EESSI
- **Aksjon:** Sjekk manuelt i EESSI

---

## 5. Teknisk referanse

### 5.1 Nøkkelfiler

| Fil | Beskrivelse |
|-----|-------------|
| `service/.../jobb/RettOppFeilMedlPerioderJob.kt` | Hovedjobb for deteksjon og retting |
| `service/.../jobb/RettOppFeilMedlPerioderRepository.kt` | Repository for å finne kandidater |
| `ArbeidFlereLandSedRuter.kt` | Logikk som avgjør om ny behandling opprettes |

### 5.2 SED-typer

| SED | Betydning |
|-----|-----------|
| A003 | Beslutning om lovvalg (artikkel 13) |
| A008 | Melding om endring |
| X006 | Avbryt BUC |
| X008 | Ugyldiggjør SED |

### 5.3 Statuser

**MEDL-statuser:**
| Status | Betydning |
|--------|-----------|
| `GYLD` | Gyldig (midlertidig) |
| `ENDL` | Endelig |
| `AVVI` | Avvist |
| `AVST` | Avstemt (manuelt håndtert) |

**Saksstatuser:**
| Status | Betydning |
|--------|-----------|
| `LOVVALG_AVKLART` | Normalt sluttresultat |
| `ANNULLERT` | Saken er annullert |

**Behandlingsresultater:**
| Resultat | Betydning |
|----------|-----------|
| `HENLEGGELSE` | Behandling henlagt (etter X008/X006) |
| `REGISTRERT_UNNTAK` | Gyldig unntak registrert |

---

## 6. API og kjøring

### 6.1 API-endepunkter

```bash
# Kjøre jobb (dry-run)
curl -X POST "https://melosys-api.../api/admin/rett-opp-feil-medl-perioder/kjor?dryRun=true"

# Hente rapport
curl "https://melosys-api.../api/admin/rett-opp-feil-medl-perioder/rapport"

# Sjekke status
curl "https://melosys-api.../api/admin/rett-opp-feil-medl-perioder/status"
```

### 6.2 Generere HTML-rapport

Etter kjøring av jobben kan du generere en detaljert HTML-rapport for fag:

```bash
cd scripts/rett-opp-rapport

# 1. Hent rapport fra API og lagre til JSON
curl "https://melosys-api.../api/admin/rett-opp-feil-medl-perioder/rapport" > feil1-del2-0-2723.json

# 2. Hent behandlinger per fagsak (SQL eller API)
# CSV-format: saksnummer,gsakSaksnummer,saksstatus,behandlingId,behandlingType,behandlingTema,behandlingStatus,registrertDato,resultat
# Lagre som: feil1-fagsak-behandlinger.csv

# 3. Generer HTML-rapport
python3 generer-rapport.py

# 4. Åpne rapport
open MELOSYS-7711-feil1-del2-detaljert.html
```

**Rapporten viser for hver sak:**
- Kategori (DEL2A, DEL2B, UNSAFE, etc.) med forklaring
- Alle behandlinger på fagsaken med resultat
- MEDL-perioder med nåværende status
- Aksjon som skal utføres (eller hvorfor den ikke kan fikses)

**MEDL-status sjekk:**
Jobben sjekker nå MEDL-status før retting og hopper over perioder som allerede har status `AVST`.
For å få MEDL-status i rapporten må du kjøre en ny dry-run etter kodeendringen.

### 6.3 SQL for behandlinger

```sql
SELECT
    f.saksnummer,
    f.gsak_saksnummer,
    f.status as saksstatus,
    b.id as behandling_id,
    b.beh_type,
    b.beh_tema,
    b.status as behandling_status,
    b.registrert_dato,
    br.resultat_type
FROM fagsak f
JOIN behandling b ON b.saksnummer = f.saksnummer
LEFT JOIN behandlingsresultat br ON br.behandling_id = b.id
WHERE f.saksnummer IN (
    SELECT DISTINCT b2.saksnummer
    FROM behandling b2
    JOIN behandlingsresultat br2 ON br2.behandling_id = b2.id
    WHERE br2.resultat_type = 'HENLEGGELSE'
      AND b2.beh_tema = 'BESLUTNING_LOVVALG_ANNET_LAND'
)
AND f.status = 'LOVVALG_AVKLART'
ORDER BY f.saksnummer, b.registrert_dato;
```

---

## 7. Historikk

| Dato | Hendelse |
|------|----------|
| 2024-12-02 | Møte med Rune, Francois og Mina - plan vedtatt |
| 2024-12-04 | Stikkprøvekontroll av Mina - problematisk case identifisert |
| 2024-12-05 | SAFE/UNSAFE logikk utviklet |
| 2024-12-06 | Del 1 implementert og testet |
| 2024-12-09 | Del 1 ferdig rettet (2,849 saker) |
| 2024-12-10 | Del 2 implementert med MEDL-status sjekk |

---

## 8. Jira-oppgaver

| Oppgave | Beskrivelse | Status |
|---------|-------------|--------|
| MELOSYS-7711 | Hovedoppgave - finne og rette feil | Pågår |
| MELOSYS-7751 | Del 1 - 1 behandling, 1 A003 | Ferdig |
| MELOSYS-7752 | Del 2 - flere behandlinger | Pågår |
| MELOSYS-7768 | Fiks av rotårsaken (AvsluttArt13BehandlingJobb) | Gjenstår |

---

## 9. Læringspunkter

1. **Ikke stol kun på database-data** - EESSI-data kan inneholde informasjon som aldri kom inn i Melosys
2. **Tell ALT** - Ikke filtrer på status når du teller SEDer
3. **Sammenlign på tvers av systemer** - A003 i EESSI vs behandlinger i Melosys
4. **Verifiser med konkrete testcaser** - MEL-509129 var kritisk for å finne feil i logikken
5. **Runtime-sjekker** - Implementer SAFE/UNSAFE sjekk i selve jobben som backup

---

*Sist oppdatert: 2025-12-10*
