#!/bin/bash

# ProGuard/R8 Verification Script
# This script verifies the ProGuard configuration is correct

set -e

echo "========================================="
echo "ProGuard/R8 Configuration Verification"
echo "========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if proguard-rules.pro exists
echo "1. Checking ProGuard configuration file..."
if [ -f "app/proguard-rules.pro" ]; then
    echo -e "${GREEN}✓${NC} ProGuard rules file found"
    echo "   Location: app/proguard-rules.pro"
    echo "   Size: $(wc -l < app/proguard-rules.pro) lines"
else
    echo -e "${RED}✗${NC} ProGuard rules file not found!"
    exit 1
fi
echo ""

# Check for syntax errors in ProGuard rules
echo "2. Checking ProGuard rules syntax..."
if grep -q "^-keep" app/proguard-rules.pro; then
    echo -e "${GREEN}✓${NC} ProGuard keep rules found"
    keep_count=$(grep -c "^-keep" app/proguard-rules.pro)
    echo "   Keep rules: $keep_count"
else
    echo -e "${RED}✗${NC} No keep rules found!"
    exit 1
fi
echo ""

# Verify critical rules are present
echo "3. Verifying critical library rules..."

check_rule() {
    local library=$1
    local pattern=$2
    if grep -q "$pattern" app/proguard-rules.pro; then
        echo -e "   ${GREEN}✓${NC} $library rules present"
        return 0
    else
        echo -e "   ${RED}✗${NC} $library rules MISSING"
        return 1
    fi
}

errors=0

# Check Room
check_rule "Room Database" "@androidx.room.Entity" || ((errors++))

# Check Hilt
check_rule "Hilt DI" "dagger.hilt" || ((errors++))

# Check TensorFlow Lite
check_rule "TensorFlow Lite" "org.tensorflow.lite" || ((errors++))

# Check App Center
check_rule "App Center" "com.microsoft.appcenter" || ((errors++))

# Check Compose
check_rule "Jetpack Compose" "androidx.compose" || ((errors++))

# Check WorkManager
check_rule "WorkManager" "androidx.work" || ((errors++))

# Check kotlinx.serialization
check_rule "kotlinx.serialization" "kotlinx.serialization" || ((errors++))

# Check Retrofit
check_rule "Retrofit" "retrofit2" || ((errors++))

# Check Gson
check_rule "Gson" "com.google.gson" || ((errors++))

# Check Enum preservation
check_rule "Enum classes" "enum \*" || ((errors++))

# Check Parcelable
check_rule "Parcelable" "android.os.Parcelable" || ((errors++))

# Check Security/Biometric
check_rule "Security/Biometric" "androidx.biometric" || ((errors++))

# Check SQLCipher
check_rule "SQLCipher" "net.sqlcipher" || ((errors++))

# Check Assisted Injection
check_rule "Assisted Injection" "dagger.assisted" || ((errors++))

echo ""

if [ $errors -eq 0 ]; then
    echo -e "${GREEN}✓${NC} All critical library rules are present"
else
    echo -e "${RED}✗${NC} $errors library rule(s) missing"
fi
echo ""

# Check if line numbers are preserved
echo "4. Checking debug information preservation..."
if grep -q "keepattributes.*SourceFile.*LineNumberTable" app/proguard-rules.pro; then
    echo -e "${GREEN}✓${NC} Line numbers preserved for crash reporting"
else
    echo -e "${YELLOW}⚠${NC} Line numbers not preserved (recommended for crash reporting)"
fi
echo ""

# Check optimization settings
echo "5. Checking optimization settings..."
if grep -q "optimizationpasses" app/proguard-rules.pro; then
    passes=$(grep "optimizationpasses" app/proguard-rules.pro | grep -oP '\d+')
    echo -e "${GREEN}✓${NC} Optimization enabled ($passes passes)"
else
    echo -e "${YELLOW}⚠${NC} No optimization settings specified"
fi
echo ""

# Check logging removal
echo "6. Checking logging removal rules..."
if grep -q "assumenosideeffects.*Log" app/proguard-rules.pro; then
    echo -e "${GREEN}✓${NC} Debug logging will be removed in release"
else
    echo -e "${YELLOW}⚠${NC} Debug logging may not be removed"
fi
echo ""

# Check build.gradle configuration
echo "7. Checking build.gradle configuration..."
if grep -q "isMinifyEnabled = true" app/build.gradle.kts; then
    echo -e "${GREEN}✓${NC} Minification enabled in release build"
else
    echo -e "${RED}✗${NC} Minification not enabled!"
    ((errors++))
fi

if grep -q "isShrinkResources = true" app/build.gradle.kts; then
    echo -e "${GREEN}✓${NC} Resource shrinking enabled"
else
    echo -e "${YELLOW}⚠${NC} Resource shrinking not enabled"
fi

if grep -q 'proguard-rules.pro' app/build.gradle.kts; then
    echo -e "${GREEN}✓${NC} ProGuard rules file referenced in build config"
else
    echo -e "${RED}✗${NC} ProGuard rules not referenced in build.gradle!"
    ((errors++))
fi
echo ""

# Summary
echo "========================================="
echo "Verification Summary"
echo "========================================="

if [ $errors -eq 0 ]; then
    echo -e "${GREEN}✓ All checks passed!${NC}"
    echo ""
    echo "ProGuard configuration is complete and ready for release builds."
    echo ""
    echo "Next steps:"
    echo "  1. Build release APK: ./gradlew assembleRelease"
    echo "  2. Test on device: Install and verify all features"
    echo "  3. Review mapping.txt: Check app/build/outputs/mapping/release/"
    echo "  4. Monitor crashes: Verify crash reports are deobfuscated"
    exit 0
else
    echo -e "${RED}✗ $errors error(s) found!${NC}"
    echo ""
    echo "Please fix the issues above before building a release."
    exit 1
fi
