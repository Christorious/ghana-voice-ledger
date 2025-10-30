#!/bin/bash

# Ghana Voice Ledger - Keystore Generation Script
# This script generates a release keystore for signing the Android app
# 
# USAGE: ./scripts/generate-keystore.sh
# 
# IMPORTANT: 
# - Store the generated keystore and passwords securely
# - Never commit keystore files or passwords to version control
# - Use a password manager for secure storage
# - Keep backup copies in secure locations

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
KEYSTORE_DIR="keystore"
KEYSTORE_NAME="ghana-voice-ledger-release"
KEY_ALIAS="ghana-voice-ledger-key"
VALIDITY_YEARS=25

# Create keystore directory if it doesn't exist
mkdir -p "$KEYSTORE_DIR"

echo -e "${BLUE}=== Ghana Voice Ledger Keystore Generation ===${NC}"
echo -e "${YELLOW}This will generate a release keystore for signing the Android app${NC}"
echo
echo -e "${BLUE}Configuration:${NC}"
echo -e "  Keystore Directory: ${KEYSTORE_DIR}"
echo -e "  Keystore Name: ${KEYSTORE_NAME}.jks"
echo -e "  Key Alias: ${KEY_ALIAS}"
echo -e "  Validity Period: ${VALIDITY_YEARS} years"
echo

# Confirm before proceeding
read -p "Do you want to continue? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${RED}Keystore generation cancelled.${NC}"
    exit 1
fi

echo -e "${BLUE}Generating keystore...${NC}"

# Generate the keystore
keytool -genkeypair \
    -v \
    -storetype JKS \
    -keystore "${KEYSTORE_DIR}/${KEYSTORE_NAME}.jks" \
    -keyalg RSA \
    -keysize 2048 \
    -validity $((VALIDITY_YEARS * 365)) \
    -alias "$KEY_ALIAS" \
    -dname "CN=Ghana Voice Ledger, OU=Development, O=Voice Ledger, L=Accra, ST=Greater Accra, C=GH" \
    -keypass:env \
    -storepass:env

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Keystore generated successfully!${NC}"
    echo
    echo -e "${BLUE}Next steps:${NC}"
    echo -e "1. Copy keystore.properties.example to keystore.properties"
    echo -e "2. Fill in your keystore credentials in keystore.properties"
    echo -e "3. Add keystore.properties to .gitignore (already done)"
    echo -e "4. Store your keystore and passwords securely"
    echo
    echo -e "${YELLOW}IMPORTANT SECURITY NOTES:${NC}"
    echo -e "- Store the keystore file in a secure location"
    echo -e "- Use a strong, unique password"
    echo -e "- Keep backup copies in different secure locations"
    echo -e "- Never commit keystore files or passwords to Git"
    echo -e "- Consider using a password manager"
    echo
    echo -e "${BLUE}Keystore location: ${KEYSTORE_DIR}/${KEYSTORE_NAME}.jks${NC}"
else
    echo -e "${RED}✗ Keystore generation failed!${NC}"
    exit 1
fi