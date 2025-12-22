---
name: vedtak
description: |
  Expert knowledge of vedtak (verdict) processing in melosys-api using the saksflyt saga pattern.
  Use when: (1) Debugging vedtak-related issues or failures in IVERKSETT_VEDTAK_* processes,
  (2) Investigating race conditions where vedtak fails due to concurrent operations,
  (3) Understanding how fattVedtak triggers MEDL updates, letters, invoices, and case closure,
  (4) Tracing why vedtak got stuck or failed at specific steps,
  (5) Understanding the relationship between VedtakService classes and Prosessinstans,
  (6) Debugging issues with brev (letters), fakturering (invoicing), or MEDL registration.
---

# Vedtak Saga Pattern

Vedtak (verdict) processing in melosys-api orchestrates multiple async operations through the saksflyt
saga pattern. When `fattVedtak()` is called, it triggers a multi-step process that updates MEDL,
sends letters, creates invoices, and closes the case.

## Quick Reference

### Vedtak Service → Saga Mapping

| Service | ProsessType | Key Steps |
|---------|-------------|-----------|
| `FtrlVedtakService` | `IVERKSETT_VEDTAK_FTRL` | MEDL → Faktura → Close |
| `EosVedtakService` | `IVERKSETT_VEDTAK_EOS` | Myndighet → MEDL → Faktura → Brev → Close |
| `TrygdeavtaleVedtakService` | `IVERKSETT_VEDTAK_TRYGDEAVTALE` | Myndighet → MEDL → Close |
| `IkkeYrkesaktivVedtakService` | `IVERKSETT_VEDTAK_IKKE_YRKESAKTIV` | MEDL → Brev → Close |
| `ÅrsavregningVedtakService` | `IVERKSETT_VEDTAK_AARSAVREGNING` | Faktura → Pensjon → Close |

### Vedtak Lifecycle

```
┌─────────────┐    ┌──────────────────┐    ┌──────────────────┐
│ fattVedtak()│───►│ Behandling       │───►│ Prosessinstans   │
│ (Service)   │    │ IVERKSETTER_     │    │ KLAR → FERDIG    │
│             │    │ VEDTAK           │    │                  │
└─────────────┘    └──────────────────┘    └──────────────────┘
                   Sets behandlingsstatus   Executes async steps
```

### Common Entry Points

```kotlin
// FTRL vedtak
prosessinstansService.opprettProsessinstansIverksettVedtakFTRL(behandling, request, nyStatus)

// EØS vedtak
prosessinstansService.opprettProsessinstansIverksettVedtakEos(behandling)

// Trygdeavtale vedtak
prosessinstansService.opprettProsessinstansIverksettVedtakTrygdeavtale(behandling)
```

## Vedtak Flow Definitions

### IVERKSETT_VEDTAK_FTRL (Folketrygdloven)
```
LAGRE_MEDLEMSKAPSPERIODE_MEDL → OPPRETT_FAKTURASERIE →
AVSLUTT_SAK_OG_BEHANDLING → SEND_MELDING_OM_VEDTAK → RESET_ÅPNE_ÅRSAVREGNINGER
```

### IVERKSETT_VEDTAK_EOS (EU/EØS)
```
AVKLAR_MYNDIGHET → AVKLAR_ARBEIDSGIVER → LAGRE_LOVVALGSPERIODE_MEDL →
OPPRETT_FAKTURASERIE → SEND_VEDTAKSBREV_INNLAND → SEND_VEDTAK_UTLAND →
DISTRIBUER_JOURNALPOST_UTLAND → AVSLUTT_SAK_OG_BEHANDLING → SEND_MELDING_OM_VEDTAK
```

### IVERKSETT_VEDTAK_TRYGDEAVTALE (Bilateral)
```
AVKLAR_MYNDIGHET → AVKLAR_ARBEIDSGIVER → LAGRE_LOVVALGSPERIODE_MEDL →
AVSLUTT_SAK_OG_BEHANDLING → SEND_MELDING_OM_VEDTAK
```

### IVERKSETT_VEDTAK_IKKE_YRKESAKTIV (Non-worker)
```
LAGRE_LOVVALGSPERIODE_MEDL → SEND_VEDTAKSBREV_INNLAND →
AVSLUTT_SAK_OG_BEHANDLING → SEND_MELDING_OM_VEDTAK
```

