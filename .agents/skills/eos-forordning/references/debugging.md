# EOS-Forordning Debugging

## Common Issues

### 1. Wrong Sakstype Selected

**Symptom**: EU/EEA country treated as FTRL or TRYGDEAVTALE

**Cause**: JournalfoeringValidering checks søknadsland

**Debug**:
```sql
SELECT f.id, f.gsak_saksnr, f.sakstype,
       mo.mottatte_opplysninger_data
FROM fagsak f
JOIN behandling b ON b.fagsak_id = f.id
JOIN mottatte_opplysninger mo ON mo.behandling_id = b.id
WHERE f.id = :fagsakId;
-- Check søknadsland in mottatte_opplysninger_data
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
SELECT lp.bestemmelse, f.sakstype
FROM lovvalgsperiode lp
JOIN behandlingsresultat br ON lp.behandlingsresultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE b.id = :behandlingId;
```

**Valid combinations**:
- EU_EOS → FO_883_2004_* or FO_987_2009_*
- TRYGDEAVTALE → Country-specific articles (AUS_*, CAN_*, UK_*, etc.)

### 3. EESSI Integration Failure

**Symptom**: SED not sent, BUC not created

**Debug**:
```sql
SELECT bc.*, sed.*
FROM buc_case bc
LEFT JOIN sed ON sed.buc_case_id = bc.id
WHERE bc.fagsak_id = :fagsakId
ORDER BY bc.opprettet_tid DESC;
```

**Check prosessinstans**:
```sql
SELECT pi.*, ps.steg, ps.status
FROM prosessinstans pi
JOIN prosess_steg ps ON ps.prosessinstans_id = pi.id
WHERE pi.behandling_id = :behandlingId
AND pi.prosesstype LIKE '%SED%'
ORDER BY ps.opprettet_tid;
```

### 4. A1 Not Generated

**Symptom**: Vedtak created but no A1 in SAF/Joark

**Check a1Produseres flag**:
```sql
SELECT br.id, br.type as resultattype,
       CASE WHEN br.type IN ('INNVILGELSE', 'REGISTRERING_UNNTAK')
            THEN 'true' ELSE 'false' END as a1_produseres
FROM behandlingsresultat br
WHERE br.behandling_id = :behandlingId;
```

**Check dokumentproduksjon**:
```sql
SELECT dp.*, b.id as behandling_id
FROM dokumentproduksjon dp
JOIN behandling b ON dp.behandling_id = b.id
WHERE b.id = :behandlingId
AND dp.type = 'A1';
```

### 5. Wrong Country Determined

**Symptom**: Lovvalgsland is incorrect

**Debug søknad data**:
```sql
SELECT mo.mottatte_opplysninger_data
FROM mottatte_opplysninger mo
WHERE mo.behandling_id = :behandlingId;
-- Check arbeidsland, søknadsland fields
```

**Check behandlingsresultat**:
```sql
SELECT lp.lovvalgsland, lp.bestemmelse
FROM lovvalgsperiode lp
JOIN behandlingsresultat br ON lp.behandlingsresultat_id = br.id
WHERE br.behandling_id = :behandlingId;
```

## SQL Queries

### EU_EOS Cases Overview

```sql
SELECT
    f.gsak_saksnr,
    f.sakstype,
    b.id as behandling_id,
    b.status,
    b.tema as behandlingstema,
    br.type as resultattype
FROM fagsak f
JOIN behandling b ON b.fagsak_id = f.id
LEFT JOIN behandlingsresultat br ON br.behandling_id = b.id
WHERE f.sakstype = 'EU_EOS'
ORDER BY b.opprettet_tid DESC
FETCH FIRST 100 ROWS ONLY;
```

### Lovvalgsperioder with 883/2004 Articles

```sql
SELECT
    f.gsak_saksnr,
    lp.bestemmelse,
    lp.lovvalgsland,
    lp.fom,
    lp.tom,
    lp.innvilgelsesresultat
FROM lovvalgsperiode lp
JOIN behandlingsresultat br ON lp.behandlingsresultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE f.sakstype = 'EU_EOS'
AND lp.bestemmelse LIKE 'FO_883%'
ORDER BY b.opprettet_tid DESC
FETCH FIRST 50 ROWS ONLY;
```

### BUC and SED Status

```sql
SELECT
    f.gsak_saksnr,
    bc.type as buc_type,
    bc.status as buc_status,
    bc.rina_id,
    sed.type as sed_type,
    sed.status as sed_status
FROM buc_case bc
JOIN fagsak f ON bc.fagsak_id = f.id
LEFT JOIN sed ON sed.buc_case_id = bc.id
WHERE f.sakstype = 'EU_EOS'
AND f.saksnummer = :saksnummer
ORDER BY bc.opprettet_tid DESC;
```

### Unntaksperioder (Art. 16)

```sql
SELECT
    up.id,
    up.fom,
    up.tom,
    up.unntak_fra_bestemmelse,
    up.unntak_fra_lovvalgsland,
    up.innvilgelsesresultat,
    f.gsak_saksnr
FROM unntaksperiode up
JOIN behandlingsresultat br ON up.behandlingsresultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE f.sakstype = 'EU_EOS'
ORDER BY b.opprettet_tid DESC;
```

### Multi-State Cases (Art. 13)

```sql
SELECT
    f.gsak_saksnr,
    b.tema as behandlingstema,
    lp.bestemmelse,
    lp.lovvalgsland
FROM lovvalgsperiode lp
JOIN behandlingsresultat br ON lp.behandlingsresultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE lp.bestemmelse LIKE 'FO_883_2004_ART13%'
ORDER BY b.opprettet_tid DESC;
```

## Article Statistics

### Distribution of Articles Used

```sql
SELECT
    lp.bestemmelse,
    COUNT(*) as antall
FROM lovvalgsperiode lp
JOIN behandlingsresultat br ON lp.behandlingsresultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE f.sakstype = 'EU_EOS'
AND b.status = 'AVSLUTTET'
GROUP BY lp.bestemmelse
ORDER BY antall DESC;
```

### Countries Distribution

```sql
SELECT
    lp.lovvalgsland,
    COUNT(*) as antall
FROM lovvalgsperiode lp
JOIN behandlingsresultat br ON lp.behandlingsresultat_id = br.id
JOIN behandling b ON br.behandling_id = b.id
JOIN fagsak f ON b.fagsak_id = f.id
WHERE f.sakstype = 'EU_EOS'
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

| Toggle | Purpose |
|--------|---------|
| `EESSI_ENABLED` | Enable EESSI/EUX integration |
| `UK_POST_BREXIT` | Use UK protocol instead of 883/2004 |

## Related Skills

- **lovvalg**: Detailed article information
- **eessi-eux**: EESSI integration details
- **sed**: SED document handling
- **kodeverk**: Lovvalgsbestemmelser enums
