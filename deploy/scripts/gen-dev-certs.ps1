# Generate self-signed certs for local HTTPS smoke (blog-ops-docker)
# Requires: openssl in PATH
$ErrorActionPreference = "Stop"
$sslDir = Join-Path $PSScriptRoot "..\nginx\ssl"
New-Item -ItemType Directory -Force -Path $sslDir | Out-Null

$key = Join-Path $sslDir "privkey.pem"
$crt = Join-Path $sslDir "fullchain.pem"
$cnf = Join-Path $sslDir "localhost.cnf"

@"
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
"@ | Set-Content -Path $cnf -Encoding ASCII

openssl req -x509 -nodes -newkey rsa:2048 -days 825 `
  -keyout $key -out $crt -config $cnf

Remove-Item $cnf -Force
Write-Host "Wrote:"
Write-Host "  $crt"
Write-Host "  $key"
Write-Host "Browsers will warn on self-signed certs; that is expected for local smoke."