### IVERKSETT_VEDTAK_AARSAVREGNING (Annual Reconciliation)
```
SEND_FAKTURA_AARSAVREGNING → VARSLE_PENSJONSOPPTJENING →
AVSLUTT_SAK_OG_BEHANDLING → SEND_MELDING_OM_VEDTAK
```

## Key Vedtak Steps Explained

| Step | Description | Common Issues |
|------|-------------|---------------|
| `LAGRE_MEDLEMSKAPSPERIODE_MEDL` | Saves membership period to MEDL register | MEDL API timeout, duplicate periods |
| `LAGRE_LOVVALGSPERIODE_MEDL` | Saves applicable law period to MEDL | MEDL API errors |
| `OPPRETT_FAKTURASERIE` | Creates invoice series in faktureringskomponenten | Duplicate invoice, amount mismatch |
| `SEND_VEDTAKSBREV_INNLAND` | Sends verdict letter via Dokgen/Joark | Letter generation failure |
| `SEND_VEDTAK_UTLAND` | Sends SED to foreign authority via RINA | RINA connection issues |
| `AVSLUTT_SAK_OG_BEHANDLING` | Closes case and treatment | Status already closed |
| `SEND_MELDING_OM_VEDTAK` | Publishes Kafka event about verdict | Behind feature toggle |

## Debugging Vedtak Issues

### Check Existing Vedtak Process

```kotlin
// In FtrlVedtakService - prevents duplicate vedtak
if (prosessinstansService.harVedtakInstans(behandlingID)) {
    throw FunksjonellException("Det finnes allerede en vedtak-prosess for behandling $behandlingID")
}
```

### Find Vedtak Prosessinstans

```sql
-- Find vedtak saga for behandling
SELECT id, type, status, sist_utforte_steg, registrert_dato, endret_dato
FROM prosessinstans
WHERE behandling_id = :behandlingId
AND type LIKE 'IVERKSETT_VEDTAK%'
ORDER BY registrert_dato DESC;
```

### Check Stuck Vedtak Processes

```sql
SELECT id, type, status, sist_utforte_steg, endret_dato
FROM prosessinstans
WHERE type LIKE 'IVERKSETT_VEDTAK%'
AND status = 'UNDER_BEHANDLING'
AND endret_dato < SYSDATE - INTERVAL '1' HOUR;
```

### Check for Duplicate Vedtak Attempts

```sql
SELECT behandling_id, COUNT(*) as count
FROM prosessinstans
WHERE type LIKE 'IVERKSETT_VEDTAK%'
GROUP BY behandling_id
HAVING COUNT(*) > 1;
```

## Race Condition Patterns

### Pattern 1: Concurrent Vedtak Attempt
User clicks "Fatt vedtak" twice quickly:

```
Time    Thread A                    Thread B
────────────────────────────────────────────────────
T1      harVedtakInstans() = false
T2                                  harVedtakInstans() = false
T3      Create prosessinstans
T4                                  Create prosessinstans  ← DUPLICATE!
T5      Commit TX
T6                                  Commit TX
```

**Symptom**: Two IVERKSETT_VEDTAK processes for same behandling.
**Solution**: Use database constraint or pessimistic locking.

### Pattern 2: Behandling Status Race
Vedtak process and manual close overlap:

```
Time    Vedtak Saga                 Saksbehandler
────────────────────────────────────────────────────
T1      Start AVSLUTT_SAK_OG_BEH
T2                                  Click "Avslutt behandling"
T3      Read: status=UNDER_BEH
T4                                  Set: status=AVSLUTTET
T5      Set: status=AVSLUTTET      ← OptimisticLock or overwrite!
```

**Symptom**: Behandling closed without completing all steps.
**Investigation**: Check audit trail for status changes.

## Detailed Documentation

- **[Vedtak Types](references/vedtak-types.md)**: Complete vedtak type reference with service mappings
- **[Race Conditions](references/race-conditions.md)**: Vedtak-specific race conditions and mitigation
- **[Debugging Guide](references/debugging.md)**: SQL queries, log patterns, and investigation steps
