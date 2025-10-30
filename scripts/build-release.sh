#!/bin/bash

# Ghana Voice Ledger - Release Build Script
# This script builds signed release APKs and AABs for all flavors
# 
# USAGE: ./scripts/build-release.sh [flavor] [build-type]
# 
# ARGUMENTS:
#   flavor: dev, staging, prod (default: prod)
#   build-type: apk, aab, all (default: all)
#
# REQUIREMENTS:
# - keystore.properties must be configured with signing credentials
# - Android SDK and build tools must be installed

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
FLAVOR=${1:-prod}
BUILD_TYPE=${2:-all}
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="$PROJECT_DIR/app/build/outputs"

echo -e "${BLUE}=== Ghana Voice Ledger Release Build ===${NC}"
echo -e "${BLUE}Project: ${PROJECT_DIR}${NC}"
echo -e "${BLUE}Flavor: ${FLAVOR}${NC}"
echo -e "${BLUE}Build Type: ${BUILD_TYPE}${NC}"
echo

# Check if keystore.properties exists
if [ ! -f "$PROJECT_DIR/keystore.properties" ]; then
    echo -e "${RED}✗ keystore.properties not found!${NC}"
    echo -e "${YELLOW}Please copy keystore.properties.example to keystore.properties and configure your signing credentials.${NC}"
    exit 1
fi

# Check if keystore file exists
KEYSTORE_FILE=$(grep "RELEASE_KEYSTORE_FILE" "$PROJECT_DIR/keystore.properties" | cut -d'=' -f2)
if [ ! -f "$PROJECT_DIR/$KEYSTORE_FILE" ] && [ ! -f "$KEYSTORE_FILE" ]; then
    echo -e "${RED}✗ Keystore file not found: $KEYSTORE_FILE${NC}"
    echo -e "${YELLOW}Please ensure your keystore file exists and the path in keystore.properties is correct.${NC}"
    exit 1
fi

# Clean previous builds
echo -e "${BLUE}Cleaning previous builds...${NC}"
cd "$PROJECT_DIR"
./gradlew clean

# Build based on type
case $BUILD_TYPE in
    "apk")
        echo -e "${BLUE}Building release APK for ${FLAVOR} flavor...${NC}"
        ./gradlew assemble${FLAVOR^}Release
        echo -e "${GREEN}✓ APK build completed!${NC}"
        ;;
    "aab")
        echo -e "${BLUE}Building release AAB for ${FLAVOR} flavor...${NC}"
        ./gradlew bundle${FLAVOR^}Release
        echo -e "${GREEN}✓ AAB build completed!${NC}"
        ;;
    "all")
        echo -e "${BLUE}Building release APK and AAB for ${FLAVOR} flavor...${NC}"
        ./gradlew assemble${FLAVOR^}Release bundle${FLAVOR^}Release
        echo -e "${GREEN}✓ APK and AAB builds completed!${NC}"
        ;;
    *)
        echo -e "${RED}✗ Invalid build type: $BUILD_TYPE${NC}"
        echo -e "${YELLOW}Valid options: apk, aab, all${NC}"
        exit 1
        ;;
esac

# Display build artifacts
echo
echo -e "${BLUE}=== Build Artifacts ===${NC}"

APK_DIR="$BUILD_DIR/apk/${FLAVOR}/release"
AAB_DIR="$BUILD_DIR/bundle/${FLAVOR}/release"

if [ -d "$APK_DIR" ] && [ "$(ls -A $APK_DIR 2>/dev/null)" ]; then
    echo -e "${GREEN}APK files:${NC}"
    find "$APK_DIR" -name "*.apk" -exec basename {} \; | while read apk; do
        echo -e "  ${GREEN}✓${NC} $apk"
        echo -e "    Location: $APK_DIR/$apk"
    done
fi

if [ -d "$AAB_DIR" ] && [ "$(ls -A $AAB_DIR 2>/dev/null)" ]; then
    echo -e "${GREEN}AAB files:${NC}"
    find "$AAB_DIR" -name "*.aab" -exec basename {} \; | while read aab; do
        echo -e "  ${GREEN}✓${NC} $aab"
        echo -e "    Location: $AAB_DIR/$aab"
    done
fi

# Display build information
echo
echo -e "${BLUE}=== Build Information ===${NC}"
echo -e "${BLUE}Build Date:${NC} $(date)"
echo -e "${BLUE}Git Commit:${NC} $(git rev-parse --short HEAD 2>/dev/null || echo 'N/A')"
echo -e "${BLUE}Git Branch:${NC} $(git branch --show-current 2>/dev/null || echo 'N/A')"

# Show file sizes
echo
echo -e "${BLUE}=== File Sizes ===${NC}"
if [ -d "$APK_DIR" ]; then
    find "$APK_DIR" -name "*.apk" -exec ls -lh {} \; | awk '{print $5 " " $9}'
fi
if [ -d "$AAB_DIR" ]; then
    find "$AAB_DIR" -name "*.aab" -exec ls -lh {} \; | awk '{print $5 " $9}'
fi

echo
echo -e "${GREEN}=== Build Complete! ===${NC}"
echo -e "${YELLOW}Next steps:${NC}"
echo -e "1. Test the APK on a physical device"
echo -e "2. Upload AAB to Google Play Console for distribution"
echo -e "3. Verify app signing information with: apksigner verify"
echo
echo -e "${BLUE}For testing:${NC}"
echo -e "  adb install -r $BUILD_DIR/apk/${FLAVOR}/release/*.apk"