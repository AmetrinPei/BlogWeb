#!/usr/bin/env bash
# Generate self-signed certs for local HTTPS smoke (blog-ops-docker)
set -euo pipefail
SSL_DIR="$(cd "$(dirname "$0")/../nginx/ssl" && pwd)"
mkdir -p "$SSL_DIR"
CNF="$SSL_DIR/localhost.cnf"
cat >"$CNF" <<'EOF'
[req]
default_bits = 2048
prompt = no
default_md = sha256
distinguished_name = dn
x509_extensions = v3_req

[dn]
CN = localhost

[v3_req]
subjectAltName = @alt_names
basicConstraints = CA:FALSE
keyUsage = digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth

[alt_names]
DNS.1 = localhost
IP.1 = 127.0.0.1
EOF

openssl req -x509 -nodes -newkey rsa:2048 -days 825 \
  -keyout "$SSL_DIR/privkey.pem" \
  -out "$SSL_DIR/fullchain.pem" \
  -config "$CNF"

rm -f "$CNF"
echo "Wrote $SSL_DIR/fullchain.pem and privkey.pem"
