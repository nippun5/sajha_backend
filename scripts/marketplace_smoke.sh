#!/usr/bin/env bash
set -euo pipefail
# Requires: jq and existing Phase 4A backend. Works without verified phone for denial checks.
API="${SAJHA_API_URL:-http://localhost:8080/api/v1}"
echo 'Checking public listing browse...'
curl -fsS "$API/marketplace/listings?kind=product&page=0&size=5" | jq -e '.items | type=="array"' > /dev/null
echo 'PASS: public browsing'
code=$(curl -sS -o /dev/null -w '%{http_code}' -H 'Content-Type: application/json' \
  -d '{"kind":"product","title":"Desk lamp","description":"Works great","contactPhone":"+13145550123","location":"St Louis","details":{"price":18,"category":"Home","condition":"good"}}' \
  "$API/marketplace/listings")
[[ "$code" == 401 ]] || { echo "FAIL: anonymous create returned $code (expected 401)"; exit 1; }
echo 'PASS: guests cannot publish'
if [[ -z "${SAJHA_ACCESS_TOKEN:-}" ]]; then
  echo 'Set SAJHA_ACCESS_TOKEN to check authenticated create; verified account needed for 201.'
  exit 0
fi
VERIFIED_PHONE="${SAJHA_VERIFIED_PHONE:-+13145550123}"
body=$(jq -nc --arg p "$VERIFIED_PHONE" '{kind:"product",title:"Demo desk lamp",description:"Test listing for backend integration only",contactPhone:$p,location:"St Louis, Missouri",details:{price:18,category:"Home",condition:"good"},mediaIds:[]}')
status=$(curl -sS -o /tmp/sajha_listing_result.json -w '%{http_code}' \
  -H "Authorization: Bearer $SAJHA_ACCESS_TOKEN" -H 'Content-Type: application/json' \
  -H "Idempotency-Key: sample-$(date +%s)" \
  -d "$body" \
  "$API/marketplace/listings")
echo "Create result: HTTP $status"
if [[ "$status" == 201 ]]; then
  jq -e '.id and .contactPhone and (.status=="active")' /tmp/sajha_listing_result.json
elif [[ "$status" == 403 ]]; then
  jq -r '.error.code' /tmp/sajha_listing_result.json
  echo 'Expected for unverified account phone/contact.'
else
  cat /tmp/sajha_listing_result.json; exit 1
fi
