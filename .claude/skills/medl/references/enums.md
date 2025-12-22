# MEDL Enums Reference

Complete reference for MEDL-specific enums used in the integration.

## PeriodestatusMedl

Period validity status in MEDL.

| Code | Name | Description |
|------|------|-------------|
| `GYLD` | Gyldig | Valid/approved period |
| `UAVK` | Under avklaring | Under clarification, pending decision |
| `AVST` | Avstått | Ceased/terminated/voided |

**Usage**:
- `GYLD` - Final approved periods
- `UAVK` - Provisional periods, Article 16 requests pending response
- `AVST` - Rejected, terminated, or misregistered periods

## LovvalgMedl

Law choice determination status.

| Code | Name | Description |
|------|------|-------------|
| `ENDL` | Endelig | Final determination |
| `FORL` | Foreløpig | Provisional/temporary |
| `UAVK` | Under avklaring | Under clarification |

**Usage**:
- `ENDL` - Most approved periods
- `FORL` - Article 13 without approved exemption registration
- `UAVK` - Anmodningsperioder (exception requests)

## StatusaarsakMedl

Reason for period status (used with AVST status).

| Code | Name | Description |
|------|------|-------------|
| `AVVIST` | Avvist | Rejected by decision |
| `FEILREGISTRERT` | Feilregistrert | Registered in error |
| `OPPHORT` | Opphørt | Terminated/ceased |

## DekningMedl

Social insurance coverage type.

| Code | Description | Domain Mapping |
|------|-------------|----------------|
| `FULL` | Full coverage | FULL_DEKNING, FULL_DEKNING_* |
| `UNNTATT` | Exempt from coverage | UTEN_DEKNING |
| `IKKE_PENSJONSDEL` | No pension part | Specific FTRL cases |
| `FTRL_2_7_3_LEDD_B` | FTRL §2-7, 3rd para b | FTRL-specific |
| `FTRL_2_7_2_LEDD` | FTRL §2-7, 2nd para | FTRL-specific |
| `IKKEMED_FTRL_2_14` | Not member FTRL §2-14 | FTRL-specific |
| `IKKEMED_FO883_11_3E` | Not member EU 883 11.3e | EU regulation |
| `FRAVTALE_NATO_FRADRAG` | NATO agreement deduction | Special agreement |

## GrunnlagMedl

Legal basis/regulation reference. Extensive enum with 200+ values.

### EU Regulation 883/2004 (FO_*)

| Code | Article | Description |
|------|---------|-------------|
| `FO_11_3_A` | Art. 11.3.a | Work in one country |
| `FO_11_3_B` | Art. 11.3.b | Civil servants |
| `FO_11_3_C` | Art. 11.3.c | Unemployment benefits |
| `FO_11_3_D` | Art. 11.3.d | Military service |
| `FO_11_3_E` | Art. 11.3.e | Other persons (residency) |
| `FO_11_4` | Art. 11.4 | Posted civil servants |
| `FO_12_1` | Art. 12.1 | Posted workers |
| `FO_12_2` | Art. 12.2 | Self-employed posted |
| `FO_13_1` | Art. 13.1 | Work in multiple countries |
| `FO_13_2` | Art. 13.2 | Self-employed multiple countries |
| `FO_13_3` | Art. 13.3 | Employee + self-employed |
| `FO_13_4` | Art. 13.4 | Civil servants multiple countries |
| `FO_16_1` | Art. 16.1 | Exception agreement |

### EU Regulation 987/2009 (FO_987_*)

| Code | Article | Description |
|------|---------|-------------|
| `FO_987_14_5` | Art. 14.5 | Ship crew |
| `FO_987_14_6` | Art. 14.6 | Flight crew |
| `FO_987_14_11` | Art. 14.11 | Marginal activity |

### Bilateral Treaties (KONV_*)

| Code Pattern | Country |
|--------------|---------|
| `KONV_AUSTRALIA_*` | Australia |
| `KONV_CANADA_*` | Canada |
| `KONV_USA_*` | USA |
| `KONV_STORBRIT_*` | UK/Northern Ireland |
| `KONV_CHILE_*` | Chile |
| `KONV_ISRAEL_*` | Israel |
| `KONV_KOREA_*` | South Korea |

### Norwegian Law (FTL_*)

| Code | Provision | Description |
|------|-----------|-------------|
| `FTL_2_1` | §2-1 | Mandatory membership (residence) |
| `FTL_2_2` | §2-2 | Mandatory membership (work) |
| `FTL_2_5` | §2-5 | Foreign workers in Norway |
| `FTL_2_6` | §2-6 | Norwegians abroad |
| `FTL_2_7` | §2-7 | Voluntary membership |
| `FTL_2_8` | §2-8 | Exemption from membership |
| `FTL_2_14` | §2-14 | Limited membership |

### Transition Rules (FO_1408_*)

Old EU Regulation 1408/71 codes for transition cases:
- `FO_1408_13_2_A` - Art. 13.2.a
- `FO_1408_14_*` - Art. 14 variants
- `FO_1408_17` - Exception agreement (old)

## KildedokumenttypeMedl

Source document type.

| Code | Description | When Used |
|------|-------------|-----------|
| `HENV_SOKNAD` | Application/request | Default for applications |
| `SED` | Structured Electronic Document | EU/EEA document exchange |
| `DOKUMENT` | Generic document | Treaty + registration exemption |
| `A1` | A1 Certificate | A1 anmodning |

**Selection logic**:
```kotlin
when {
    erTrygdeavtale && erRegistreringUnntak -> DOKUMENT
    erTrygdeavtale && erAnmodning -> HENV_SOKNAD
    erEuEos && erA1Anmodning -> A1
    erSedBehandling -> SED
    else -> HENV_SOKNAD
}
```
