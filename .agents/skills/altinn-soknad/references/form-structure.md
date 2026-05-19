# Altinn Form Structure

## MedlemskapArbeidEOS_M (Form 6320)

The Altinn A1 application form for posted workers in EU/EEA.

### XSD Schema

Located at: `soknad-altinn/src/main/resources/xsd/NAV_MedlemskapArbeidEOS_M_2020-11-11_6320_46081_SERES.xsd`

### Top-Level Structure

```xml
<melding dataFormatId="6320" dataFormatVersion="46081">
  <Innhold>
    <arbeidsgiver>...</arbeidsgiver>
    <arbeidstaker>...</arbeidstaker>
    <midlertidigUtsendt>...</midlertidigUtsendt>
    <fullmakt>...</fullmakt>
  </Innhold>
</melding>
```

## Section: Arbeidsgiver (Employer)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `virksomhetsnummer` | String | Yes | Norwegian org number (9 digits) |
| `virksomhetsnavn` | String | No | Company name |
| `offentligVirksomhet` | Boolean | No | Is public sector employer |
| `kontaktperson` | Object | No | Contact person details |
| `samletVirksomhetINorge` | Object | No | Norwegian operations statistics |
| `adresse` | Object | No | Employer address |

### SamletVirksomhetINorge (If private sector)

| Field | Type | Description |
|-------|------|-------------|
| `antallAnsatte` | Integer | Total employees in Norway |
| `antallAdministrativeAnsatteINorge` | Integer | Admin staff in Norway |
| `antallUtsendte` | Integer | Number of posted workers |
| `andelOmsetningINorge` | Integer | % of revenue in Norway |
| `andelOppdragINorge` | Integer | % of contracts in Norway |
| `andelKontrakterInngaasINorge` | Integer | % of contracts signed in Norway |
| `andelRekrutteresINorge` | Integer | % of recruitment in Norway |

## Section: Arbeidstaker (Employee)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `foedselsnummer` | String | Yes | Norwegian fødselsnummer/D-nummer |
| `utenlandskIDnummer` | String | No | Foreign ID number |
| `foedested` | String | No | Place of birth |
| `foedeland` | String | No | Country of birth |
| `barn` | Object | No | Children traveling with worker |

### Barn (Children)

```xml
<barn>
  <barnet>
    <foedselsnummer>...</foedselsnummer>
    <navn>...</navn>
  </barnet>
</barn>
```

## Section: MidlertidigUtsendt (Posted Worker Details)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `arbeidsland` | String | Yes | Work country (ISO-2) |
| `arbeidssted` | Object | Yes | Work location details |
| `utenlandsoppdraget` | Object | Yes | Assignment details |
| `loennOgGodtgjoerelse` | Object | Yes | Salary information |
| `virksomhetIUtlandet` | Object | No | Foreign company details |
| `loennetArbeidMinstEnMnd` | Boolean | No | Employed at least 1 month before posting |
| `beskrivArbeidSisteMnd` | String | No | Work description last month |
| `andreArbeidsgivereIUtsendingsperioden` | Boolean | No | Other employers during posting |
| `skattepliktig` | Boolean | No | Tax liable in Norway |
| `mottaYtelserNorge` | Boolean | No | Receiving Norwegian benefits |
| `mottaYtelserUtlandet` | Boolean | No | Receiving foreign benefits |

### Arbeidssted (Work Location)

```xml
<arbeidssted>
  <typeArbeidssted>LAND|OFFSHORE|SKIPSFART|LUFTFART</typeArbeidssted>
  <!-- One of the following based on type: -->
  <arbeidPaaLand>...</arbeidPaaLand>
  <offshoreEnheter>...</offshoreEnheter>
  <skipListe>...</skipListe>
  <luftfart>...</luftfart>
</arbeidssted>
```

#### ArbeidPaaLand (Land-based work)

