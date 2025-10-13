#!/bin/bash

# Ghana Voice Ledger Deployment Script
# Usage: ./scripts/deploy.sh [environment] [build_type]
# Example: ./scripts/deploy.sh staging release

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BUILD_DIR="$PROJECT_ROOT/app/build/outputs"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Default values
ENVIRONMENT=${1:-debug}
BUILD_TYPE=${2:-debug}
FLAVOR="ghana"

# Validate environment
case $ENVIRONMENT in
    debug|staging|release)
        log_info "Deploying to $ENVIRONMENT environment"
        ;;
    *)
        log_error "Invalid environment: $ENVIRONMENT"
        log_info "Valid environments: debug, staging, release"
        exit 1
        ;;
esac

# Validate build type
case $BUILD_TYPE in
    debug|release)
        log_info "Building $BUILD_TYPE variant"
        ;;
    *)
        log_error "Invalid build type: $BUILD_TYPE"
        log_info "Valid build types: debug, release"
        exit 1
        ;;
esac

# Load environment variables
if [ -f "$PROJECT_ROOT/.env" ]; then
    log_info "Loading environment variables from .env"
    source "$PROJECT_ROOT/.env"
else
    log_warning ".env file not found, using default values"
fi

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if gradlew exists
    if [ ! -f "$PROJECT_ROOT/gradlew" ]; then
        log_error "gradlew not found in project root"
        exit 1
    fi
    
    # Make gradlew executable
    chmod +x "$PROJECT_ROOT/gradlew"
    
    # Check Java version
    if ! command -v java &> /dev/null; then
        log_error "Java not found. Please install Java 17 or higher"
        exit 1
    fi
    
    # Check Android SDK
    if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
        log_error "ANDROID_HOME or ANDROID_SDK_ROOT not set"
        exit 1
    fi
    
    # Check keystore for release builds
    if [ "$BUILD_TYPE" = "release" ]; then
        if [ -z "$KEYSTORE_PASSWORD" ] || [ -z "$KEY_ALIAS" ] || [ -z "$KEY_PASSWORD" ]; then
            log_error "Keystore credentials not set for release build"
            log_info "Please set KEYSTORE_PASSWORD, KEY_ALIAS, and KEY_PASSWORD"
            exit 1
        fi
        
        KEYSTORE_PATH="${KEYSTORE_PATH:-keystore/ghana-voice-ledger.jks}"
        if [ ! -f "$PROJECT_ROOT/$KEYSTORE_PATH" ]; then
            log_error "Keystore file not found: $KEYSTORE_PATH"
            exit 1
        fi
    fi
    
    log_success "Prerequisites check passed"
}

# Clean build directory
clean_build() {
    log_info "Cleaning build directory..."
    cd "$PROJECT_ROOT"
    ./gradlew clean
    log_success "Build directory cleaned"
}

# Run tests
run_tests() {
    log_info "Running tests..."
    cd "$PROJECT_ROOT"
    
    # Run unit tests
    ./gradlew test${FLAVOR^}${BUILD_TYPE^}UnitTest
    
    # Run lint checks
    ./gradlew lint${FLAVOR^}${BUILD_TYPE^}
    
    log_success "Tests completed successfully"
}

# Build application
build_app() {
    log_info "Building application..."
    cd "$PROJECT_ROOT"
    
    local gradle_task=""
    case $ENVIRONMENT in
        debug)
            gradle_task="assemble${FLAVOR^}Debug"
            ;;
        staging)
            gradle_task="assemble${FLAVOR^}Staging"
            ;;
        release)
            gradle_task="assemble${FLAVOR^}Release bundle${FLAVOR^}Release"
            ;;
    esac
    
    log_info "Running Gradle task: $gradle_task"
    ./gradlew $gradle_task --stacktrace
    
    log_success "Application built successfully"
}

