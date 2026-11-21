#!/usr/bin/env bash
# Local integration smoke test; creates a real DB account, so use development DB only.
set -euo pipefail
command -v jq >/dev/null || { echo 'Install jq first: brew install jq'; exit 1; }
API="${API_URL:-http://localhost:8080/api/v1}"
EMAIL="sajha-smoke-$(date +%s)@example.test"
PASSWORD='DevTestPassword-123!'
PHONE='+13145550123'
BODY=$(jq -nc --arg email "$EMAIL" --arg pass "$PASSWORD" --arg phone "$PHONE" \
 '{name:"Smoke Tester",email:$email,password:$pass,phone:$phone}')
HEALTH=$(curl -fsS "$API/health")
test "$(jq -r .status <<<"$HEALTH")" = 'UP'
echo 'PASS: health'
REG=$(curl -fsS -H 'Content-Type: application/json' -d "$BODY" "$API/auth/register")
ACCESS=$(jq -r .accessToken <<<"$REG")
REFRESH=$(jq -r .refreshToken <<<"$REG")
test -n "$ACCESS"
test "$(jq -r .user.phoneVerified <<<"$REG")" = 'false'
echo 'PASS: registration returns unverified account'
LOGIN=$(curl -fsS -H 'Content-Type: application/json' -d "$(jq -nc --arg email "$EMAIL" --arg pass "$PASSWORD" '{email:$email,password:$pass}')" "$API/auth/login")
LOGIN_ACCESS=$(jq -r .accessToken <<<"$LOGIN")
LOGIN_REFRESH=$(jq -r .refreshToken <<<"$LOGIN")
test -n "$LOGIN_ACCESS"
echo 'PASS: credential login'
ME=$(curl -fsS -H "Authorization: Bearer $ACCESS" "$API/me")
test "$(jq -r .email <<<"$ME")" = "$EMAIL"
echo 'PASS: protected /me'
ROTATE=$(curl -fsS -H 'Content-Type: application/json' -d "$(jq -nc --arg t "$REFRESH" '{refreshToken:$t}')" "$API/auth/refresh")
NEXT_REFRESH=$(jq -r .refreshToken <<<"$ROTATE")
test "$REFRESH" != "$NEXT_REFRESH"
echo 'PASS: refresh rotates tokens'
STATUS=$(curl -s -o /dev/null -w '%{http_code}' -H 'Content-Type: application/json' -d "$(jq -nc --arg t "$REFRESH" '{refreshToken:$t}')" "$API/auth/refresh")
test "$STATUS" = '401'
echo 'PASS: old refresh token rejected'
STATUS=$(curl -s -o /dev/null -w '%{http_code}' "$API/me")
test "$STATUS" = '401'
echo 'PASS: guest /me blocked'
STATUS=$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $ACCESS" "$API/admin/business-requests")
test "$STATUS" = '403'
echo 'PASS: admin API not accidentally exposed'
STATUS=$(curl -s -o /dev/null -w '%{http_code}' -X POST -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $LOGIN_ACCESS" \
  -d "$(jq -nc --arg t "$LOGIN_REFRESH" '{refreshToken:$t}')" "$API/auth/logout")
test "$STATUS" = '204'
STATUS=$(curl -s -o /dev/null -w '%{http_code}' -X POST -H 'Content-Type: application/json' \
  -d "$(jq -nc --arg t "$LOGIN_REFRESH" '{refreshToken:$t}')" "$API/auth/refresh")
test "$STATUS" = '401'
echo 'PASS: logout revoked refresh token'
echo 'All smoke checks passed (OTP requires provider credentials and is tested separately).'
