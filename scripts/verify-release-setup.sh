#!/bin/bash

# Ghana Voice Ledger - Release Build Verification Script
# This script verifies that the release build configuration is properly set up
# 
# USAGE: ./scripts/verify-release-setup.sh

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ERRORS=0
WARNINGS=0

echo -e "${BLUE}=== Ghana Voice Ledger - Release Build Verification ===${NC}"
echo -e "${BLUE}Project Directory: ${PROJECT_DIR}${NC}"
echo

# Function to print success
print_success() {
    echo -e "  ${GREEN}✓${NC} $1"
}

# Function to print error
print_error() {
    echo -e "  ${RED}✗${NC} $1"
    ((ERRORS++))
}

# Function to print warning
print_warning() {
    echo -e "  ${YELLOW}⚠${NC} $1"
    ((WARNINGS++))
}

echo -e "${BLUE}Checking project structure...${NC}"

# Check if project structure is correct
if [ ! -f "$PROJECT_DIR/app/build.gradle.kts" ]; then
    print_error "app/build.gradle.kts not found"
else
    print_success "app/build.gradle.kts found"
fi

if [ ! -f "$PROJECT_DIR/app/proguard-rules.pro" ]; then
    print_error "app/proguard-rules.pro not found"
else
    print_success "app/proguard-rules.pro found"
fi

if [ ! -f "$PROJECT_DIR/keystore.properties.example" ]; then
    print_error "keystore.properties.example not found"
else
    print_success "keystore.properties.example found"
fi

echo
echo -e "${BLUE}Checking signing configuration...${NC}"

# Check if keystore.properties exists
if [ ! -f "$PROJECT_DIR/keystore.properties" ]; then
    print_warning "keystore.properties not found (required for signed builds)"
    echo -e "    ${YELLOW}Note: Copy keystore.properties.example to keystore.properties and configure credentials${NC}"
else
    print_success "keystore.properties found"
    
    # Check keystore.properties content
    if grep -q "your-keystore-file.jks" "$PROJECT_DIR/keystore.properties"; then
        print_warning "keystore.properties contains placeholder values"
    else
        print_success "keystore.properties appears to be configured"
        
        # Check if keystore file exists
        KEYSTORE_FILE=$(grep "RELEASE_KEYSTORE_FILE" "$PROJECT_DIR/keystore.properties" 2>/dev/null | cut -d'=' -f2 || echo "")
        if [ -n "$KEYSTORE_FILE" ]; then
            if [ -f "$PROJECT_DIR/$KEYSTORE_FILE" ] || [ -f "$KEYSTORE_FILE" ]; then
                print_success "Keystore file found: $KEYSTORE_FILE"
            else
                print_warning "Keystore file not found: $KEYSTORE_FILE"
            fi
        fi
    fi
fi

echo
echo -e "${BLUE}Checking .gitignore configuration...${NC}"

# Check if keystore files are ignored
if grep -q "keystore.properties" "$PROJECT_DIR/.gitignore"; then
    print_success "keystore.properties is in .gitignore"
else
    print_error "keystore.properties not found in .gitignore"
fi

if grep -q "*.jks" "$PROJECT_DIR/.gitignore"; then
    print_success "*.jks files are in .gitignore"
else
    print_error "*.jks files not found in .gitignore"
fi

if grep -q "*.keystore" "$PROJECT_DIR/.gitignore"; then
    print_success "*.keystore files are in .gitignore"
else
    print_error "*.keystore files not found in .gitignore"
fi

echo
echo -e "${BLUE}Checking build configuration...${NC}"

# Check if build.gradle.kts has signing configuration
if grep -q "signingConfigs" "$PROJECT_DIR/app/build.gradle.kts"; then
    print_success "Signing configuration found in build.gradle.kts"
else
    print_error "Signing configuration not found in build.gradle.kts"
fi

# Check if product flavors are configured
if grep -q "productFlavors" "$PROJECT_DIR/app/build.gradle.kts"; then
    print_success "Product flavors found in build.gradle.kts"
else
    print_error "Product flavors not found in build.gradle.kts"
fi

# Check if release build type is properly configured
if grep -q "isMinifyEnabled = true" "$PROJECT_DIR/app/build.gradle.kts"; then
    print_success "Release minification enabled"
else
    print_error "Release minification not enabled"
fi

if grep -q "isShrinkResources = true" "$PROJECT_DIR/app/build.gradle.kts"; then
    print_success "Resource shrinking enabled"
else
    print_error "Resource shrinking not enabled"
fi

echo
echo -e "${BLUE}Checking ProGuard rules...${NC}"

# Check essential ProGuard rules
RULES_FILE="$PROJECT_DIR/app/proguard-rules.pro"

if grep -q "androidx.room" "$RULES_FILE"; then
    print_success "Room ProGuard rules found"
else
    print_error "Room ProGuard rules not found"
fi

if grep -q "dagger.hilt" "$RULES_FILE"; then
    print_success "Hilt ProGuard rules found"
else
    print_error "Hilt ProGuard rules not found"
fi

if grep -q "org.tensorflow.lite" "$RULES_FILE"; then
    print_success "TensorFlow Lite ProGuard rules found"
