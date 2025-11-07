# Feature Toggles i Melosys-api

Denne dokumentasjonen beskriver hvordan feature toggles håndteres i Melosys-api, med fokus på lokal utvikling med Unleash.

## Innholdsfortegnelse

- [Oversikt](#oversikt)
- [DefaultEnabledUnleash](#defaultenabledunleash)
- [Oppsett av Unleash lokalt](#oppsett-av-unleash-lokalt)
- [Bruk i koden](#bruk-i-koden)
- [Miljøkonfigurasjon](#miljøkonfigurasjon)
- [Best practices](#best-practices)

## Oversikt

Melosys-api bruker [Unleash](https://www.getunleash.io/) for feature toggles. Systemet støtter tre hovedmodi:

1. **Produksjon (NAIS)**: Standard Unleash-oppførsel hvor ukjente flags er disabled
2. **Test**: Standard Unleash-oppførsel hvor ukjente flags er disabled
3. **Lokal utvikling**: Spesiell `DefaultEnabledUnleash`-implementasjon som gjør ukjente flags **enabled** som standard

## DefaultEnabledUnleash

### Problemstilling

Ved lokal utvikling med Unleash i Docker Compose kan det være tungvint å konfigurere alle feature flags manuelt. Dette kan føre til:
- Manuelt arbeid med å sette opp hver ny toggle
- Behov for koordinering mellom utviklere
- Tregere utviklingssyklus

### Løsning: Transparent Wrapper

`DefaultEnabledUnleash` er en wrapper rundt standard `DefaultUnleash` som endrer oppførselen kun for ukjente toggles:

```kotlin
// Arkitektur
Application Code
    ↓ (inject Unleash)
FeatureToggleConfigLocal (@Profile("!nais & !test"))
    ↓ (wraps DefaultUnleash)
DefaultEnabledUnleash (transparent wrapper)
    ↓ (delegates to)
DefaultUnleash (standard Unleash client)
```

### Oppførsel

| Toggle-status | Definert i Unleash? | Resultat |
|---------------|---------------------|----------|
| Ukjent toggle | ❌ Nei | ✅ `true` (enabled) |
| Konfigurert toggle | ✅ Ja, enabled | ✅ `true` (enabled) |
| Konfigurert toggle | ✅ Ja, disabled | ❌ `false` (disabled) |

**Viktig**: Denne oppførselen gjelder **kun** i lokal utvikling (`!nais & !test` profiler).

### Teknisk implementering

Implementeringen sjekker om en toggle er definert før den returnerer verdi:

```kotlin
override fun isEnabled(toggleName: String, defaultSetting: Boolean): Boolean {
    val toggleDefinition = wrappedUnleash.more().getFeatureToggleDefinition(toggleName)

    return if (toggleDefinition.isPresent) {
        // Definert: bruk faktisk status fra Unleash
        wrappedUnleash.isEnabled(toggleName, defaultSetting)
    } else {
        // Ukjent: default til enabled
        true
    }
}
```

### Fordeler

✅ **Null kodeendringer**: All eksisterende kode fungerer uendret
✅ **Profilspesifikt**: Gjelder kun lokal utvikling
✅ **Transparent**: Utviklere injiserer `Unleash` som før
✅ **Type-safe**: Implementerer `Unleash`-interfacet korrekt
✅ **Testbar**: Fullstendig testet med 10 unit tester
✅ **Produksjonssikker**: NAIS og test bruker standard oppførsel

## Oppsett av Unleash lokalt

### 1. Start lokalt miljø med melosys-docker-compose

Det enkleste er å bruke [melosys-docker-compose](https://github.com/navikt/melosys-docker-compose) som starter Unleash sammen med alle andre tjenester:

```bash
# Fra melosys-docker-compose mappen
make start-all
```

Dette starter alle nødvendige tjenester inkludert Unleash.

### 2. Konfigurasjon

Unleash-tilkoblingen er allerede konfigurert i `application-local.yml`:

```yaml
unleash:
  url: http://localhost:4242/api
  token: "*:development.unleash-insecure-client-api-token"
  app-name: melosys-api-local
  environment: development
```

### 3. Unleash Admin UI

Når Unleash kjører, få tilgang til admin-grensesnittet:

```bash
# Fra melosys-docker-compose mappen
make open-unleash
```

Dette åpner Unleash UI i nettleseren på http://localhost:4242

**Login credentials:**
- Brukernavn: `admin`
- Passord: `unleash4all`

### 4. Hvordan det fungerer

Når applikasjonen starter med Unleash server konfigurert (i lokale profiler):

1. `FeatureToggleConfigLocal` leser `unleash.url` og `unleash.token` fra `application-local.yml`
2. Hvis begge er tilstede, opprettes en `DefaultUnleash`-klient som kobles til serveren
3. Klienten wrappes automatisk med `DefaultEnabledUnleash`:
   - For toggles konfigurert i Unleash → bruker faktisk status
   - For toggles IKKE konfigurert i Unleash → returnerer `true` (enabled som standard)
4. Denne wrapperen er **kun aktiv i lokale profiler** (`!nais & !test`)

### 5. Fallback-modus (LocalUnleash)

Hvis `unleash.url` eller `unleash.token` **ikke** er konfigurert:

- `FeatureToggleConfigLocal` oppretter en `LocalUnleash`-instans i stedet
- `LocalUnleash` er en in-memory fake som enabler alle toggles **unntatt**:
  - `MELOSYS_ÅRSAVREGNING_UTEN_FLYT` (eksplisitt disabled)

For å bruke fallback-modus, fjern eller kommenter ut `unleash.url` i `application-local.yml`.

## Bruk i koden

### Definere toggle-navn

Alle toggle-navn bør defineres som konstanter i `ToggleName.kt`:

```kotlin
object ToggleName {
    const val MIN_NYE_FUNKSJON = "melosys.min-nye-funksjon"
}
```

### Injisere Unleash

Injiser `Unleash` på vanlig måte - wrappingen skjer automatisk i lokal utvikling:

```kotlin
@Service
class MinService(
    private val unleash: Unleash  // Får automatisk DefaultEnabledUnleash i lokal utvikling
) {
    fun utførHandling() {
        if (unleash.isEnabled(ToggleName.MIN_NYE_FUNKSJON)) {
            // Returnerer true hvis ikke konfigurert i Unleash (kun lokal utvikling)
            // Returnerer faktisk status hvis konfigurert i Unleash
        }
    }
}
```

### Konfigurere toggles i Unleash UI

For å legge til en ny toggle i Unleash admin UI:

1. Åpne Unleash UI med `make open-unleash` (fra melosys-docker-compose) eller naviger til http://localhost:4242
2. Logg inn med `admin` / `unleash4all`
3. Klikk "New feature toggle"
4. Skriv inn toggle-navnet (f.eks. `melosys.min-nye-funksjon`)
5. Legg til i `default`-prosjektet
6. Enable/disable etter behov
7. Legg til utrullingsstrategier om ønskelig (gradvis utrulling, per bruker-ID, etc.)

## Miljøkonfigurasjon

### Profiler og oppførsel

| Profil | Unleash-implementasjon | Ukjente toggles |
|--------|------------------------|-----------------|
| `local-mock`, `local-q1`, `local-q2` | `DefaultEnabledUnleash` | ✅ Enabled |
| `nais` (prod/preprod) | Standard `DefaultUnleash` | ❌ Disabled |
| `test` | Standard `DefaultUnleash` | ❌ Disabled |

### Konfigurasjonsfiler

- `application-local.yml`: Unleash URL og token for lokal utvikling
- `FeatureToggleConfigLocal.kt`: Oppretter og wrapper Unleash-klient for lokal utvikling
- `FeatureToggleConfigNais.kt`: Standard Unleash-oppsett for NAIS

## Best practices

1. **Definer konstanter i `ToggleName`**: Alle toggle-navn som konstanter for type-sikkerhet
2. **Bruk beskrivende navn**: Følg konvensjonen `melosys.feature-navn`
3. **Test begge tilstander**: Test alltid funksjonen med toggle både ON og OFF
4. **Dokumenter toggles**: Legg til kommentarer som forklarer hva hver toggle styrer
5. **Rydd opp**: Fjern toggles og tilhørende kode når funksjonen er ferdig utrullet
6. **Vær eksplisitt i produksjon**: Konfigurer alle toggles som brukes i prod

## Feilsøking

### Unleash server kobler ikke til

Sjekk om Unleash kjører:
```bash
# Sjekk om Unleash container kjører
docker ps | grep unleash

# Eller prøv å åpne Unleash UI
make open-unleash  # fra melosys-docker-compose mappen
```

Hvis Unleash ikke kjører, start det lokale miljøet:
```bash
make start-all  # fra melosys-docker-compose mappen
```

### Alle toggles returnerer true i lokal utvikling

Dette er forventet oppførsel når toggles ikke er konfigurert i Unleash og du kjører i lokale profiler. For å teste disabled-tilstand, legg til toggle i Unleash UI og disable den.

### Ønsker standard Unleash-oppførsel (ukjent = disabled)?

Default-enabled oppførselen er **kun aktiv i lokale profiler** (`!nais & !test`). I produksjon (NAIS) og test-miljøer gjelder standard Unleash-oppførsel (ukjente toggles = disabled).

Hvis du trenger standard oppførsel i lokal utvikling, fjern eller kommenter ut `unleash.url`-konfigurasjonen for å bruke `LocalUnleash` fallback i stedet.

## Relaterte filer

- `config/src/main/kotlin/no/nav/melosys/featuretoggle/DefaultEnabledUnleash.kt` - Implementasjon
- `config/src/test/kotlin/no/nav/melosys/featuretoggle/DefaultEnabledUnleashTest.kt` - Tester
- `config/src/main/kotlin/no/nav/melosys/featuretoggle/FeatureToggleConfigLocal.kt` - Lokal konfigurasjon
- `config/src/main/kotlin/no/nav/melosys/featuretoggle/ToggleName.kt` - Toggle-navnkonstanter
