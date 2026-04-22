# Letter Types Reference

## Produserbaredokumenter Enum

All letter types are defined in the external `melosys-internt-kodeverk` library.

## Decision Letters (Vedtaksbrev)

### FTRL (Folketrygdloven)

| Enum | Template | Journal Title | Category |
|------|----------|---------------|----------|
| `INNVILGELSE_FOLKETRYGDLOVEN` | innvilgelse_ftrl | Vedtak om frivillig medlemskap | VB |
| `PLIKTIG_MEDLEM_FTRL` | pliktig_medlem_ftrl | Vedtak om pliktig medlemskap | VB |
| `PENSJONIST_PLIKTIG_FTRL` | pensjonist_pliktig_ftrl | Vedtak om pliktig medlemskap | VB |
| `PENSJONIST_FRIVILLIG_FTRL` | pensjonist_frivillig_ftrl | Vedtak om frivillig medlemskap | VB |
| `IKKE_YRKESAKTIV_PLIKTIG_FTRL` | ikke_yrkesaktiv_pliktig_ftrl | Vedtak om pliktig medlemskap | VB |
| `IKKE_YRKESAKTIV_FRIVILLIG_FTRL` | ikke_yrkesaktiv_frivillig_ftrl | Vedtak om frivillig medlemskap | VB |

### EØS/EFTA

| Enum | Template | Journal Title | Category |
|------|----------|---------------|----------|
| `INNVILGELSE_EFTA_STORBRITANNIA` | innvilgelse_efta_storbritannia | Vedtak om medlemskap | VB |
| `AVSLAG_EFTA_STORBRITANNIA` | avslag_efta_storbritannia | Avslag på søknad om medlemskap | VB |

### Trygdeavtale (Bilateral)

| Enum | Template | Journal Title | Category |
|------|----------|---------------|----------|
| `TRYGDEAVTALE_GB` | trygdeavtale_gb | Vedtak om medlemskap + Attest | VB |
| `TRYGDEAVTALE_US` | trygdeavtale_us | Vedtak om medlemskap + Attest | VB |
| `TRYGDEAVTALE_CAN` | trygdeavtale_ca | Vedtak om medlemskap + Attest | VB |
| `TRYGDEAVTALE_AU` | trygdeavtale_au | Vedtak om medlemskap + Attest | VB |

### Special Cases

| Enum | Template | Journal Title | Category |
|------|----------|---------------|----------|
| `IKKE_YRKESAKTIV_VEDTAKSBREV` | ikke_yrkesaktiv_vedtaksbrev | Vedtak om medlemskap | VB |
| `VEDTAK_OPPHOERT_MEDLEMSKAP` | vedtak_opphoert_medlemskap | (From enum beskrivelse) | VB |
| `AARSAVREGNING_VEDTAKSBREV` | aarsavregning_vedtaksbrev | Vedtak om årsavregning | VB |

## Rejection Letters

| Enum | Template | Journal Title | Category |
|------|----------|---------------|----------|
| `AVSLAG_MANGLENDE_OPPLYSNINGER` | avslag_manglende_opplysninger | Avslag pga manglende opplysninger | VB |

## Information Letters (Infobrev)

### Processing Time Notices

| Enum | Template | Journal Title | Category |
|------|----------|---------------|----------|
| `MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD` | saksbehandlingstid_soknad | Melding om forventet saksbehandlingstid | IB |
| `MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE` | saksbehandlingstid_klage | Melding om forventet saksbehandlingstid | IB |

### Missing Information

| Enum | Template | Journal Title | Category |
|------|----------|---------------|----------|
| `MANGELBREV_BRUKER` | mangelbrev_bruker | Melding om manglende opplysninger | IB |
| `MANGELBREV_ARBEIDSGIVER` | mangelbrev_arbeidsgiver | Melding om manglende opplysninger | IB |

### Orientations/Notifications

