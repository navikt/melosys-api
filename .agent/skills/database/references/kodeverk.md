# Kodeverk Tables (Lookup/Reference Data)

All lookup tables in the melosys database with their valid values.

## Case Types

### FAGSAK_TYPE
```
EU_EOS        - EU/EEA regulation (trygdeforordningene)
TRYGDEAVTALE  - Bilateral treaty
FTRL          - National Insurance Act only (folketrygdloven)
```

### FAGSAK_TEMA
```
MEDLEMSKAP_LOVVALG  - Membership and applicable legislation
UNNTAK              - Exception cases
TRYGDEAVGIFT        - Social security charges
```

### FAGSAK_STATUS
```
OPPRETTET           - Created, not yet processed
LOVVALG_AVKLART     - Applicable legislation determined
MEDLEMSKAP_AVKLART  - Membership determined
TRYGDEAVGIFT_AVKLART - Charges determined
HENLAGT             - Dismissed
HENLAGT_BORTFALT    - Dismissed as lapsed
VIDERESENDT         - Forwarded
AVSLUTTET           - Closed
ANNULLERT           - Annulled
OPPHØRT             - Ceased
```

## Treatment Types

### BEHANDLING_TYPE
```
FØRSTEGANG                        - First-time application
NY_VURDERING                      - Re-evaluation
KLAGE                             - Complaint
ANKE                              - Appeal
ENDRET_PERIODE                    - Period modification
HENVENDELSE                       - Inquiry
MANGLENDE_INNBETALING_TRYGDEAVGIFT - Missing payment
ÅRSAVREGNING                      - Annual reconciliation
SATSENDRING                       - Rate change
SED                               - SED document processing
SØKNAD                            - Application
```

### BEHANDLING_STATUS
```
OPPRETTET                     - Created
UNDER_BEHANDLING              - In progress
AVSLUTTET                     - Closed
IVERKSETTER_VEDTAK            - Executing decision
MIDLERTIDIG_LOVVALGSBESLUTNING - Provisional decision (Art. 13)
AVVENT_DOK_UTL                - Awaiting foreign documentation
AVVENT_DOK_PART               - Awaiting party documentation
TIDSFRIST_UTLØPT              - Deadline expired
VURDER_DOKUMENT               - New document received
FORELØPIG_LOVVALG             - Awaiting provisional response
ANMODNING_UNNTAK_SENDT        - Exception request sent
SVAR_ANMODNING_MOTTATT        - Exception response received
AVVENT_FAGLIG_AVKLARING       - Awaiting expert clarification
SENDT_ASD                     - Sent to ministry
SVAR_ASD_MOTTATT              - Ministry response received
```

### BEHANDLING_TEMA (23 values)
```
UTSENDT_ARBEIDSTAKER              - Posted worker
UTSENDT_SELVSTENDIG               - Posted self-employed
ARBEID_FLERE_LAND                 - Work in multiple countries
ARBEID_KUN_NORGE                  - Work only in Norway
ARBEID_NORGE_BOSATT_ANNET_LAND    - Work in Norway, residing abroad
ARBEID_ETT_LAND_ØVRIG             - Other single country work
ARBEID_TJENESTEPERSON_ELLER_FLY   - Civil servant/flight crew
PENSJONIST                        - Pensioner
IKKE_YRKESAKTIV                   - Non-employed
YRKESAKTIV                        - Employed
VIRKSOMHET                        - Company-related
UNNTAK_MEDLEMSKAP                 - Membership exception
REGISTRERING_UNNTAK               - Exception registration
REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING  - Posted exception
REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE          - Other exceptions
TRYGDETID                         - Insurance period inquiry
BESLUTNING_LOVVALG_NORGE          - Norway designated (A003)
BESLUTNING_LOVVALG_ANNET_LAND     - Other country designated (A003)
ANMODNING_OM_UNNTAK_HOVEDREGEL    - Exception request (A001)
A1_ANMODNING_OM_UNNTAK_PAPIR      - Paper A1 exception request
FORESPØRSEL_TRYGDEMYNDIGHET       - Authority inquiry
ØVRIGE_SED_MED                    - Other SED - membership
ØVRIGE_SED_UFM                    - Other SED - exception
```

## Actor Roles

### ROLLE_TYPE
```
BRUKER                    - Citizen (the person the case concerns)
VIRKSOMHET                - Company
ARBEIDSGIVER              - Employer
TRYGDEMYNDIGHET           - Foreign social security authority
FULLMEKTIG                - Power of attorney holder
REPRESENTANT              - Representative
REPRESENTANT_TRYGDEAVGIFT - Charge payment representative
```

