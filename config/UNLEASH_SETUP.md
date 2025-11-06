# Unleash Feature Toggle Setup

This document describes how to set up and use Unleash for feature toggles in local development.

## Overview

The application supports two modes for feature toggles in local development:

1. **Real Unleash Server** (Docker Compose) - Recommended for testing with a real toggle server
2. **LocalUnleash Fallback** - In-memory fake implementation when no Unleash server is configured

## DefaultEnabledUnleash

When running in local profiles (`!nais & !test`), the application automatically wraps the Unleash client with `DefaultEnabledUnleash`, which provides **default-enabled behavior** for unknown flags.

### Key Features

- **Transparent**: No code changes needed - just inject `Unleash` as usual
- **Unknown flags default to ENABLED**: When a toggle is not configured in Unleash, `isEnabled()` returns `true`
- **Respects explicit states**: When a toggle IS configured in Unleash, its actual state is used
- **Perfect for local development**: New features work immediately without manual Unleash configuration
- **Only in local profiles**: Production (NAIS) and test environments use standard Unleash behavior

### Usage

No changes needed! Just inject `Unleash` as usual:

```kotlin
@Service
class MyService(
    private val unleash: Unleash  // This is automatically DefaultEnabledUnleash in local dev
) {
    fun doSomething() {
        if (unleash.isEnabled(ToggleName.MY_NEW_FEATURE)) {
            // Returns true if not configured in Unleash (local dev only)
            // Returns actual state if configured in Unleash
        }
    }
}
```

## Running Unleash with Docker Compose

### 1. Start Unleash Server

The easiest way to run Unleash locally is with Docker Compose:

```bash
docker run -d \
  -e UNLEASH_PORT=4242 \
  -e UNLEASH_PROXY_SECRETS='[{"keys": ["*:development.unleash-insecure-client-api-token"], "type": "client"}]' \
  -p 4242:4242 \
  --name unleash \
  unleashorg/unleash-server:latest
```

Or use docker-compose.yml:

```yaml
version: '3.8'
services:
  unleash:
    image: unleashorg/unleash-server:latest
    ports:
      - "4242:4242"
    environment:
      - UNLEASH_PORT=4242
      - UNLEASH_PROXY_SECRETS=[{"keys": ["*:development.unleash-insecure-client-api-token"], "type": "client"}]
```

### 2. Configuration

The application is already configured to connect to Unleash at `http://localhost:4242/api` in `application-local.yml`:

```yaml
unleash:
  url: http://localhost:4242/api
  token: "*:development.unleash-insecure-client-api-token"
  app-name: melosys-api-local
  environment: development
```

### 3. Access Unleash Admin UI

Once running, access the Unleash admin interface at:

```
http://localhost:4242
```

Default credentials:
- Username: `admin`
- Password: `unleash4all`

### 4. How it Works

When the application starts with Unleash server configured (in local profiles):

1. `FeatureToggleConfigLocal` reads the `unleash.url` and `unleash.token` properties
2. If both are present, it creates a `DefaultUnleash` client connected to the server
3. The client is automatically wrapped with `DefaultEnabledUnleash`:
   - For toggles configured in Unleash → uses their actual state
   - For toggles NOT configured in Unleash → returns `true` (enabled by default)
4. This wrapper is **only active in local profiles** (`!nais & !test`)

## Fallback Mode (LocalUnleash)

If `unleash.url` or `unleash.token` are NOT configured:

1. `FeatureToggleConfigLocal` creates a `LocalUnleash` instance instead
2. `LocalUnleash` is an in-memory fake that enables all toggles except:
   - `MELOSYS_ÅRSAVREGNING_UTEN_FLYT` (explicitly disabled)

To use fallback mode, remove or comment out the `unleash.url` in `application-local.yml`.

## Configuring Toggles in Unleash

To add a new toggle in Unleash admin UI:

1. Navigate to http://localhost:4242
2. Click "New feature toggle"
3. Enter the toggle name (e.g., `melosys.my-new-feature`)
4. Add to `default` project
5. Enable/disable as needed
6. Add rollout strategies if desired (gradual rollout, by user ID, etc.)

## Best Practices

1. **Add constants to `ToggleName`**: Define all toggle names as constants in `ToggleName.kt`
2. **Use descriptive names**: Follow the convention `melosys.feature-name`
3. **Default to enabled in local**: Let `FeatureToggleService` default to enabled for rapid development
4. **Test both states**: Always test your feature with the toggle ON and OFF
5. **Document toggles**: Add comments explaining what each toggle controls

## Troubleshooting

### Unleash server not connecting

Check if the server is running:
```bash
curl http://localhost:4242/health
```

### All toggles returning true in local dev

This is expected behavior when toggles are not configured in Unleash and you're running in local profiles. To test disabled state, add the toggle in Unleash UI and disable it.

### Want standard Unleash behavior (unknown = disabled)?

The default-enabled behavior is **only active in local profiles** (`!nais & !test`). In production (NAIS) and test environments, the standard Unleash behavior applies (unknown toggles = disabled).

If you need standard behavior in local dev, remove or comment out the `unleash.url` configuration to use `LocalUnleash` fallback instead.
