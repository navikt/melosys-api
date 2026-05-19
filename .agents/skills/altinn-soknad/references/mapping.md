# Altinn to Domain Mapping

## SoeknadMapper

The `SoeknadMapper` class transforms the Altinn XML structure (`MedlemskapArbeidEOSM`) into the internal `Soeknad` domain model.

## Top-Level Mappings

| Altinn Path | Domain Model | Notes |
|-------------|--------------|-------|
| `innhold.midlertidigUtsendt.arbeidsland` | `Soeknad.soeknadsland.landkoder` | Single country from Altinn |
| `innhold.midlertidigUtsendt.utenlandsoppdraget.periodeUtland` | `Soeknad.periode` | fom/tom dates |
| `innhold.arbeidstaker` | `Soeknad.personOpplysninger` | Person details |
| `innhold.midlertidigUtsendt.arbeidssted` | One of: `arbeidPaaLand`, `maritimtArbeid`, `luftfartBaser` | Based on `typeArbeidssted` |
| `innhold.midlertidigUtsendt.loennOgGodtgjoerelse` | `Soeknad.loennOgGodtgjoerelse` | Salary info |
| `innhold.midlertidigUtsendt.virksomhetIUtlandet` | `Soeknad.foretakUtland` | Foreign company |
| `innhold.arbeidsgiver` | `Soeknad.juridiskArbeidsgiverNorge` | Norwegian employer |
| `innhold.midlertidigUtsendt.utenlandsoppdraget` | `Soeknad.utenlandsoppdraget` | Assignment details |
| `innhold.midlertidigUtsendt` (various) | `Soeknad.arbeidssituasjonOgOevrig` | Work situation |

## Person Mappings

```java
OpplysningerOmBrukeren {
    utenlandskIdent = [
        UtenlandskIdent {
            ident = arbeidstaker.utenlandskIDnummer,
            landkode = midlertidigUtsendt.arbeidsland
        }
    ],
    foedestedOgLand = FoedestedOgLand {
        foedested = arbeidstaker.foedested,
        foedeland = arbeidstaker.foedeland
    },
    medfolgendeFamilie = arbeidstaker.barn.barnet.map {
        MedfolgendeFamilie.tilBarnFraFnrOgNavn(fnr, navn)
    }
}
```

## Work Location Mappings

### Land-Based (LAND)

```java
ArbeidPaaLand {
    fysiskeArbeidssteder = fysiskeArbeidssteder.map { fa ->
        FysiskArbeidssted {
            firmanavn = fa.firmanavn,
            adresse = StrukturertAdresse(
                gatenavn = fa.gatenavn,
                postnummer = fa.postkode,
                poststed = fa.by,
                region = fa.region,
                landkode = fa.land
            )
        }
    },
    erFastArbeidssted = fastArbeidssted,
    erHjemmekontor = hjemmekontor
}
```

### Offshore (OFFSHORE)

```java
List<MaritimtArbeid> = offshoreEnheter.map { oe ->
    MaritimtArbeid {
        enhetNavn = oe.enhetsNavn,
        innretningstype = mapInnretningstyper(oe.enhetsType),
        innretningLandkode = oe.sokkelLand
    }
}

// Innretningstype mapping:
// BORESKIP -> Innretningstyper.BORESKIP
// PLATTFORM, ANNEN_STASJONAER_ENHET -> Innretningstyper.PLATTFORM
```

### Maritime (SKIPSFART)

```java
List<MaritimtArbeid> = skipListe.map { skip ->
    MaritimtArbeid {
        enhetNavn = skip.skipNavn,
        fartsomradeKode = Fartsomrader.valueOf(skip.fartsomraade.toUpperCase()),
        flaggLandkode = skip.flaggland,
        territorialfarvannLandkode = skip.territorialEllerHavnLand
    }
}
```

### Aviation (LUFTFART)

```java
List<LuftfartBase> = luftfartBaser.map { lb ->
    LuftfartBase {
        hjemmebaseNavn = lb.hjemmebaseNavn,
        hjemmebaseLandkode = lb.hjemmebaseLand,
        flyvningstype = Flyvningstyper.valueOf(lb.typeFlyvninger.toUpperCase())
    }
}
```

## Salary Mappings

