#!/bin/bash
#
# Henter miljøvariabler som er styrt av plattform-secrets via nais CLI.
# Følger https://doc.nais.io/services/secrets/how-to/get-platform-secret/
#
# Bruk:
#   get-azure-secrets.sh [flagg] [VARIABEL...]
#
# Eksempler:
#   get-azure-secrets.sh AZURE_APP_CLIENT_SECRET
#       -> printer kun verdien (rått), egnet for kommando-substitusjon
#
#   get-azure-secrets.sh AZURE_APP_CLIENT_ID AZURE_APP_CLIENT_SECRET MELOSYSDB_PASSWORD
#       -> printer NAVN=verdi per linje
#
#   get-azure-secrets.sh --list
#       -> lister alle tilgjengelige variabelnavn og hvilken secret de kommer fra
#
# Flagg:
#   -a, --app <navn>          app-navn på nais (default: melosys)
#   -e, --environment <miljø> nais-miljø (default: dev-fss)
#   -t, --team <team>         nais-team (default: teammelosys)
#   -r, --reason <tekst>      begrunnelse for audit-logg
#       --export              skriv "export NAVN='verdi'" (shell-sikker quoting)
#   -l, --list                list tilgjengelige variabler, hent ingen verdier
#   -d, --debug               skriv diagnostikk til stderr (aldri hemmelige verdier)
#   -h, --help                vis denne hjelpeteksten
#
# Env-variabler (brukes som default, overstyres av flagg):
#   NAIS_APP_NAME, NAIS_ENVIRONMENT, NAIS_TEAM, NAIS_SECRET_REASON
#   AZURE_ENV_VAR_NAME  - bakoverkompatibelt: variabelnavn hvis ingen er oppgitt
#   NAIS_SECRET_DEBUG=1 - samme som --debug
#
set -uo pipefail

APP_NAME="${NAIS_APP_NAME:-melosys}"
ENVIRONMENT="${NAIS_ENVIRONMENT:-dev-fss}"
TEAM="${NAIS_TEAM:-teammelosys}"
REASON="${NAIS_SECRET_REASON:-Lokal utvikling av melosys-api, kjørt $(date '+%d.%m.%Y %H:%M'). Verdiene brukes kun lokalt av utvikler.}"
DEBUG="${NAIS_SECRET_DEBUG:-0}"
LIST_ONLY=0
EXPORT_FORMAT=0
VAR_NAMES=()

usage() {
    sed -n '3,32p' "$0" | sed 's/^# \{0,1\}//'
}

debug() {
    [ "$DEBUG" = "1" ] && printf '[debug] %s\n' "$*" >&2
    return 0
}

die() {
    printf '%s\n' "$*" >&2
    exit 1
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        -a|--app)         APP_NAME="${2:-}"; shift 2 ;;
        -e|--environment) ENVIRONMENT="${2:-}"; shift 2 ;;
        -t|--team)        TEAM="${2:-}"; shift 2 ;;
        -r|--reason)      REASON="${2:-}"; shift 2 ;;
        --export)         EXPORT_FORMAT=1; shift ;;
        -l|--list)        LIST_ONLY=1; shift ;;
        -d|--debug)       DEBUG=1; shift ;;
        -h|--help)        usage; exit 0 ;;
        --)               shift; while [ "$#" -gt 0 ]; do VAR_NAMES+=("$1"); shift; done ;;
        -*)               die "Ukjent flagg: $1 (se --help)" ;;
        *)                VAR_NAMES+=("$1"); shift ;;
    esac
done

if [ "${#VAR_NAMES[@]}" -eq 0 ] && [ "$LIST_ONLY" -eq 0 ]; then
    VAR_NAMES=("${AZURE_ENV_VAR_NAME:-AZURE_APP_CLIENT_SECRET}")
fi

command -v nais >/dev/null 2>&1 || die "nais CLI er ikke installert, se https://cli.nais.io"
command -v jq   >/dev/null 2>&1 || die "jq er ikke installert, se https://jqlang.org"

# nais-versjoner varierer i om de støtter --team. Prøv med, fall tilbake uten.
run_nais() {
    local out
    if out=$("$@" --team "$TEAM" 2>/dev/null) && [ -n "$out" ]; then
        printf '%s' "$out"
        return 0
    fi
    debug "kall med --team $TEAM feilet, prøver uten --team"
    out=$("$@" 2>&1) || { printf '%s' "$out"; return 1; }
    printf '%s' "$out"
}

debug "app=$APP_NAME environment=$ENVIRONMENT team=$TEAM"

APP_ENV_JSON=$(run_nais nais app env "$APP_NAME" --environment "$ENVIRONMENT" --output json) || {
    printf "'nais app env %s --environment %s' feilet. Er du innlogget ('nais login')? Riktig app-navn?\n" \
        "$APP_NAME" "$ENVIRONMENT" >&2
    [ "$DEBUG" = "1" ] && printf '[debug] %s\n' "$APP_ENV_JSON" >&2
    exit 1
}

printf '%s' "$APP_ENV_JSON" | jq -e . >/dev/null 2>&1 || {
    printf "Uventet svar fra 'nais app env' (ikke JSON). Sjekk app-navn '%s' og miljø '%s'.\n" \
        "$APP_NAME" "$ENVIRONMENT" >&2
    [ "$DEBUG" = "1" ] && printf '[debug] %s\n' "$APP_ENV_JSON" >&2
    exit 1
}

