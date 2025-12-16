# Add Missing Endpoints to melosys-docker-compose-mock

## Background

The melosys-api integration tests are migrating from in-process mocks to using the melosys-docker-compose-mock container. However, two endpoints are missing from the container and currently fall back to the in-process mock.

## Missing Endpoints

### 1. KodeverkAPI (felles-kodeverk)

**Endpoint:** `GET /api/v1/kodeverk/{kodeverkNavn}/koder/betydninger`

**Query Parameters:**
- `ekskluderUgyldige` (boolean)
- `oppslagsdato` (date)
- `spraak` (string, e.g., "nb")

**Response:** Returns a `FellesKodeverkDto` with kodeverk meanings.

**Example Response:**
```json
{
  "betydninger": {
    "andreSkift": [
      {
        "gyldigFra": "2019-01-01",
        "gyldigTil": "9999-12-31",
        "beskrivelser": {
          "nb": {
            "term": "Andre skift",
            "tekst": "Andre skift"
          }
        }
      }
    ]
  }
}
```

**Implementation Notes:**
- The endpoint is called with various `kodeverkNavn` values like `Arbeidstidsordninger`
- The mock just needs to return a valid response structure - the actual values don't matter for tests
- The implementation should be simple and return a generic response for any kodeverk name

### 2. Inngangsvilkaar API

**Endpoint:** `POST /api/inngangsvilkaar`

**Request Body:**
```json
{
  "statsborgerskap": ["NO", "SE"],
  "arbeidsland": ["NO"],
  "flereLandUkjentHvilke": false,
  "periode": {
    "fom": "2024-01-01",
    "tom": "2024-12-31"
  }
}
```

**Response:**
```json
{
  "kvalifisererForEf883_2004": false,
  "feilmeldinger": []
}
```

**Implementation Notes:**
- Always returns `kvalifisererForEf883_2004: false` and empty `feilmeldinger`
- This is a simple stub that doesn't need any logic

## Reference Implementation

The in-process mock implementations are in melosys-api:
- `integrasjonstest/src/test/kotlin/no/nav/melosys/melosysmock/kodeverk/KodeverkApi.kt`
- `integrasjonstest/src/test/kotlin/no/nav/melosys/melosysmock/inngang/InngangApi.kt`

## Why This Is Needed

With these endpoints added, the melosys-api integration tests can use the container for ALL external services, eliminating the need for the hybrid approach where some services use the container and others use the in-process mock.

This will allow us to eventually remove the in-process mock code from melosys-api entirely, simplifying the test architecture.

## Acceptance Criteria

1. `GET /api/v1/kodeverk/{kodeverkNavn}/koder/betydninger` returns valid FellesKodeverkDto
2. `POST /api/inngangsvilkaar` returns valid InngangsvilkarResponse
3. Both endpoints should be unauthenticated (no token required for test usage)
4. After implementing, the ContainerOpprettSakIT test should work with ALL services pointing to the container