```xml
<arbeidPaaLand>
  <fysiskeArbeidssteder>
    <fysiskArbeidssted>
      <firmanavn>Company Name</firmanavn>
      <gatenavn>Street</gatenavn>
      <postkode>12345</postkode>
      <by>City</by>
      <region>Region</region>
      <land>SE</land>
    </fysiskArbeidssted>
  </fysiskeArbeidssteder>
  <fastArbeidssted>true</fastArbeidssted>
  <hjemmekontor>false</hjemmekontor>
</arbeidPaaLand>
```

#### OffshoreEnheter (Offshore work)

```xml
<offshoreEnheter>
  <offshoreEnhet>
    <enhetsNavn>Platform Name</enhetsNavn>
    <enhetsType>PLATTFORM|BORESKIP|ANNEN_STASJONAER_ENHET</enhetsType>
    <sokkelLand>NO</sokkelLand>
  </offshoreEnhet>
</offshoreEnheter>
```

#### SkipListe (Maritime work)

```xml
<skipListe>
  <skip>
    <skipNavn>Ship Name</skipNavn>
    <fartsomraade>INNENRIKS|UTENRIKS</fartsomraade>
    <flaggland>NO</flaggland>
    <territorialEllerHavnLand>SE</territorialEllerHavnLand>
  </skip>
</skipListe>
```

#### Luftfart (Aviation)

```xml
<luftfart>
  <luftfartBaser>
    <luftfartbase>
      <hjemmebaseNavn>Oslo Gardermoen</hjemmebaseNavn>
      <hjemmebaseLand>NO</hjemmebaseLand>
      <typeFlyvninger>INNENRIKS|UTENRIKS</typeFlyvninger>
    </luftfartbase>
  </luftfartBaser>
</luftfart>
```

### Utenlandsoppdraget (Foreign Assignment)

| Field | Type | Description |
|-------|------|-------------|
| `periodeUtland` | Tidsrom | Start/end dates of assignment |
| `sendesUtOppdragIUtlandet` | Boolean | Sent abroad for assignment |
| `ansattForOppdragIUtlandet` | Boolean | Hired specifically for this assignment |
| `ansattEtterOppdraget` | Boolean | Will remain employed after |
| `drattPaaEgetInitiativ` | Boolean | Went abroad on own initiative |
| `erstatterTidligereUtsendte` | Boolean | Replacing previous posted worker |
| `samletUtsendingsperiode` | Tidsrom | Total posting period (if replacement) |
| `erArbeidstakerAnsattHelePerioden` | Boolean | Employed for entire period |

### LoennOgGodtgjoerelse (Salary)

| Field | Type | Description |
|-------|------|-------------|
| `norskArbgUtbetalerLoenn` | Boolean | Norwegian employer pays salary |
| `loennNorskArbg` | BigDecimal | Monthly salary from Norwegian employer |
| `utlArbgUtbetalerLoenn` | Boolean | Foreign employer pays salary |
| `loennUtlArbg` | BigDecimal | Monthly salary from foreign employer |
| `utlArbTilhorerSammeKonsern` | Boolean | Foreign employer in same group |
| `mottarNaturalytelser` | Boolean | Receives benefits in kind |
| `samletVerdiNaturalytelser` | BigDecimal | Value of benefits in kind |
| `betalerArbeidsgiveravgift` | Boolean | Employer pays social charges |
| `trukketTrygdeavgift` | Boolean | Social security deducted |

## Section: Fullmakt (Power of Attorney)

| Field | Type | Description |
|-------|------|-------------|
| `fullmektigVirksomhetsnummer` | String | Advisory firm org number (if applicable) |
| `fullmaktFraArbeidstaker` | Boolean | Employee has given power of attorney |

### Fullmektig Types Created

| Scenario | Fullmaktstyper |
|----------|----------------|
| Advisory firm fills form | `FULLMEKTIG_ARBEIDSGIVER` |
| + Employee gave fullmakt | `FULLMEKTIG_SØKNAD`, `FULLMEKTIG_ARBEIDSGIVER` |
| Employer fills form + Employee gave fullmakt | `FULLMEKTIG_SØKNAD`, `FULLMEKTIG_ARBEIDSGIVER` |
