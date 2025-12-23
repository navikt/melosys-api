---
skill: vilkaar
description: Expert knowledge of vilkårsvurdering (requirements evaluation) for FTRL bestemmelser
triggers:
  - vilkår
  - vilkaar
  - inngangsvilkår
  - avklarte fakta
  - avklartefakta
  - begrunnelse
  - oppfylt
  - requirements
  - conditions
  - eligibility
references:
  - references/structure.md
  - references/avklartefakta.md
  - references/evaluation.md
  - references/debugging.md
---

# Vilkaar Skill

## Quick Reference

### What is Vilkår?

Vilkår are requirements/conditions that must be evaluated to determine if a person qualifies for a specific FTRL bestemmelse (legal provision). Each bestemmelse has its own set of vilkår that must be fulfilled (oppfylt).

### Domain Model

```
Behandling
    └── Behandlingsresultat
            └── Vilkaarsresultat (per vilkår)
                    ├── vilkaar: Vilkaar (enum)
                    ├── oppfylt: Boolean
                    ├── begrunnelser: Set<String>
                    └── begrunnelseFritekst: String?
```

### Key Components

| Component | Description | Location |
|-----------|-------------|----------|
| `VilkårForBestemmelse` | Routes to theme-specific vilkår | service/.../ftrl/bestemmelse/vilkaar/ |
| `Vilkår` | Data class holding vilkår definition | service/.../ftrl/bestemmelse/vilkaar/Vilkår.kt |
| `Vilkaarsresultat` | Entity storing evaluation result | domain/.../Vilkaarsresultat.java |
| `AvklarteFaktaForBestemmelse` | Determines required facts | service/.../ftrl/bestemmelse/avklartefakta/ |
| `VilkaarsvurderingService` | Handles vilkår operations | service/.../vilkaar/VilkaarsvurderingService.java |

### Vilkår by Behandlingstema

**YRKESAKTIV** (employed abroad):
- `NORSK_STATSBORGER` - Norwegian citizen
- `ANNEN_STATSBORGER` - Other citizenship
- `TIDLIGERE_MEDLEM` - Previous member
- `ARBEID_FOR_NORSK_ARBEIDSGIVER` - Work for Norwegian employer
- `OPPTJENINGSAAR_PENSJON` - Pension earning years
- Additional vilkår per specific bestemmelse

**IKKE_YRKESAKTIV** (not employed):
- `NORSK_STATSBORGER`
- `ANNEN_STATSBORGER`
- `TIDLIGERE_MEDLEM`
- `FORSØRGET_AV_MEDLEM` - Supported by member
- `OPPTJENINGSAAR_PENSJON`

**PENSJONIST** (pensioner):
- `NORSK_STATSBORGER`
- `ANNEN_STATSBORGER`
- `TIDLIGERE_MEDLEM`
- `MOTTOK_PENSJON_FØR_1994` - Received pension before 1994

### Vilkår Evaluation Flow

```
1. Saksbehandler selects bestemmelse
           │
           ▼
2. VilkårForBestemmelse.hentVilkår(bestemmelse, behandlingstema)
           │
           ▼
3. System shows required vilkår in UI
           │
           ▼
4. Saksbehandler marks each vilkår as oppfylt/ikke oppfylt
           │
           ▼
5. VilkaarsvurderingService stores results
           │
           ▼
6. Vilkaarsresultat entities created on Behandlingsresultat
```

### Common Vilkår Enum Values

```kotlin
enum class Vilkaar {
    NORSK_STATSBORGER,
    ANNEN_STATSBORGER,
    TIDLIGERE_MEDLEM,
    ARBEID_FOR_NORSK_ARBEIDSGIVER,
    ARBEID_I_ANNEN_STAT,
    FORSØRGET_AV_MEDLEM,
    OPPTJENINGSAAR_PENSJON,
    MOTTOK_PENSJON_FØR_1994,
    ARBEID_FOR_NORSK_UTENRIKSTJENESTE,
    ARBEID_SOM_MISJONÆR,
    ARBEID_SOM_AU_PAIR,
    STUDERER_I_UTLANDET,
    // ... more
}
```

### AvklarteFakta

AvklarteFakta are pre-collected facts used by saksbehandler when evaluating vilkår:

```kotlin
enum class AvklartFakta {
    ARBEIDSFORHOLD,        // Employment relationships
    STATSBORGERSKAP,       // Citizenship
    PENSJON,              // Pension info
    INNTEKT,              // Income
    MEDLEMSKAPSHISTORIKK, // Membership history
    // ... more
}
```

### Quick Debugging

```sql
-- Check vilkår results for a behandling
SELECT vr.id, vr.vilkaar, vr.oppfylt, vr.begrunnelse_fritekst
FROM vilkaarsresultat vr
JOIN behandlingsresultat br ON vr.behandlingsresultat_id = br.id
WHERE br.behandling_id = :behandlingId;

-- Missing vilkår (bestemmelse set but no vilkår evaluated)
SELECT b.id, br.bestemmelse
FROM behandling b
JOIN behandlingsresultat br ON br.behandling_id = b.id
WHERE br.bestemmelse IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM vilkaarsresultat vr
    WHERE vr.behandlingsresultat_id = br.id
);
```

## When to Use This Skill

- Understanding which vilkår apply to a bestemmelse
- Debugging why a bestemmelse cannot be selected
- Adding new vilkår to a bestemmelse
- Understanding avklartefakta collection
- Investigating vilkårsvurdering validation errors

## Related Skills

- **ftrl**: FTRL bestemmelser that have vilkår
- **medlemskap**: Membership periods require fulfilled vilkår
- **behandling**: Treatment lifecycle including vilkår evaluation
- **behandlingsresultat**: Results include vilkår outcomes
