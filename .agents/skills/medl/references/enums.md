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

| Constant | `.kode` | Description |
|----------|---------|-------------|
| `AVVIST` | `Avvist` | Rejected by decision |
| `FEILREGISTRERT` | `Feilregistrert` | Registered in error |
| `OPPHORT` | `Opphort` | Terminated/ceased |

## DekningMedl

Social insurance coverage type. Authoritative list: `DekningMedl.kt`.
Note: the enum **constant name** often differs from the transmitted `.kode` string.

| Constant | `.kode` | Description |
|----------|---------|-------------|
| `FULL` | `Full` | Full coverage |
| `UNNTATT` | `Unntatt` | Exempt from coverage |
| `IKKE_PENSJONSDEL` | `IKKEPENDEL` | No pension part |
| `FTRL_2_7_3_LEDD_B` | `FTL_2-7_3_ledd_b` | FTRL §2-7 3rd para b |
| `FTRL_2_7A_2_LEDD_B` | `FTL_2-7a_2_ledd_b` | FTRL §2-7a 2nd para b |
| `FTRL_2_9_1_LEDD_A` … `FTRL_2_9_3_LEDD_1C` | `FTL_2-9_*` | FTRL §2-9 coverage variants |
| `TILLEGSAVTALE_NATO_DEKNING` | `Helsetjenester_sykepenger_...` | NATO supplementary agreement coverage |

(There is no `FTRL_2_7_2_LEDD`, `IKKEMED_FTRL_2_14`, `IKKEMED_FO883_11_3E`, or `FRAVTALE_NATO_FRADRAG` in `DekningMedl`.)

## GrunnlagMedl

Legal basis/regulation reference. Large enum — **`GrunnlagMedl.kt` is the authoritative list**;
`MedlPeriodeKonverter.kt` is the authoritative Lovvalgsbestemmelse→GrunnlagMedl mapping.
The enum **constant name** is what code references; the `.kode` string is what is transmitted
to MEDL and frequently differs (e.g. `FO_11_3_A.kode == "FO_11_3_a"`).

### EU Regulation 883/2004 (FO_*) — verified constants

| Constant | `.kode` | Article | Description |
|----------|---------|---------|-------------|
| `FO_11_3_A` | `FO_11_3_a` | Art. 11.3.a | Work in one country |
| `FO_11_3_B` | `FO_11_3_b` | Art. 11.3.b | Civil servants |
| `FO_11_3_C` | `FO_11_3_c` | Art. 11.3.c | Unemployment benefits |
| `FO_11_3_D` | `FO_11_3_d` | Art. 11.3.d | Military service |
| `FO_11_3_E` | `FO_11_3_e` | Art. 11.3.e | Other persons (residency) |
| `FO_11_4` | `FO_11_4` | Art. 11.4 | Posted civil servants |
| `FO_12_1` | `FO_12_1` | Art. 12.1 | Posted workers |
| `FO_12_2` | `FO_12_2` | Art. 12.2 | Self-employed posted |
| `FO_13_1_A` | `FO_13_1_a` | Art. 13.1.a | Work in multiple countries (employed) |
| `FO_13_1_B` | `FO_13_1_b` | Art. 13.1.b | Work in multiple countries (b.i) |
| `FO_13_2_A` / `FO_13_2_B` | `FO_13_2_a` / `_b` | Art. 13.2 | Self-employed multiple countries |
| `FO_13_3` | `FO_13_3` | Art. 13.3 | Employee + self-employed |
| `FO_13_4` | `FO_13_4` | Art. 13.4 | Civil servants multiple countries |
| `FO_15` | `FO_15` | Art. 15 | Auxiliary staff of EU |
| `FO_16` | `FO_16` | Art. 16 | Exception agreement (ART16_1 and ART16_2 both map here) |

(There is no `FO_13_1` or `FO_16_1` constant — use `FO_13_1_A`/`FO_13_1_B` and `FO_16`.)

### EU Regulation 987/2009 (FO_987_*)

| Constant | `.kode` | Article | Description |
|----------|---------|---------|-------------|
| `FO_987_2009_14_11` | `FO_987_2009_14_11` | Art. 14.11 | Determination of substantial activity / centre of interest |

(There is no `FO_987_14_5` or `FO_987_14_6` constant.)

