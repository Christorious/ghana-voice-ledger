# Ghana Voice Ledger - Deployment Guide

This document provides comprehensive instructions for deploying the Ghana Voice Ledger application across different environments.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Environment Configuration](#environment-configuration)
3. [Build Configuration](#build-configuration)
4. [Deployment Environments](#deployment-environments)
5. [CI/CD Pipeline](#cicd-pipeline)
6. [Docker Deployment](#docker-deployment)
7. [Manual Deployment](#manual-deployment)
8. [Troubleshooting](#troubleshooting)

## Prerequisites

### Development Environment
- **Java Development Kit (JDK)**: Version 17 or higher
- **Android SDK**: API level 34 with build tools 34.0.0
- **Gradle**: Version 8.5 or higher (included via wrapper)
- **Git**: For version control
- **Docker**: For containerized builds (optional)

### Required Tools
```bash
# Verify Java installation
java -version

# Verify Android SDK
echo $ANDROID_HOME

# Verify Gradle
./gradlew --version
```

### Signing Configuration
For release builds, you need:
- Keystore file (`keystore/ghana-voice-ledger.jks`)
- Keystore password
- Key alias
- Key password

## Environment Configuration

### 1. Environment Variables
Copy `.env.example` to `.env` and configure:

```bash
cp .env.example .env
```

Key variables to configure:
```bash
# Build Configuration
KEYSTORE_PASSWORD=your_keystore_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password

# API Endpoints
API_BASE_URL_DEV=https://api-dev.voiceledger.com
API_BASE_URL_STAGING=https://api-staging.voiceledger.com
API_BASE_URL_PROD=https://api.voiceledger.com

# Firebase Projects
FIREBASE_PROJECT_ID_DEV=ghana-voice-ledger-dev
FIREBASE_PROJECT_ID_STAGING=ghana-voice-ledger-staging
FIREBASE_PROJECT_ID_PROD=ghana-voice-ledger-prod
```

### 2. Firebase Configuration
Place Firebase configuration files in the appropriate directories:
- `app/google-services.json` (for debug/staging)
- `app/src/release/google-services.json` (for production)

### 3. Google Cloud Configuration
Configure Google Cloud Speech API:
```bash
export GOOGLE_CLOUD_PROJECT_ID=ghana-voice-ledger
export GOOGLE_CLOUD_SPEECH_API_KEY=your_api_key
```

## Build Configuration

### Build Variants
The app supports multiple build variants:

#### Flavors
- **ghana**: Ghana-specific configuration (GHS currency, local languages)
- **global**: Global configuration (USD currency, English only)

#### Build Types
- **debug**: Development builds with debugging enabled
- **staging**: Pre-production builds with release optimizations
- **release**: Production builds with full optimizations

### Build Commands

#### Debug Builds
```bash
# Build debug APK
./gradlew assembleGhanaDebug

# Install debug APK
./gradlew installGhanaDebug
```

#### Staging Builds
```bash
# Build staging APK
./gradlew assembleGhanaStaging

# Build all staging variants
./gradlew assembleStaging
```

#### Release Builds
```bash
# Build release APK
./gradlew assembleGhanaRelease

# Build release AAB (for Play Store)
./gradlew bundleGhanaRelease

# Build both APK and AAB
./gradlew assembleGhanaRelease bundleGhanaRelease
```

## Deployment Environments

### 1. Development Environment
- **Purpose**: Local development and testing
- **Configuration**: Debug build with mock data
- **Database**: Local SQLite with test data
- **API**: Development API endpoints

```bash
# Quick development build
./gradlew assembleGhanaDebug --build-cache --parallel
```

### 2. Staging Environment
- **Purpose**: Pre-production testing
- **Configuration**: Staging build with production-like data
- **Database**: Staging database
- **API**: Staging API endpoints

```bash
# Staging deployment
./scripts/deploy.sh staging release
```

### 3. Production Environment
- **Purpose**: Live application for end users
- **Configuration**: Release build with full optimizations
- **Database**: Production database
- **API**: Production API endpoints

```bash
# Production deployment
./scripts/deploy.sh release release
```

## CI/CD Pipeline

### GitHub Actions Workflow
The CI/CD pipeline is configured in `.github/workflows/ci-cd.yml` and includes:

#### 1. Test Stage
- Unit tests
- Instrumented tests
- Lint checks
- Security scans

#### 2. Build Stage
- Debug builds (all branches)
- Staging builds (develop/release branches)
- Release builds (main branch/releases)

#### 3. Deploy Stage
- Internal testing (main branch)
- Production deployment (releases)

### Required Secrets
Configure these secrets in GitHub repository settings:

```bash
# Signing
KEYSTORE_BASE64          # Base64 encoded keystore file
KEYSTORE_PASSWORD        # Keystore password
KEY_ALIAS               # Key alias
KEY_PASSWORD            # Key password

# Google Play
GOOGLE_PLAY_SERVICE_ACCOUNT_JSON  # Service account JSON

# Notifications
SLACK_WEBHOOK_URL       # Slack webhook for notifications
```

### Triggering Deployments

#### Automatic Deployments
- **Push to main**: Triggers internal testing deployment
- **Create release**: Triggers production deployment
- **Push to develop**: Triggers staging build

#### Manual Deployments
```bash
# Trigger workflow manually
gh workflow run ci-cd.yml --ref main
```

## Docker Deployment

### Development with Docker
```bash
# Start development environment
docker-compose up app-builder

# Build with specific profile
docker-compose --profile firebase up

# Build release with Docker
docker-compose --profile release up release-builder
```

### Production Docker Build
```bash
# Build production image
docker build --target runtime \
  --build-arg BUILD_TYPE=release \
  --build-arg KEYSTORE_PASSWORD=$KEYSTORE_PASSWORD \
  --build-arg KEY_ALIAS=$KEY_ALIAS \
  --build-arg KEY_PASSWORD=$KEY_PASSWORD \
  -t ghana-voice-ledger:latest .

# Run artifact server
docker run -p 8090:80 ghana-voice-ledger:latest
```

## Manual Deployment

### Using Deployment Script
```bash
# Make script executable (Linux/macOS)
chmod +x scripts/deploy.sh

# Deploy to staging
./scripts/deploy.sh staging release

# Deploy to production
./scripts/deploy.sh release release
```

### Manual Build Process
1. **Clean and prepare**:
   ```bash
   ./gradlew clean
   ```

2. **Run tests**:
   ```bash
   ./gradlew testGhanaReleaseUnitTest
   ./gradlew lintGhanaRelease
   ```

3. **Build release**:
   ```bash
   ./gradlew assembleGhanaRelease bundleGhanaRelease
   ```

4. **Verify outputs**:
   ```bash
   ls -la app/build/outputs/apk/ghana/release/
   ls -la app/build/outputs/bundle/ghanaRelease/
   ```

### Google Play Console Deployment
1. **Upload AAB file** to Google Play Console
2. **Configure release notes** and metadata
3. **Set rollout percentage** (start with 5-10%)
4. **Monitor crash reports** and user feedback
5. **Gradually increase rollout** to 100%

## Build Optimization

### Using Optimization Script
```bash
# Run full optimization
./scripts/optimize-build.sh all

# Specific optimizations
./scripts/optimize-build.sh clean      # Clean caches
./scripts/optimize-build.sh properties # Optimize Gradle properties
./scripts/optimize-build.sh analyze    # Analyze build performance
```

### Performance Tips
1. **Use build cache**: `--build-cache`
2. **Enable parallel builds**: `--parallel`
3. **Keep Gradle daemon running**: `--daemon`
4. **Use configuration cache**: `--configuration-cache`

```bash
# Optimized build command
./gradlew assembleGhanaRelease --build-cache --parallel --configuration-cache
```

## Troubleshooting

### Common Issues

#### 1. Keystore Issues
```bash
# Error: Keystore not found
# Solution: Ensure keystore path is correct
export KEYSTORE_PATH=keystore/ghana-voice-ledger.jks

# Error: Wrong keystore password
# Solution: Verify password in .env file
```

#### 2. Build Failures
```bash
# Error: Out of memory
# Solution: Increase Gradle heap size
export GRADLE_OPTS="-Xmx4g -XX:MaxMetaspaceSize=512m"

# Error: SDK not found
# Solution: Set ANDROID_HOME
export ANDROID_HOME=/path/to/android-sdk
```

#### 3. Dependency Issues
```bash
# Clear dependency cache
./gradlew --refresh-dependencies

# Clean and rebuild
./gradlew clean build
```

#### 4. Firebase Configuration
```bash
# Error: google-services.json not found
# Solution: Ensure file is in correct location
cp google-services.json app/
```

### Debug Commands
```bash
# Check Gradle configuration
./gradlew properties

# Analyze dependencies
./gradlew app:dependencies

# Check build performance
./gradlew assembleGhanaDebug --profile

# Verify signing configuration
./gradlew signingReport
```

### Log Analysis
```bash
# View build logs
./gradlew assembleGhanaRelease --info

# Debug build issues
./gradlew assembleGhanaRelease --debug --stacktrace

# Check lint issues
./gradlew lintGhanaRelease --continue
```

## Security Considerations

### 1. Keystore Security
- Store keystore files securely
- Use environment variables for passwords
- Never commit keystore files to version control

### 2. API Keys
- Use different API keys for each environment
- Rotate API keys regularly
- Monitor API key usage

### 3. Build Security
- Scan dependencies for vulnerabilities
- Use ProGuard/R8 for code obfuscation
- Enable certificate pinning in production

## Monitoring and Maintenance

### 1. Build Monitoring
- Monitor build times and success rates
- Set up alerts for build failures
- Track APK/AAB size over time

### 2. Deployment Monitoring
- Monitor crash rates after deployments
- Track user adoption of new versions
- Monitor performance metrics

### 3. Maintenance Tasks
- Regular dependency updates
- Security patch deployments
- Performance optimization reviews

## Support and Documentation

### Additional Resources
- [Android Developer Documentation](https://developer.android.com/)
- [Gradle Build Tool Documentation](https://gradle.org/guides/)
- [Firebase Documentation](https://firebase.google.com/docs)
- [Google Play Console Help](https://support.google.com/googleplay/android-developer/)

### Getting Help
- Check build logs for specific error messages
- Review GitHub Actions workflow runs
- Consult team documentation and runbooks
- Contact DevOps team for infrastructure issues

---

**Last Updated**: $(date)
**Version**: 1.0.0