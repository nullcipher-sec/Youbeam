#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$ROOT/deploy/secrets"
ENV_FILE="$ROOT/deploy/.env"
FORCE=0
[[ "${1:-}" == "--force" ]] && FORCE=1

need() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "ERROR: '$1' not found in PATH. $2" >&2
    exit 1
  }
}
need openssl "Install it with your package manager (Git Bash ships with it)."
need tinkey  "Download it from https://developers.google.com/tink/tinkey-overview (needs Java 11+)."
need mkcert  "Download it from https://github.com/FiloSottile/mkcert/releases"

PUBLIC_IP="${PUBLIC_IP:-}"
if [[ -z "$PUBLIC_IP" && -f "$ENV_FILE" ]]; then
  PUBLIC_IP="$(grep -E '^PUBLIC_IP=' "$ENV_FILE" | tail -n1 | cut -d= -f2- | tr -d '"' | tr -d "'" || true)"
fi

umask 077
mkdir -p "$OUT"
chmod 700 "$OUT"

should_make() {
  [[ $FORCE -eq 1 ]] && return 0
  local f
  for f in "$@"; do [[ -f "$OUT/$f" ]] || return 0; done
  return 1
}

if should_make hpke_private.json hpke_public.json; then
  echo ">> HPKE X25519 (Tinkey)"
  rm -f "$OUT/hpke_private.json" "$OUT/hpke_public.json"
  tinkey create-keyset \
    --key-template DHKEM_X25519_HKDF_SHA256_HKDF_SHA256_AES_256_GCM \
    --out-format json \
    --out "$OUT/hpke_private.json"
  tinkey create-public-keyset \
    --in "$OUT/hpke_private.json" --in-format json \
    --out "$OUT/hpke_public.json" --out-format json
else
  echo "== HPKE already exists (use --force to regenerate)"
fi

if should_make jwt_private.pem jwt_public.pem; then
  echo ">> JWT ES256 (openssl, P-256 curve)"
  tmp="$(mktemp)"
  trap 'rm -f "$tmp"' EXIT
  openssl ecparam -name prime256v1 -genkey -noout -out "$tmp"
  openssl pkcs8 -topk8 -nocrypt -in "$tmp" -out "$OUT/jwt_private.pem"
  openssl ec -in "$OUT/jwt_private.pem" -pubout -out "$OUT/jwt_public.pem" 2>/dev/null
else
  echo "== JWT already exists (use --force to regenerate)"
fi

if should_make tls.crt tls.key; then
  echo ">> Local TLS (mkcert)"
  names=(localhost 127.0.0.1 ::1)
  [[ -n "$PUBLIC_IP" ]] && names+=("$PUBLIC_IP")
  mkcert -cert-file "$OUT/tls.crt" -key-file "$OUT/tls.key" "${names[@]}"
  echo "   If the browser does not trust the certificate, run once: mkcert -install"
else
  echo "== TLS already exists (use --force to regenerate)"
fi

chmod 644 "$OUT"/* 2>/dev/null || true

echo
echo "Done. Files in deploy/secrets/:"
ls -1 "$OUT"