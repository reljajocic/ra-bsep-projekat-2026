#!/bin/bash

# Skripta za popunjavanje baze testnim podacima preko REST API-ja.
# Pokrece se dok backend radi na https://localhost:8443
# Koristi -k jer je sertifikat self-signed.

BASE_URL="https://localhost:8443/api"

echo "=== BSEP seed skripta ==="
echo ""

# 1. Login kao admin
echo "Prijava kao admin..."
TOKEN=$(curl -k -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@bsep.com","password":"Admin1234!"}' \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

if [ -z "$TOKEN" ]; then
  echo "GRESKA: Nije moguce prijaviti se. Da li backend radi?"
  exit 1
fi
echo "Token dobijen."
echo ""

AUTH="Authorization: Bearer $TOKEN"

# 2. ROOT sertifikat (vazi 10 godina)
echo "Kreiranje ROOT sertifikata..."
ROOT_ID=$(curl -k -s -X POST "$BASE_URL/certificates" \
  -H "Content-Type: application/json" -H "$AUTH" \
  -d '{
    "certificateType":"ROOT",
    "subjectCN":"BSEP Root CA",
    "subjectO":"FTN",
    "subjectOU":"Katedra za informatiku",
    "subjectC":"RS",
    "validityDays":3650,
    "keyUsage":"keyCertSign,cRLSign",
    "issuerCertificateId":null
  }' | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
echo "ROOT kreiran, id=$ROOT_ID"

# 3. INTERMEDIATE sertifikat (vazi 5 godina)
echo "Kreiranje INTERMEDIATE sertifikata..."
INT_ID=$(curl -k -s -X POST "$BASE_URL/certificates" \
  -H "Content-Type: application/json" -H "$AUTH" \
  -d "{
    \"certificateType\":\"INTERMEDIATE\",
    \"subjectCN\":\"BSEP Intermediate CA\",
    \"subjectO\":\"FTN\",
    \"subjectOU\":\"IT\",
    \"subjectC\":\"RS\",
    \"validityDays\":1825,
    \"keyUsage\":\"keyCertSign,cRLSign\",
    \"issuerCertificateId\":$ROOT_ID
  }" | sed -n 's/.*"id":\([0-9]*\).*/\1/p')
echo "INTERMEDIATE kreiran, id=$INT_ID"

# 4. Dva END_ENTITY sertifikata (vaze 1 godinu)
echo "Kreiranje END_ENTITY sertifikata (server.ftn.com)..."
curl -k -s -X POST "$BASE_URL/certificates" \
  -H "Content-Type: application/json" -H "$AUTH" \
  -d "{
    \"certificateType\":\"END_ENTITY\",
    \"subjectCN\":\"server.ftn.com\",
    \"subjectO\":\"FTN\",
    \"subjectOU\":\"IT\",
    \"subjectC\":\"RS\",
    \"validityDays\":365,
    \"keyUsage\":\"digitalSignature,keyEncipherment\",
    \"extendedKeyUsage\":\"serverAuth\",
    \"issuerCertificateId\":$INT_ID
  }" > /dev/null
echo "END_ENTITY 1 kreiran."

echo "Kreiranje END_ENTITY sertifikata (client.ftn.com)..."
curl -k -s -X POST "$BASE_URL/certificates" \
  -H "Content-Type: application/json" -H "$AUTH" \
  -d "{
    \"certificateType\":\"END_ENTITY\",
    \"subjectCN\":\"client.ftn.com\",
    \"subjectO\":\"FTN\",
    \"subjectOU\":\"IT\",
    \"subjectC\":\"RS\",
    \"validityDays\":365,
    \"keyUsage\":\"digitalSignature,keyEncipherment\",
    \"extendedKeyUsage\":\"clientAuth\",
    \"issuerCertificateId\":$INT_ID
  }" > /dev/null
echo "END_ENTITY 2 kreiran."
echo ""

echo "=== Gotovo! Baza je popunjena testnim sertifikatima. ==="
echo "Napomena: password manager unosi se ne mogu seed-ovati skriptom"
echo "jer se lozinke enkriptuju u browseru tvojim RSA javnim kljucem."