| Enum | Template | Journal Title | Category |
|------|----------|---------------|----------|
| `ORIENTERING_ANMODNING_UNNTAK` | orientering_anmodning_unntak | (From enum beskrivelse) | IB |
| `ORIENTERING_TIL_ARBEIDSGIVER_OM_VEDTAK` | orientering_til_arbeidsgiver_om_vedtak | (From enum beskrivelse) | IB |
| `MELDING_HENLAGT_SAK` | henleggelse | Henleggelse av søknad | IB |

### Trygdeavgift Related

| Enum | Template | Journal Title | Category |
|------|----------|---------------|----------|
| `INNHENTING_AV_INNTEKTSOPPLYSNINGER` | innhenting_av_inntektsopplysninger | (From enum beskrivelse) | IB |
| `TRYGDEAVGIFT_INFORMASJONSBREV` | trygdeavgift_informasjonsbrev | Informasjon om trygdeavgift | IB |
| `VARSELBREV_MANGLENDE_INNBETALING` | varsel_manglende_innbetaling | Melding om manglende innbetaling + Varsel om opphør | IB |

## Free-Text Letters

| Enum | Template | Journal Title | Category |
|------|----------|---------------|----------|
| `FRITEKSTBREV` | fritekstbrev | (From enum beskrivelse) | IB |
| `GENERELT_FRITEKSTBREV_BRUKER` | fritekstbrev | (From enum beskrivelse) | IB |
| `GENERELT_FRITEKSTBREV_ARBEIDSGIVER` | fritekstbrev | (From enum beskrivelse) | IB |
| `GENERELT_FRITEKSTBREV_VIRKSOMHET` | fritekstbrev | (From enum beskrivelse) | IB |
| `UTENLANDSK_TRYGDEMYNDIGHET_FRITEKSTBREV` | trygdeavtale_fritekstbrev | (From enum beskrivelse) | IB |
| `GENERELT_FRITEKSTVEDLEGG` | fritekstvedlegg | (From enum beskrivelse) | IB |

## Certificates & Attestations

| Enum | Template | Journal Title | Category |
|------|----------|---------------|----------|
| `ATTEST_A1` | (legacy) | A1 Certificate | VB |
| `ANMODNING_UNNTAK` | (legacy) | Article 16 Request | VB |

## Legacy Letters (DokumentService)

Letters not yet migrated to Dokgen are handled by the legacy `DokumentService`:

| Enum | Description |
|------|-------------|
| `INNVILGELSE_YRKESAKTIV` | Old approval letter |
| `INNVILGELSE_YRKESAKTIV_FLERE_LAND` | Multi-state approval |
| `AVSLAG_YRKESAKTIV` | Old rejection letter |
| `INNVILGELSE_ARBEIDSGIVER` | Employer approval |
| `AVSLAG_ARBEIDSGIVER` | Employer rejection |
| `ORIENTERING_UTPEKING_UTLAND` | Foreign designation |
| `ORIENTERING_VIDERESENDT_SOEKNAD` | Application forwarded |

## Letter Type by Use Case

### New Application (FØRSTEGANG)
- `MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD` - Initial response
- `MANGELBREV_*` - If info missing
- `INNVILGELSE_*` or `AVSLAG_*` - Decision

### Appeal (KLAGE)
- `MELDING_FORVENTET_SAKSBEHANDLINGSTID_KLAGE` - Initial response
- Decision letter based on outcome

### Article 16 Exception
- `ORIENTERING_ANMODNING_UNNTAK` - Notify about request sent
- Decision letter after response

### Membership Termination
- `VEDTAK_OPPHOERT_MEDLEMSKAP` - Termination decision
- `VARSELBREV_MANGLENDE_INNBETALING` - Warning before termination

### Annual Settlement (Årsavregning)
- `TRYGDEAVGIFT_INFORMASJONSBREV` - Information
- `AARSAVREGNING_VEDTAKSBREV` - Settlement decision
