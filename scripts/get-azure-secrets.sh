#!/bin/bash
#
# Henter AZURE_APP_CLIENT_SECRET for en app via nais CLI.
# Følger https://doc.nais.io/services/secrets/how-to/get-platform-secret/
#
# Bruk: get-azure-secrets.sh <app-navn> [miljø]
#
# Env-variabler:
#   AZURE_ENV_VAR_NAME  - miljøvariabelen som skal hentes (default AZURE_APP_CLIENT_SECRET)
#   NAIS_SECRET_REASON  - begrunnelse for audit-logg
#   NAIS_SECRET_DEBUG   - sett til 1 for å dumpe JSON-strukturen til stderr
#
set -uo pipefail

if [ "$#" -lt 1 ]; then
    echo "Usage: $0 <app-name> [environment]" >&2
    exit 1
fi

APP_NAME="$1"
ENVIRONMENT="${2:-${NAIS_ENVIRONMENT:-dev-fss}}"
ENV_VAR_NAME="${AZURE_ENV_VAR_NAME:-AZURE_APP_CLIENT_SECRET}"
REASON="${NAIS_SECRET_REASON:-Lokal utvikling av melosys-api}"

command -v nais >/dev/null 2>&1 || { echo "nais CLI er ikke installert, se https://cli.nais.io" >&2; exit 1; }
command -v jq >/dev/null 2>&1 || { echo "jq er ikke installert, se https://jqlang.org" >&2; exit 1; }

# 1. Finn navnet på secreten som inneholder miljøvariabelen
APP_ENV_JSON=$(nais app env "$APP_NAME" --environment "$ENVIRONMENT" --output json) || {
    echo "'nais app env $APP_NAME --environment $ENVIRONMENT' feilet. Er du innlogget ('nais login')?" >&2
    exit 1
}

SECRET_NAME=$(printf '%s' "$APP_ENV_JSON" \
    | jq -r --arg name "$ENV_VAR_NAME" '.[] | select(.name == $name) | .source.name // empty' \
    | head -n 1)

if [ -z "$SECRET_NAME" ] || [ "$SECRET_NAME" = "null" ]; then
    echo "Fant ikke secret for $ENV_VAR_NAME i app $APP_NAME ($ENVIRONMENT)." >&2
    echo "Tilgjengelige variabler:" >&2
    printf '%s' "$APP_ENV_JSON" | jq -r '.[].name' >&2
    exit 1
fi

# 2. Hent verdien fra secreten
SECRET_JSON=$(nais secret get "$SECRET_NAME" \
    --environment "$ENVIRONMENT" \
    --with-values \
    --reason "$REASON" \
    --output json) || {
    echo "'nais secret get $SECRET_NAME --environment $ENVIRONMENT' feilet." >&2
    exit 1
}

if [ "${NAIS_SECRET_DEBUG:-0}" = "1" ]; then
    printf '%s' "$SECRET_JSON" | jq -r 'paths(scalars) | join(".")' >&2
fi

# Uttrekk. nais secret get returnerer:
#   {"name":..., "environment":..., "data":[{"key":..,"value":..,"encoding":..}], "lastModified":..}
# Eldre/andre former ({"data":{"NAVN":"verdi"}} eller [{"name":..,"value":..}]) håndteres også.
ENTRY=$(printf '%s' "$SECRET_JSON" | jq -r --arg name "$ENV_VAR_NAME" '
    [ ( .. | objects | select((.key? // .name?) == $name) | {value: (.value? // ""), encoding: (.encoding? // "")} ),
      ( .. | objects | select(has($name)) | .[$name] | strings | {value: ., encoding: ""} ) ]
    | map(select(.value | length > 0)) | first // empty
    | "\(.encoding)\t\(.value)"
')

if [ -z "$ENTRY" ]; then
    echo "Klarte ikke å lese $ENV_VAR_NAME ut av secret $SECRET_NAME." >&2
    echo "Kjør på nytt med NAIS_SECRET_DEBUG=1 for å se JSON-strukturen." >&2
    exit 1
fi

ENCODING="${ENTRY%%$'\t'*}"
VALUE="${ENTRY#*$'\t'}"

if [ "$ENCODING" = "base64" ]; then
    VALUE_DECODED=$(printf '%s' "$VALUE" | base64 -d 2>/dev/null || printf '%s' "$VALUE" | base64 -D 2>/dev/null) || {
        echo "Base64-dekoding feilet for $ENV_VAR_NAME i secret $SECRET_NAME." >&2
        exit 1
    }
    VALUE="$VALUE_DECODED"
    if [ -z "$VALUE" ]; then
        echo "Base64-dekoding ga tom verdi for $ENV_VAR_NAME i secret $SECRET_NAME." >&2
        exit 1
    fi
fi

printf '%s\n' "$VALUE"