## Result Types

### BEHANDLINGSRESULTAT_TYPE
```
FASTSATT_LOVVALGSLAND             - Legislation country determined
FORELØPIG_FASTSATT_LOVVALGSLAND   - Provisionally determined
FASTSATT_TRYGDEAVGIFT             - Charges determined
MEDLEM_I_FOLKETRYGDEN             - Member of National Insurance
UNNTATT_MEDLEMSKAP                - Excepted from membership
ANMODNING_OM_UNNTAK               - Exception requested
REGISTRERT_UNNTAK                 - Exception registered
GODKJENT_UNNTAK                   - Exception approved
DELVIS_GODKJENT_UNNTAK            - Partially approved
AVSLAG_SØKNAD                     - Application rejected
AVSLAG_MANGLENDE_OPPL             - Rejected due to missing info
AVVIST_KLAGE                      - Complaint dismissed
KLAGEINNSTILLING                  - Complaint recommendation
MEDHOLD                           - Complaint upheld
OMGJORT                           - Decision revised
HENLEGGELSE                       - Dismissed
HENLEGGELSE_BORTFALT              - Dismissed as lapsed
FERDIGBEHANDLET                   - Processing complete
ANNULLERT                         - Annulled
AVBRUTT                           - Aborted
OPPHØRT                           - Ceased
DELVIS_OPPHØRT                    - Partially ceased
UTPEKING_NORGE_AVVIST             - Norway designation rejected
IKKE_FASTSATT                     - Not determined
```

## Process Types

### PROSESS_TYPE (partial list of 47 types)
```
-- Journalføring (document registration)
JFR_NY_SAK_BRUKER                 - New case from document
JFR_NY_SAK_VIRKSOMHET             - New company case
JFR_KNYTT                         - Link to existing case
JFR_ANDREGANG_NY_BEHANDLING       - New treatment on existing case
JFR_ANDREGANG_REPLIKER_BEHANDLING - Replicate previous treatment

-- Vedtak (decision execution)
IVERKSETT_VEDTAK_EOS              - Execute EU/EEA decision
IVERKSETT_VEDTAK_FTRL             - Execute national decision
IVERKSETT_VEDTAK_TRYGDEAVTALE     - Execute treaty decision
IVERKSETT_VEDTAK_IKKE_YRKESAKTIV  - Execute non-employed decision
IVERKSETT_VEDTAK_AARSAVREGNING    - Execute annual reconciliation

-- SED (document exchange)
MOTTAK_SED                        - Receive SED
MOTTAK_SED_JOURNALFØRING          - Journal SED only
ARBEID_FLERE_LAND_NY_SAK          - A003 new case
ARBEID_FLERE_LAND_NY_BEHANDLING   - A003 new treatment

-- Unntak (exceptions)
ANMODNING_OM_UNNTAK               - Exception request
ANMODNING_OM_UNNTAK_SVAR          - Exception response
REGISTRERING_UNNTAK_NY_SAK        - Register exception new case
REGISTRERING_UNNTAK_GODKJENN      - Approve exception
REGISTRERING_UNNTAK_AVVIS         - Reject exception

-- Other
OPPRETT_SAK                       - Create case
HENLEGG_SAK                       - Dismiss case
ANNULLER_SAK                      - Annul case
SEND_BREV                         - Send letter
OPPRETT_OG_DISTRIBUER_BREV        - Create and distribute letter
SATSENDRING                       - Rate change processing
```

## Usage Examples

### Check if status is valid transition
```sql
-- Example: Check valid next statuses for UNDER_BEHANDLING
SELECT *
FROM (
    SELECT 'IVERKSETTER_VEDTAK' as next_status FROM DUAL
    UNION SELECT 'AVVENT_DOK_UTL' FROM DUAL
    UNION SELECT 'AVVENT_DOK_PART' FROM DUAL
    UNION SELECT 'MIDLERTIDIG_LOVVALGSBESLUTNING' FROM DUAL
    UNION SELECT 'AVSLUTTET' FROM DUAL
);
```

### Find all behandlinger by type
```sql
SELECT b.id, f.saksnummer, b.beh_type, bt.navn
FROM behandling b
JOIN fagsak f ON b.saksnummer = f.saksnummer
JOIN behandling_type bt ON b.beh_type = bt.kode
WHERE b.beh_type = 'NY_VURDERING';
```
