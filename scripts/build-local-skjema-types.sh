#!/usr/bin/env bash
# Tynn wrapper rundt melosys-skjema-api/scripts/publish-types-local.sh.
# Bygger types lokalt (i søsterrepoet), installerer til ~/.m2, og oppdaterer
# pom.xml så melosys-api plukker opp den lokale versjonen.
#
# Bruk:
#   ./scripts/build-local-skjema-types.sh                 # bygg + oppdater pom.xml
#   ./scripts/build-local-skjema-types.sh --no-pom-update # bygg uten å endre pom.xml
#   SKJEMA_API_DIR=/path/to/melosys-skjema-api ./scripts/build-local-skjema-types.sh

set -euo pipefail

UPDATE_POM=true
if [[ "${1:-}" == "--no-pom-update" ]]; then
    UPDATE_POM=false
fi

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
API_DIR="${SKJEMA_API_DIR:-$REPO_ROOT/../melosys-skjema-api}"
PUBLISH_SCRIPT="$API_DIR/scripts/publish-types-local.sh"

if [[ ! -d "$API_DIR" ]]; then
    echo "Fant ikke melosys-skjema-api på $API_DIR" >&2
    echo "Klon den, eller sett SKJEMA_API_DIR til riktig path:" >&2
    echo "  git clone https://github.com/navikt/melosys-skjema-api.git $API_DIR" >&2
    exit 1
fi

if [[ ! -x "$PUBLISH_SCRIPT" ]]; then
    echo "Fant ikke kjørbart $PUBLISH_SCRIPT" >&2
    echo "Sjekk at melosys-skjema-api er oppdatert (git pull) og at scriptet har +x." >&2
    exit 1
fi

POM="$REPO_ROOT/pom.xml"

echo "==> melosys-api: lokal types-bygging"
echo "    skjema-api: $API_DIR"
echo "    pom.xml:    $POM"
echo ""
echo "[wrap 1/2] Delegerer til $PUBLISH_SCRIPT..."
echo ""

# Produsent-scriptet skriver versjonen som siste stdout-linje, all annen output går til stderr.
VERSION="$("$PUBLISH_SCRIPT")"

if [[ -z "$VERSION" ]]; then
    echo "Tomt versjons-svar fra $PUBLISH_SCRIPT" >&2
    exit 1
fi

echo ""
if [[ "$UPDATE_POM" == false ]]; then
    echo "[wrap 2/2] Hopper over pom.xml-oppdatering (--no-pom-update)."
    echo ""
    echo "Sett selv i $POM:"
    echo "  <melosys-skjema-api-types.version>$VERSION</melosys-skjema-api-types.version>"
    exit 0
fi

echo "[wrap 2/2] Oppdaterer $POM til versjon $VERSION..."
sed -i.bak -E \
    "s|<melosys-skjema-api-types\.version>[^<]+</melosys-skjema-api-types\.version>|<melosys-skjema-api-types.version>$VERSION</melosys-skjema-api-types.version>|" \
    "$POM"
rm -f "$POM.bak"

echo ""
echo "Ferdig. pom.xml peker nå på lokal versjon $VERSION."
echo "Husk å revertere pom.xml-endringen før commit ('git checkout pom.xml')."