```java
LoennOgGodtgjoerelse {
    norskArbgUtbetalerLoenn = loennOgGodtgjoerelse.norskArbgUtbetalerLoenn,
    erAnsattHelePerioden = utenlandsoppdraget.erArbeidstakerAnsattHelePerioden,
    utlArbgUtbetalerLoenn = loennOgGodtgjoerelse.utlArbgUtbetalerLoenn,
    utlArbTilhorerSammeKonsern = loennOgGodtgjoerelse.utlArbTilhorerSammeKonsern,
    norskBruttoLoennPerMnd = hentNorskBruttoLoennPerMnd(loennOgGodtgjoerelse),
    utlBruttoLoennPerMnd = loennOgGodtgjoerelse.loennUtlArbg,
    mottarNaturalytelser = loennOgGodtgjoerelse.mottarNaturalytelser,
    samletVerdiNaturalytelser = loennOgGodtgjoerelse.samletVerdiNaturalytelser,
    betalerArbeidsgiveravgift = loennOgGodtgjoerelse.betalerArbeidsgiveravgift,
    trukketTrygdeavgift = loennOgGodtgjoerelse.trukketTrygdeavgift
}
```

### Special Case: Salary Bug Handling

```java
// Altinn sometimes sends loennNorskArbg=0 even when norskArbgUtbetalerLoenn=true
// and utlArbgUtbetalerLoenn=false. We treat this as null.
private static BigDecimal hentNorskBruttoLoennPerMnd(LoennOgGodtgjoerelse log) {
    boolean harAltinnEtProblem = log.isNorskArbgUtbetalerLoenn()
        && !log.isUtlArbgUtbetalerLoenn()
        && BigDecimal.ZERO.equals(log.getLoennNorskArbg());
    return harAltinnEtProblem ? null : log.getLoennNorskArbg();
}
```

## Norwegian Employer Mappings

```java
JuridiskArbeidsgiverNorge {
    erOffentligVirksomhet = arbeidsgiver.offentligVirksomhet,
    // Only for private employers:
    antallAnsatte = samletVirksomhetINorge.antallAnsatte,
    antallAdmAnsatte = samletVirksomhetINorge.antallAdministrativeAnsatteINorge,
    antallUtsendte = samletVirksomhetINorge.antallUtsendte,
    andelOmsetningINorge = samletVirksomhetINorge.andelOmsetningINorge,
    andelOppdragINorge = samletVirksomhetINorge.andelOppdragINorge,
    andelKontrakterINorge = samletVirksomhetINorge.andelKontrakterInngaasINorge,
    andelRekruttertINorge = samletVirksomhetINorge.andelRekrutteresINorge,
    ekstraArbeidsgivere = [arbeidsgiver.virksomhetsnummer]
}
```

## Assignment Mappings

```java
Utenlandsoppdraget {
    samletUtsendingsperiode = erstatterTidligereUtsendte
        ? lagPeriode(samletUtsendingsperiode)
        : new Periode(),
    sendesUtOppdragIUtlandet = utenlandsoppdraget.sendesUtOppdragIUtlandet,
    ansattEtterOppdraget = utenlandsoppdraget.ansattEtterOppdraget,
    ansattForOppdragIUtlandet = utenlandsoppdraget.ansattForOppdragIUtlandet,
    drattPaaEgetInitiativ = utenlandsoppdraget.drattPaaEgetInitiativ,
    erstatter = utenlandsoppdraget.erstatterTidligereUtsendte
}
```

## Work Situation Mappings

```java
ArbeidssituasjonOgOevrig {
    harLoennetArbeidMinstEnMndFoerUtsending = midlertidigUtsendt.loennetArbeidMinstEnMnd,
    beskrivelseArbeidSisteMnd = midlertidigUtsendt.beskrivArbeidSisteMnd,
    harAndreArbeidsgivereIUtsendingsperioden = midlertidigUtsendt.andreArbeidsgivereIUtsendingsperioden,
    beskrivelseAnnetArbeid = midlertidigUtsendt.beskrivelseAnnetArbeid,
    erSkattepliktig = midlertidigUtsendt.skattepliktig,
    mottarYtelserNorge = midlertidigUtsendt.mottaYtelserNorge,
    mottarYtelserUtlandet = midlertidigUtsendt.mottaYtelserUtlandet
}
```

## Country Code Handling

Country codes from Altinn may be in different formats. The `IsoLandkodeKonverterer` is used:

```java
import static no.nav.melosys.domain.util.IsoLandkodeKonverterer.tilIso2FraEuEosLandnavn;

// Converts EU/EEA country names to ISO-2 codes
String landkode = tilIso2FraEuEosLandnavn(postadresseUtland.getLand());
```