# Verify build outputs
verify_build() {
    log_info "Verifying build outputs..."
    
    local apk_path=""
    local aab_path=""
    
    case $ENVIRONMENT in
        debug)
            apk_path="$BUILD_DIR/apk/$FLAVOR/debug"
            ;;
        staging)
            apk_path="$BUILD_DIR/apk/$FLAVOR/staging"
            ;;
        release)
            apk_path="$BUILD_DIR/apk/$FLAVOR/release"
            aab_path="$BUILD_DIR/bundle/${FLAVOR}Release"
            ;;
    esac
    
    # Check APK
    if [ -d "$apk_path" ]; then
        local apk_files=$(find "$apk_path" -name "*.apk" | wc -l)
        if [ $apk_files -gt 0 ]; then
            log_success "APK files found: $apk_files"
            find "$apk_path" -name "*.apk" -exec ls -lh {} \;
        else
            log_error "No APK files found in $apk_path"
            exit 1
        fi
    fi
    
    # Check AAB (for release builds)
    if [ "$ENVIRONMENT" = "release" ] && [ -d "$aab_path" ]; then
        local aab_files=$(find "$aab_path" -name "*.aab" | wc -l)
        if [ $aab_files -gt 0 ]; then
            log_success "AAB files found: $aab_files"
            find "$aab_path" -name "*.aab" -exec ls -lh {} \;
        else
            log_error "No AAB files found in $aab_path"
            exit 1
        fi
    fi
}

# Deploy to Google Play (for release builds)
deploy_to_play_store() {
    if [ "$ENVIRONMENT" != "release" ]; then
        return 0
    fi
    
    log_info "Deploying to Google Play Store..."
    
    # Check if Google Play credentials are available
    if [ -z "$GOOGLE_PLAY_SERVICE_ACCOUNT_JSON" ]; then
        log_warning "Google Play service account not configured, skipping Play Store deployment"
        return 0
    fi
    
    # This would typically use a tool like fastlane or gradle-play-publisher
    log_info "Google Play deployment would be handled by CI/CD pipeline"
    log_success "Deployment configuration verified"
}

# Generate deployment report
generate_report() {
    log_info "Generating deployment report..."
    
    local report_file="$PROJECT_ROOT/deployment-report-$(date +%Y%m%d-%H%M%S).txt"
    
    cat > "$report_file" << EOF
Ghana Voice Ledger Deployment Report
===================================

Deployment Date: $(date)
Environment: $ENVIRONMENT
Build Type: $BUILD_TYPE
Flavor: $FLAVOR

Build Information:
- Git Commit: $(git rev-parse HEAD 2>/dev/null || echo "N/A")
- Git Branch: $(git branch --show-current 2>/dev/null || echo "N/A")
- Build Host: $(hostname)
- Build User: $(whoami)

Build Outputs:
EOF
    
    # Add APK information
    find "$BUILD_DIR" -name "*.apk" -exec echo "APK: {}" \; -exec ls -lh {} \; >> "$report_file" 2>/dev/null || true
    
    # Add AAB information
    find "$BUILD_DIR" -name "*.aab" -exec echo "AAB: {}" \; -exec ls -lh {} \; >> "$report_file" 2>/dev/null || true
    
    log_success "Deployment report generated: $report_file"
}

# Cleanup function
cleanup() {
    log_info "Performing cleanup..."
    # Add any cleanup tasks here
    log_success "Cleanup completed"
}

# Main deployment function
main() {
    log_info "Starting Ghana Voice Ledger deployment"
    log_info "Environment: $ENVIRONMENT, Build Type: $BUILD_TYPE"
    
    # Set trap for cleanup on exit
    trap cleanup EXIT
    
    # Run deployment steps
    check_prerequisites
    clean_build
    run_tests
    build_app
    verify_build
    deploy_to_play_store
    generate_report
    
    log_success "Deployment completed successfully!"
    log_info "Build outputs available in: $BUILD_DIR"
}

# Run main function
main "$@"