if [ "$DEBUG" = "1" ] || [ "$LIST_ONLY" -eq 1 ]; then
    {
        printf '\nTilgjengelige variabler for %s (%s):\n' "$APP_NAME" "$ENVIRONMENT"
        printf '%s' "$APP_ENV_JSON" \
            | jq -r '.[] | "  \(.name)\t<- \(.source.name // "inline")"' \
            | sort \
            | column -t -s $'\t' 2>/dev/null || printf '%s' "$APP_ENV_JSON" | jq -r '.[] | "  \(.name)"' | sort
        printf '\n'
    } >&2
fi

[ "$LIST_ONLY" -eq 1 ] && exit 0

# Cache for hentede secrets, slik at vi kun kaller 'nais secret get' én gang per secret
CACHE_DIR=$(mktemp -d) || die "Klarte ikke å opprette midlertidig katalog"
trap 'rm -rf "$CACHE_DIR"' EXIT

cache_path() {
    printf '%s/%s' "$CACHE_DIR" "$(printf '%s' "$1" | tr -c 'A-Za-z0-9_.-' '_')"
}

fetch_secret() {
    local secret_name="$1" path json
    path=$(cache_path "$secret_name")

    if [ ! -f "$path" ]; then
        json=$(run_nais nais secret get "$secret_name" \
            --environment "$ENVIRONMENT" \
            --with-values \
            --reason "$REASON" \
            --output json) || {
            # Rå output vises ikke, den kan i verste fall inneholde hemmelige verdier.
            printf "'nais secret get %s --environment %s' feilet.\n" "$secret_name" "$ENVIRONMENT" >&2
            [ "$DEBUG" = "1" ] && printf '[debug] %s\n' "$json" >&2
            return 1
        }
        printf '%s' "$json" > "$path"

        if [ "$DEBUG" = "1" ]; then
            printf '[debug] nøkler i secret %s:\n' "$secret_name" >&2
            printf '%s' "$json" | jq -r '
                [ .. | objects | (.key? // empty) ] as $k
                | if ($k | length) > 0 then $k[]
                  else (.data? | objects | keys[]? ) // empty end
            ' 2>/dev/null | sed 's/^/          /' >&2
        fi
    fi

    cat "$path"
}

# Leser én verdi ut av secret-JSON. Håndterer formene:
#   {"data":[{"key":..,"value":..,"encoding":..}]}, {"data":{"NAVN":"verdi"}}, [{"name":..,"value":..}]
extract_value() {
    local json="$1" name="$2" entry encoding value
    entry=$(printf '%s' "$json" | jq -r --arg name "$name" '
        [ ( .. | objects | select((.key? // .name?) == $name) | {value: (.value? // ""), encoding: (.encoding? // "")} ),
          ( .. | objects | select(has($name)) | .[$name] | strings | {value: ., encoding: ""} ) ]
        | map(select(.value | length > 0)) | first // empty
        | "\(.encoding)\t\(.value)"
    ')

    [ -z "$entry" ] && return 1

    encoding="${entry%%$'\t'*}"
    value="${entry#*$'\t'}"

    if [ "$encoding" = "base64" ]; then
        # GNU coreutils bruker -d, macOS/BSD bruker -D
        value=$(printf '%s' "$value" | base64 -d 2>/dev/null \
            || printf '%s' "$value" | base64 -D 2>/dev/null) || return 1
        [ -z "$value" ] && return 1
    fi

    printf '%s' "$value"
}

emit() {
    local name="$1" value="$2"
    if [ "${#VAR_NAMES[@]}" -eq 1 ] && [ "$EXPORT_FORMAT" -eq 0 ]; then
        printf '%s\n' "$value"
    elif [ "$EXPORT_FORMAT" -eq 1 ]; then
        printf "export %s='%s'\n" "$name" "${value//\'/\'\\\'\'}"
    else
        printf '%s=%s\n' "$name" "$value"
    fi
}

FAILED=0
for var_name in "${VAR_NAMES[@]}"; do
    secret_name=$(printf '%s' "$APP_ENV_JSON" \
        | jq -r --arg name "$var_name" '.[] | select(.name == $name) | .source.name // empty' \
        | head -n 1)

    if [ -z "$secret_name" ]; then
        # Ingen secret-referanse: verdien kan ligge inline i app env-outputen
        inline=$(printf '%s' "$APP_ENV_JSON" \
            | jq -r --arg name "$var_name" '.[] | select(.name == $name) | .value // empty' \
            | head -n 1)
        if [ -n "$inline" ]; then
            debug "$var_name hentet inline fra app env"
            emit "$var_name" "$inline"
            continue
        fi
        printf 'Fant ikke %s i app %s (%s). Kjør med --list for å se tilgjengelige variabler.\n' \
            "$var_name" "$APP_NAME" "$ENVIRONMENT" >&2
        FAILED=1
        continue
    fi

    debug "$var_name <- secret $secret_name"

    if ! secret_json=$(fetch_secret "$secret_name"); then
        FAILED=1
        continue
    fi

    if ! value=$(extract_value "$secret_json" "$var_name"); then
        printf 'Klarte ikke å lese %s ut av secret %s. Kjør på nytt med --debug for å se nøklene.\n' \
            "$var_name" "$secret_name" >&2
        FAILED=1
        continue
    fi

    emit "$var_name" "$value"
done

exit "$FAILED"
