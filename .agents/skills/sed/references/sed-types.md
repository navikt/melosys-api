# SED Types Reference

Complete reference for SED (Structured Electronic Document) types.

## A-Series (Applicable Legislation)

EU Regulation 883/2004 applicable legislation documents.

| SED | Name | Purpose | Direction |
|-----|------|---------|-----------|
| `A001` | Request for exception (Søknad/anmodning om unntak) | Start exception process (Art. 16), opens LA_BUC_01 | Outgoing |
| `A002` | Request for information | Ask for additional info | Both |
| `A003` | Decision on applicable legislation (A1) | A1 certificate / decision | Outgoing |
| `A004` | Refusal of A003 decision (Avslag) | Refuse a decision (LA_BUC_02) | Outgoing |
| `A005` | Reply to request | Response to A002 | Both |
| `A006` | Notification of termination | End of posting period | Both |
| `A007` | Notification of changes | Change in circumstances | Both |
| `A008` | Reply to notification | Response to A006/A007 | Both |
| `A009` | Posting notification (Melding om utstasjonering, Art. 12) | Notify about posting (LA_BUC_04) | Outgoing |
| `A010` | Additional information | Supplement to previous SED | Both |
| `A011` | Approval of exception request (Innvilgelse av søknad om unntak) | Positive response to A001 in LA_BUC_01 | Both |
| `A012` | Change to decision (Endringsmelding) | Notify of changes to a sent A003 | Both |

### When to Use

**A001 - Exception Request (Art. 16)**:
- Worker needs exception from normal rules
- Sent to country that would normally be competent
- Starts the LA_BUC_01 (søknad om unntak) workflow

**A003 - A1 Decision**:
- Confirms applicable legislation
- Core decision document
- Used for posted workers, multi-state workers

**A004 - Refusal of A003**:
- Counterpart's refusal of an A003 decision (LA_BUC_02)
- Must include reason
- (The refusal of an A001 *exception* request is A002, not A004)

**A011 - Approval of exception request**:
- Positive response to A001 in LA_BUC_01
- Confirms the exception agreement (beslutning = innvilgelse)

## H-Series (Healthcare)

Healthcare-coordination documents under EU Regulation 883/2004. These exist in
the `SedType` enum but melosys-api does **not** drive an H-series healthcare flow
(it is lovvalg-focused; H_BUC handling lives largely in melosys-eessi). The enum
members are H001-H006, H010, H011, H012, H020, H061, H065, H066, H070, H120,
H121, H130 (note: no H021 or H062).

| SED | Notes |
|-----|-------|
| `H001` | Healthcare-coordination document |
| `H002` | Healthcare-coordination document |
| `H003` | Healthcare-coordination document (S1/E106-related) |
| `H004`–`H006` | Healthcare-coordination documents |
| `H010`, `H011`, `H012` | Healthcare-coordination documents |
| `H020` | Insurance-period statement |
| `H061`, `H065`, `H066`, `H070` | Healthcare-coordination documents |
| `H120`, `H121` | Healthcare cost claim / reply |
| `H130` | Healthcare-coordination document |

## X-Series (Administrative)

System and administrative documents.

| SED | Name | Purpose |
|-----|------|---------|
| `X001` | Request | General request |
| `X002` | Reply to request | Response to X001 |
| `X003` | Recovery request | Start recovery process |
| `X004` | Recovery information | Recovery details |
| `X005` | Notification | General notification |
| `X006` | Reply to notification | Response to X005 |
| `X007` | Clarification | Request clarification |
| `X008` | Invalidation | Cancel/invalidate previous SED |
| `X009` | Reminder | Follow-up reminder |
| `X010` | Additional information | Supplementary info |
| `X011` | Rejection | Reject request/claim |
| `X012` | Reply to rejection | Response to X011 |
| `X013` | Administrative | (in enum) |
| `X050` | Institution info | Institution details |
| `X100` | Administrative | (in enum) |

### X008 - Invalidation

Used to cancel/correct previous SEDs. In EessiService this happens internally
(e.g. when sending a new vurdering) via the private `sendInvalideringSed`, which
calls `eessiClient.sendSedPåEksisterendeBuc(sedDataDto, rinaSaksnummer, SedType.X008)`
— `sendSedPåEksisterendeBuc` lives on `EessiClient`, not `EessiService`.

## S-Series

`S040` and `S041` are present in the `SedType` enum. They are not part of the
lovvalg flow that melosys-api drives.

## SED Type Enum

**Location**: `domain/src/main/kotlin/no/nav/melosys/domain/eessi/SedType.kt`

```kotlin
enum class SedType {
    X001, X002, X003, X004, X005, X006, X007, X008, X009, X010, X011, X012, X013, X050, X100,
    A001, A002, A003, A004, A005, A006, A007, A008, A009, A010, A011, A012,
    H001, H002, H003, H004, H005, H006, H010, H011, H012, H020,
    H061, H065, H066, H070, H120, H121, H130,
    S040, S041;

    fun erPurring(): Boolean = this == X009
}
```

## SED → Behandlingstema Mapping

Only A001/A003/A009/A010 map to a behandlingstema; every other SED type returns
`Optional.empty()`. There is no `Behandlingstema.LOVVALG` or `Behandlingstema.HELSE`.

```java
// SedTypeTilBehandlingstemaMapper.finnBehandlingstemaForSedType(sedType, lovvalgsland)
A001 → Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
A003 → BESLUTNING_LOVVALG_NORGE | BESLUTNING_LOVVALG_ANNET_LAND  // depends on lovvalgsland
A009 → Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING
A010 → Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE
// all other SED types → Optional.empty()
```

## SED → Period Type Mapping

| SED | Creates Period Type |
|-----|---------------------|
| `A001` | Anmodningsperiode |
| `A003` | Lovvalgsperiode |
| `A011` | Updates Anmodningsperiode |
| `A004` | Rejection (no period) |
