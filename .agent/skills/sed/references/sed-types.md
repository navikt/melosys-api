# SED Types Reference

Complete reference for SED (Structured Electronic Document) types.

## A-Series (Applicable Legislation)

EU Regulation 883/2004 applicable legislation documents.

| SED | Name | Purpose | Direction |
|-----|------|---------|-----------|
| `A001` | Request for applicable legislation | Start exception agreement process (Art. 16) | Outgoing |
| `A002` | Request for information | Ask for additional info | Both |
| `A003` | Certificate of applicable legislation | A1 certificate / decision | Outgoing |
| `A004` | Refusal notification | Refuse certificate request | Outgoing |
| `A005` | Reply to request | Response to A002 | Both |
| `A006` | Notification of termination | End of posting period | Both |
| `A007` | Notification of changes | Change in circumstances | Both |
| `A008` | Reply to notification | Response to A006/A007 | Both |
| `A009` | Reminder (Purring) | Follow-up on pending request | Outgoing |
| `A010` | Additional information | Supplement to previous SED | Both |
| `A011` | Approval notification | Approve exception request | Both |
| `A012` | Change notification | Notify of changes to decision | Both |

### When to Use

**A001 - Exception Request (Art. 16)**:
- Worker needs exception from normal rules
- Sent to country that would normally be competent
- Starts LA_BUC_04 workflow

**A003 - A1 Certificate**:
- Confirms applicable legislation
- Core decision document
- Used for posted workers, multi-state workers

**A004 - Refusal**:
- Reject incoming request
- Must include reason

**A011 - Approval**:
- Response to A001
- Confirms exception agreement

## H-Series (Healthcare)

Healthcare coordination under EU Regulation 883/2004.

| SED | Name | Purpose |
|-----|------|---------|
| `H001` | Request for healthcare | Request coverage confirmation |
| `H002` | Reply to healthcare request | Response to H001 |
| `H003` | Healthcare entitlement | S1/E106 equivalent |
| `H004` | Notification of change | Change in healthcare status |
| `H005` | Insurance periods | Confirm insurance periods |
| `H006` | Reply with periods | Response to H005 |
| `H020` | Insurance statement | Period statement for healthcare |
| `H021` | Reply to H020 | Confirm periods |
| `H061` | Direct payment | Healthcare direct payment |
| `H062` | Settlement | Healthcare cost settlement |
| `H120` | Cost claim | Healthcare cost reimbursement |
| `H121` | Reply to cost claim | Response to H120 |

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
| `X050` | Institution info | Institution details |

### X008 - Invalidation

Used to cancel/correct previous SEDs:
```kotlin
// Send invalidation for previous A003
eessiService.sendSedPåEksisterendeBuc(
    behandling = behandling,
    sedType = SedType.X008,
    rinaSaksnummer = originalRinaCaseId
)
```

## S-Series (Special)

Special category documents.

| SED | Name | Purpose |
|-----|------|---------|
| `S040` | Pension claim | Pension application |
| `S041` | Pension reply | Response to pension claim |

## SED Type Enum

**Location**: `domain/src/main/kotlin/no/nav/melosys/domain/eessi/SedType.kt`

```kotlin
enum class SedType {
    A001, A002, A003, A004, A005, A006, A007, A008, A009, A010, A011, A012,
    H001, H002, H003, H004, H005, H006, H020, H021, H061, H062, H120, H121,
    X001, X002, X003, X004, X005, X006, X007, X008, X009, X010, X011, X012, X050,
    S040, S041,
    // ... additional types
}
```

## SED → Behandlingstema Mapping

```kotlin
// SedTypeTilBehandlingstemaMapper
when (sedType) {
    A001, A003, A004, A011, A012 -> Behandlingstema.LOVVALG
    H001, H003, H020, H121 -> Behandlingstema.HELSE
    // ...
}
```

## SED → Period Type Mapping

| SED | Creates Period Type |
|-----|---------------------|
| `A001` | Anmodningsperiode |
| `A003` | Lovvalgsperiode |
| `A011` | Updates Anmodningsperiode |
| `A004` | Rejection (no period) |