### Bilateral Treaties

Constant names use the **country name, no `KONV_` prefix** (e.g. `AUSTRALIA`, `USA`, `CANADA`,
`CHILE`, `ISRAEL`, with article suffixes like `AUS_9_2`, `USA_5_1`). The only `KONV_*` constants
are for the EFTA/Storbritannia convention (`KONV_STORBRIT_NIRLAND_13_3_A`,
`KONV_STORBRIT_NIRLAND_13_4_1`, …). See `GrunnlagMedl.kt` for the full country list (no Korea entry exists).

### Norwegian Law (FTL_*)

Constant names use underscores; `.kode` uses a hyphen after the chapter and a leddsuffix
(e.g. `FTL_2_1.kode == "FTL_2-1"`, `FTL_2_5_1_LEDD_A.kode == "FTL_2-5_1_ledd_a"`).

| Constant | Provision | Description |
|----------|-----------|-------------|
| `FTL_2_1` | §2-1 | Mandatory membership — bosatt (residence) |
| `FTL_2_2` | §2-2 | Mandatory membership — arbeidstakere (work in Norway) |
| `FTL_2_3_2_LEDD` | §2-3 2. ledd | Mandatory membership for other groups |
| `FTL_2_5_1_LEDD_A` … `FTL_2_5_2_LEDD` | §2-5 | **Mandatory membership for persons _outside_ Norway** (e.g. Norwegian state employees abroad) |
| `FTL_2_7_1_LEDD` / `FTL_2_7_4_LEDD` / `FTL_2_7A` | §§2-7, 2-7a | Voluntary membership during stay _in_ Norway |
| `FTL_2_8_1_LEDD_A` … `FTL_2_8_4_LEDD` | §2-8 | Voluntary membership during stay _outside_ Norway |
| `FTL_2_15_2_LEDD` | §2-15 2. ledd | (registration/administrative provision) |

FTRL hjemmel notes (folketrygdloven kap. 2):
- §2-5 = pliktig medlemskap for personer **utenfor** Norge — NOT "foreign workers in Norway".
- §2-6 = begrenset medlemskap for **utenlandske statsborgere** (limited to yrkesskade/dødsfall) — NOT "Norwegians abroad". Melosys does not currently map a §2-6 GrunnlagMedl constant.
- §2-8 = **frivillig** medlemskap under opphold utenfor Norge — NOT "exemption from membership". Unntak fra medlemskap er §§2-11 til 2-13.
- There is no membership §2-14 in chapter 2 (the chapter ends at §2-13). The old `FTL_2_14` table entry is not a real FTRL paragraph and has no GrunnlagMedl constant.

### Transition Rules (FO_1408_*)

Old EU Regulation 1408/71 codes used for Art. 87.8 / 87a overgangsregler. Verified constants
(`.kode` in parentheses):
- `FO_1408_14_2_A` (`FO_1408_14_2_a`), `FO_1408_14_2_B` (`FO_1408_14_2_b`)
- `FO_1408_14_A_2` (`FO_1408_14a_2`)
- `FO_1408_14_C_A` (`FO_1408_14c_a`), `FO_1408_14_C_B` (`FO_1408_14c_b`)

(There is no `FO_1408_13_2_A` or `FO_1408_17` constant — the illustrative list is not exhaustive; see `GrunnlagMedl.kt`.)

## KildedokumenttypeMedl

Source document type. The constant name and the transmitted `.kode` differ for `A1`.

| Constant | `.kode` | Description | When Used |
|----------|---------|-------------|-----------|
| `HENV_SOKNAD` | `Henv_Soknad` | Application/request | Default for applications |
| `SED` | `SED` | Structured Electronic Document | EU/EEA document exchange |
| `DOKUMENT` | `Dokument` | Generic document | Treaty + registration exemption |
| `A1` | `PortBlank_A1` | A1 Certificate | A1 anmodning |

**Selection logic** (`MedlPeriodeService.hentKildedokumenttype`, simplified):
```kotlin
when {
    erTrygdeavtale && erRegistreringUnntak -> DOKUMENT
    erTrygdeavtale && erAnmodning -> HENV_SOKNAD
    erA1Anmodning -> A1
    erSedBehandling -> SED
    else -> HENV_SOKNAD
}