else
    print_error "TensorFlow Lite ProGuard rules not found"
fi

if grep -q "androidx.compose" "$RULES_FILE"; then
    print_success "Compose ProGuard rules found"
else
    print_error "Compose ProGuard rules not found"
fi

echo
echo -e "${BLUE}Checking scripts...${NC}"

# Check if build scripts exist
if [ -f "$PROJECT_DIR/scripts/generate-keystore.sh" ]; then
    print_success "Keystore generation script found"
    if [ -x "$PROJECT_DIR/scripts/generate-keystore.sh" ]; then
        print_success "Keystore generation script is executable"
    else
        print_warning "Keystore generation script is not executable"
    fi
else
    print_error "Keystore generation script not found"
fi

if [ -f "$PROJECT_DIR/scripts/build-release.sh" ]; then
    print_success "Release build script found"
    if [ -x "$PROJECT_DIR/scripts/build-release.sh" ]; then
        print_success "Release build script is executable"
    else
        print_warning "Release build script is not executable"
    fi
else
    print_error "Release build script not found"
fi

echo
echo -e "${BLUE}Checking documentation...${NC}"

if [ -f "$PROJECT_DIR/RELEASE_BUILD_GUIDE.md" ]; then
    print_success "Release build guide found"
else
    print_error "Release build guide not found"
fi

echo
echo -e "${BLUE}Checking dependency configuration...${NC}"

# Check if core dependencies are properly configured
BUILD_GRADLE="$PROJECT_DIR/app/build.gradle.kts"

if grep -q "implementation.*firebase-bom" "$BUILD_GRADLE"; then
    print_success "Firebase dependencies are always included"
else
    print_error "Firebase dependencies are feature-gated"
fi

if grep -q "implementation.*appcenter" "$BUILD_GRADLE"; then
    print_success "App Center dependencies are always included"
else
    print_error "App Center dependencies are missing"
fi

if grep -q "implementation.*tensorflow" "$BUILD_GRADLE"; then
    print_success "TensorFlow Lite dependencies are always included"
else
    print_error "TensorFlow Lite dependencies are feature-gated"
fi

if grep -q "implementation.*sqlcipher" "$BUILD_GRADLE"; then
    print_success "SQLCipher dependencies are always included"
else
    print_error "SQLCipher dependencies are feature-gated"
fi

if grep -q "implementation.*androidx.security.crypto" "$BUILD_GRADLE"; then
    print_success "AndroidX Security Crypto dependencies are always included"
else
    print_error "AndroidX Security Crypto dependencies are feature-gated"
fi

# Check that WebRTC remains optional
if grep -q "webRtcEnabled.*webrtc" "$BUILD_GRADLE"; then
    print_success "WebRTC dependencies are properly feature-gated"
else
    print_warning "WebRTC dependencies might not be feature-gated"
fi

echo
echo -e "${BLUE}Verifying Gradle configuration...${NC}"

# Check if gradle wrapper exists
if [ -f "$PROJECT_DIR/gradlew" ]; then
    print_success "Gradle wrapper found"
    if [ -x "$PROJECT_DIR/gradlew" ]; then
        print_success "Gradle wrapper is executable"
    else
        print_warning "Gradle wrapper is not executable"
    fi
else
    print_error "Gradle wrapper not found"
fi

# Try to run gradle check
cd "$PROJECT_DIR"
echo -e "${BLUE}Running Gradle configuration check...${NC}"

# Check if Java is available
if ! command -v java &> /dev/null; then
    print_warning "Java not available - skipping Gradle configuration check"
    print_warning "Install Java JDK to verify Gradle configuration"
else
    if ./gradlew tasks --quiet > /dev/null 2>&1; then
        print_success "Gradle configuration is valid"
    else
        print_error "Gradle configuration has errors"
    fi
fi

echo
echo -e "${BLUE}=== Verification Summary ===${NC}"

if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✓ All critical checks passed!${NC}"
    if [ $WARNINGS -eq 0 ]; then
        echo -e "${GREEN}✓ No warnings detected${NC}"
        echo -e "${GREEN}✓ Release build setup is ready!${NC}"
    else
        echo -e "${YELLOW}⚠ $WARNINGS warning(s) detected${NC}"
        echo -e "${YELLOW}⚠ Review warnings before production builds${NC}"
    fi
else
    echo -e "${RED}✗ $ERRORS error(s) detected${NC}"
    echo -e "${RED}✗ Fix errors before building releases${NC}"
fi

echo
echo -e "${BLUE}Next steps:${NC}"
if [ $ERRORS -gt 0 ]; then
    echo -e "1. Fix the errors listed above"
    echo -e "2. Run this script again to verify"
elif [ $WARNINGS -gt 0 ]; then
    echo -e "1. Review and address warnings"
    echo -e "2. Generate keystore: ./scripts/generate-keystore.sh"
    echo -e "3. Configure keystore.properties"
else
    echo -e "1. Generate keystore: ./scripts/generate-keystore.sh"
    echo -e "2. Configure keystore.properties"
    echo -e "3. Build release: ./scripts/build-release.sh"
fi

exit $ERRORS