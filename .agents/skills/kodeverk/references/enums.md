# Kodeverk Enums Reference

All enums are defined in the external `melosys-internt-kodeverk` library.

## Sakstyper (Case Types)

| Value | Description | Usage |
|-------|-------------|-------|
| `EU_EOS` | EU/EEA coordination | Most common, EU Reg. 883/2004 |
| `FTRL` | Folketrygdloven | Norwegian law cases |
| `TRYGDEAVTALE` | Bilateral treaty | Non-EU agreements |

## Sakstemaer (Case Themes)

| Value | Description |
|-------|-------------|
| `MEDLEMSKAP_LOVVALG` | Membership and law choice |
| `TRYGDEAVGIFT` | Social insurance charge |
| `UNNTAK` | Exemption |

## Saksstatuser (Case Statuses)

| Value | Description | Terminal |
|-------|-------------|----------|
| `UNDER_BEHANDLING` | Active processing | No |
| `HENLAGT` | Dismissed | Yes |
| `AVSLUTTET` | Completed | Yes |
| `OPPHØRT` | Terminated | Yes |
| `ANNULLERT` | Annulled | Yes |
| `HENLAGT_BORTFALT` | Dismissed - lapsed | Yes |

## Behandlingstyper (Treatment Types)

| Value | Description | Restrictions |
|-------|-------------|--------------|
| `FØRSTEGANG` | First-time | Initial treatment |
| `NY_VURDERING` | Re-assessment | Changed circumstances |
| `KLAGE` | Appeal | Requires toggle |
| `HENVENDELSE` | Inquiry | Allowed for closed cases |
| `ÅRSAVREGNING` | Annual settlement | Requires toggle, limited themes |
| `ENDRET_PERIODE` | Changed period | Period modifications |
| `MANGLENDE_INNBETALING_TRYGDEAVGIFT` | Missing payment | Trygdeavgift follow-up |

## Behandlingstema (Treatment Themes)

### Employment-Related

| Value | Description |
|-------|-------------|
| `YRKESAKTIV` | Employed person |
| `IKKE_YRKESAKTIV` | Non-employed |
| `PENSJONIST` | Pensioner/retiree |
| `UTSENDT_ARBEIDSTAKER` | Posted worker |
| `UTSENDT_SELVSTENDIG` | Posted self-employed |
| `ARBEID_FLERE_LAND` | Multi-state worker |
| `ARBEID_TJENESTEPERSON_ELLER_FLY` | Civil servant or flight crew |
| `ARBEID_KUN_NORGE` | Work only in Norway |

### Membership-Related

| Value | Description |
|-------|-------------|
| `UNNTAK_MEDLEMSKAP` | Exemption from membership |
| `TRYGDETID` | Insurance period calculation |

### Exception/Designation

| Value | Description |
|-------|-------------|
| `ANMODNING_OM_UNNTAK_HOVEDREGEL` | Exception request (Art. 16) |
| `REGISTRERING_UNNTAK` | Register exception |
| `REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING` | Posting exception |
| `REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE` | Other exception |
| `BESLUTNING_LOVVALG_NORGE` | Norway designated |
| `BESLUTNING_LOVVALG_ANNET_LAND` | Other country designated |

### Other

| Value | Description |
|-------|-------------|
| `VIRKSOMHET` | Company-related |
| `FORESPØRSEL_TRYGDEMYNDIGHET` | Authority inquiry |
| `A1_ANMODNING_OM_UNNTAK_PAPIR` | Paper A1 request |

## Behandlingsstatus (Treatment Status)

| Value | Description |
|-------|-------------|
| `UNDER_BEHANDLING` | Being processed |
| `AVVENT_DOK_PART` | Awaiting party documentation |
| `AVVENT_DOK_UTL` | Awaiting foreign documentation |
| `AVVENT_FAGLIG_AVKLARING` | Awaiting professional clarification |

## Behandlingsaarsaktyper (Treatment Reason Types)

| Value | Description |
|-------|-------------|
| `SØKNAD` | Application |
| `SED` | EU electronic document |
| `HENVENDELSE` | Inquiry |
| `FRITEKST` | Free text |
| `MELDING_OM_MANGLENDE_INNBETALING` | Missing payment notification |

## Aktoersroller (Actor Roles)

| Value | Description |
|-------|-------------|
| `BRUKER` | Individual person |
| `VIRKSOMHET` | Organization/company |

## Tema (System Topics)

| Value | Code | Description |
|-------|------|-------------|
| `MED` | MED | Membership |
| `TRY` | TRY | Social insurance charge |
| `UFM` | UFM | Exemption from membership |

## InnvilgelsesResultat (Approval Results)

| Value | Description |
|-------|-------------|
| `INNVILGET` | Approved |
| `AVSLAATT` | Rejected |
| `OPPHØRT` | Terminated |

## Medlemskapstyper (Membership Types)

| Value | Description |
|-------|-------------|
| `PLIKTIG` | Mandatory membership |
| `FRIVILLIG` | Voluntary membership |
| `UNNTATT` | Exempt from membership |
