# EOS-Forordning Debugging

## Common Issues

### 1. Wrong Sakstype Selected

**Symptom**: EU/EEA country treated as FTRL or TRYGDEAVTALE

**Cause**: JournalfoeringValidering checks søknadsland

**Debug**:
```sql
SELECT f.saksnummer, f.gsak_saksnummer, f.fagsak_type,
       mo.data
FROM fagsak f
JOIN behandling b ON b.saksnummer = f.saksnummer
JOIN mottatteopplysninger mo ON mo.behandling_id = b.id
WHERE f.saksnummer = :saksnummer;
-- Check søknadsland in the mottatteopplysninger.data JSON
```

**Validation logic**:
```kotlin
// JournalfoeringValidering
if (erEuEllerEøsLand && sakstype != Sakstyper.EU_EOS) {
    throw FunksjonellException("Land $land krever EU/EØS sakstype")
}
```

### 2. Invalid Lovvalgsbestemmelse

**Symptom**: `UnsupportedOperationException: Lovvalgsbestemmelse X støttes ikke`

**Cause**: Using trygdeavtale article for EU_EOS case or vice versa

**Debug**:
```sql
SELECT lp.lovvalg_bestemmelse, f.fagsak_type
FROM lovvalg_periode lp
JOIN behandlingsresultat br ON lp.beh_resultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE b.id = :behandlingId;
```

**Valid combinations**:
- EU_EOS → FO_883_2004_* or FO_987_2009_*
- TRYGDEAVTALE → Country-specific articles (AUS_*, CAN_*, UK_*, etc.)

### 3. EESSI Integration Failure

**Symptom**: SED not sent, BUC not created

**Debug**: BUC/SED documents are NOT stored in the melosys-api database — they live in melosys-eessi. From melosys-api, inspect the SED saga via `prosessinstans` (it links to behandling via `behandling_id`; the current step is the `sist_fullfort_steg` column, and the saga type is `prosess_type`).

```sql
SELECT pi.id, pi.prosess_type, pi.status, pi.sist_fullfort_steg, pi.registrert_dato
FROM prosessinstans pi
WHERE pi.behandling_id = :behandlingId
ORDER BY pi.registrert_dato DESC;
```

### 4. A1 Not Generated

**Symptom**: Vedtak created but no A1 in SAF/Joark

**Check the resultat type**: whether an A1 is produced is decided in code by `Behandlingsresultat.a1Produseres()` (`erInnvilgelse() && !erUtpeking() && harVedtak()`), not by a single column. Inspect the result type and whether a vedtak exists:

```sql
SELECT br.id, br.resultat_type
FROM behandlingsresultat br
WHERE br.behandling_id = :behandlingId;
```

The generated A1 document is journalført in SAF/Joark and is not stored as a row in the melosys-api database (there is no `dokumentproduksjon` table here).

### 5. Wrong Country Determined

**Symptom**: Lovvalgsland is incorrect

**Debug søknad data**:
```sql
SELECT mo.data
FROM mottatteopplysninger mo
WHERE mo.behandling_id = :behandlingId;
-- Check arbeidsland, søknadsland fields in the data JSON
```

**Check behandlingsresultat**:
```sql
SELECT lp.lovvalgsland, lp.lovvalg_bestemmelse
FROM lovvalg_periode lp
JOIN behandlingsresultat br ON lp.beh_resultat_id = br.id
WHERE br.behandling_id = :behandlingId;
```

## SQL Queries

### EU_EOS Cases Overview

```sql
SELECT
    f.gsak_saksnummer,
    f.fagsak_type,
    b.id as behandling_id,
    b.status,
    b.beh_tema as behandlingstema,
    br.resultat_type
FROM fagsak f
JOIN behandling b ON b.saksnummer = f.saksnummer
LEFT JOIN behandlingsresultat br ON br.behandling_id = b.id
WHERE f.fagsak_type = 'EU_EOS'
ORDER BY b.registrert_dato DESC
FETCH FIRST 100 ROWS ONLY;
```

### Lovvalgsperioder with 883/2004 Articles

```sql
SELECT
    f.gsak_saksnummer,
    lp.lovvalg_bestemmelse,
    lp.lovvalgsland,
    lp.fom_dato,
    lp.tom_dato,
    lp.innvilgelse_resultat
FROM lovvalg_periode lp
JOIN behandlingsresultat br ON lp.beh_resultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE f.fagsak_type = 'EU_EOS'
AND lp.lovvalg_bestemmelse LIKE 'FO_883%'
ORDER BY b.registrert_dato DESC
FETCH FIRST 50 ROWS ONLY;
```

