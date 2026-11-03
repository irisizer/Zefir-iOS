#!/bin/bash
# Подпись unsigned Zefir.ipa профилем Z24Z5L (team 4P2MX93T2D).
# Использование: ./sign.sh <unsigned.ipa> <signed.ipa>
set -euo pipefail

CERT=/root/Z24Z5L.p12
PROV=/root/Z24Z5L.mobileprovision
PASS=1
ENT=/root/zefir2/signing/entitlements.plist
ZSIGN=/opt/zsign/bin/zsign

IN="${1:?input ipa}"
OUT="${2:?output ipa}"

"$ZSIGN" -c "$CERT" -p "$PASS" -m "$PROV" -e "$ENT" -o "$OUT" "$IN"

# Проверка структуры подписанного бандла
TMP=$(mktemp -d)
unzip -q "$OUT" -d "$TMP"
echo "== bundle structure =="
ls "$TMP/Payload/Zefir.app" | head -20
echo "== embedded profile =="
ls -la "$TMP/Payload/Zefir.app/embedded.mobileprovision" || true
echo "== extension =="
ls "$TMP/Payload/Zefir.app/PlugIns/ZefirTunnel.appex/" | head -12
ls -la "$TMP/Payload/Zefir.app/PlugIns/ZefirTunnel.appex/embedded.mobileprovision" || true
echo "== frameworks =="
ls "$TMP/Payload/Zefir.app/PlugIns/ZefirTunnel.appex/Frameworks" 2>/dev/null || true
echo "== signatures present =="
ls -d "$TMP/Payload/Zefir.app/_CodeSignature" "$TMP/Payload/Zefir.app/PlugIns/ZefirTunnel.appex/_CodeSignature" 2>/dev/null || true
rm -rf "$TMP"
echo "== OK: $OUT =="
ls -la "$OUT"
