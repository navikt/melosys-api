#!/usr/bin/env bash
# Seeder lokalt API med eksempel-tekstblokker og brevmaler for utvikling.
# Krever:
#   - melosys.tekstblokker feature toggle aktivert
#   - Gyldig bearer-token i $MELOSYS_TOKEN (eller bruk get-azure-secrets.sh)
# Bruk: ./scripts/seed-tekstblokker.sh [base-url]
# Standard base-url: http://localhost:8080/melosys/api

set -euo pipefail

BASE_URL="${1:-http://localhost:8080/melosys/api}"
TOKEN="${MELOSYS_TOKEN:?MELOSYS_TOKEN må være satt}"

DIR="$(cd "$(dirname "$0")/.." && pwd)/dev-data"
DATA="${DIR}/tekstblokker-seed.json"

if [[ ! -f "$DATA" ]]; then
  echo "Fant ikke $DATA"
  exit 1
fi

ANTALL=$(jq 'length' "$DATA")
echo "Setter inn $ANTALL tekstblokker mot $BASE_URL ..."

jq -c '.[]' "$DATA" | while read -r blokk; do
  TITTEL=$(jq -r '.tittel' <<<"$blokk")
  echo "  - $TITTEL"
  curl -fsS -X POST "$BASE_URL/tekstblokker" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "$blokk" >/dev/null
done

echo "Ferdig."