### BUC and SED Status

BUC and SED documents are NOT stored in the melosys-api database — they are owned by melosys-eessi (queried there via the `saksrelasjon`/RINA tables). From melosys-api, trace the SED saga via `prosessinstans` instead:

```sql
SELECT pi.id, pi.prosess_type, pi.status, pi.sist_fullfort_steg
FROM prosessinstans pi
JOIN behandling b ON pi.behandling_id = b.id
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE f.fagsak_type = 'EU_EOS'
AND f.saksnummer = :saksnummer
ORDER BY pi.registrert_dato DESC;
```

### Anmodningsperioder / unntak (Art. 16)

Art. 16 exception requests are stored as `anmodningsperiode` rows (there is no `unntaksperiode` table).

```sql
SELECT
    ap.id,
    ap.fom_dato,
    ap.tom_dato,
    ap.lovvalg_bestemmelse,
    ap.lovvalgsland,
    ap.unntak_fra_bestemmelse,
    ap.unntak_fra_lovvalgsland,
    f.gsak_saksnummer
FROM anmodningsperiode ap
JOIN behandlingsresultat br ON ap.beh_resultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE f.fagsak_type = 'EU_EOS'
ORDER BY b.registrert_dato DESC;
```

### Multi-State Cases (Art. 13)

```sql
SELECT
    f.gsak_saksnummer,
    b.beh_tema as behandlingstema,
    lp.lovvalg_bestemmelse,
    lp.lovvalgsland
FROM lovvalg_periode lp
JOIN behandlingsresultat br ON lp.beh_resultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE lp.lovvalg_bestemmelse LIKE 'FO_883_2004_ART13%'
ORDER BY b.registrert_dato DESC;
```

## Article Statistics

### Distribution of Articles Used

```sql
SELECT
    lp.lovvalg_bestemmelse,
    COUNT(*) as antall
FROM lovvalg_periode lp
JOIN behandlingsresultat br ON lp.beh_resultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE f.fagsak_type = 'EU_EOS'
AND b.status = 'AVSLUTTET'
GROUP BY lp.lovvalg_bestemmelse
ORDER BY antall DESC;
```

### Countries Distribution

```sql
SELECT
    lp.lovvalgsland,
    COUNT(*) as antall
FROM lovvalg_periode lp
JOIN behandlingsresultat br ON lp.beh_resultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.saksnummer = f.saksnummer
WHERE f.fagsak_type = 'EU_EOS'
AND b.status = 'AVSLUTTET'
GROUP BY lp.lovvalgsland
ORDER BY antall DESC;
```

## EEA Countries Reference

EU/EEA countries that require EU_EOS sakstype:

| Country | ISO-2 | Member Type |
|---------|-------|-------------|
| Austria | AT | EU |
| Belgium | BE | EU |
| Bulgaria | BG | EU |
| Croatia | HR | EU |
| Cyprus | CY | EU |
| Czechia | CZ | EU |
| Denmark | DK | EU |
| Estonia | EE | EU |
| Finland | FI | EU |
| France | FR | EU |
| Germany | DE | EU |
| Greece | GR | EU |
| Hungary | HU | EU |
| Iceland | IS | EEA |
| Ireland | IE | EU |
| Italy | IT | EU |
| Latvia | LV | EU |
| Liechtenstein | LI | EEA |
| Lithuania | LT | EU |
| Luxembourg | LU | EU |
| Malta | MT | EU |
| Netherlands | NL | EU |
| Poland | PL | EU |
| Portugal | PT | EU |
| Romania | RO | EU |
| Slovakia | SK | EU |
| Slovenia | SI | EU |
| Spain | ES | EU |
| Sweden | SE | EU |
| Switzerland | CH | EFTA |
| United Kingdom | GB | Protocol |

## Feature Toggles

Feature toggles in melosys-api are Unleash toggles referenced via `ToggleName.*` constants and `unleash.isEnabled(...)`. There is no single `EESSI_ENABLED` or `UK_POST_BREXIT` switch. To find the toggles relevant to a flow, search for `ToggleName.` usages in the relevant `saksflyt`/`service` step (e.g. `ToggleName.MELOSYS_11_3_A_NORGE_ER_UTPEKT`).

## Related Skills

- **lovvalg**: Detailed article information
- **eessi-eux**: EESSI integration details
- **sed**: SED document handling
- **kodeverk**: Lovvalgsbestemmelser enums
