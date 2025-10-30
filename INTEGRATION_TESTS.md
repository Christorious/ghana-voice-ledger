# Integration Tests Documentation

## Overview

This project includes comprehensive integration tests covering end-to-end transaction flows, database migrations, and offline-to-online sync transitions. These tests run on Android devices or emulators using the Hilt testing framework.

## Test Suites

### 1. TransactionFlowIntegrationTest

**Location:** `app/src/androidTest/java/com/voiceledger/ghana/integration/TransactionFlowIntegrationTest.kt`

**Purpose:** Tests end-to-end transaction operations through the repository layer.

**Coverage:**
- Transaction insertion and retrieval
- Multiple transaction batch operations
- Daily summary calculations
- Product-based queries
- Time range queries
- Transaction updates and deletions
- Review status management
- Sync status tracking
- Search functionality
- Analytics calculations

**Key Features:**
- Uses Hilt dependency injection for realistic test environment
- Tests actual database operations with Room
- Verifies data integrity across operations
- Validates repository-level business logic

### 2. DatabaseMigrationTest

**Location:** `app/src/androidTest/java/com/voiceledger/ghana/integration/DatabaseMigrationTest.kt`

**Purpose:** Validates database schema migrations and data preservation.

**Coverage:**
- Migration from version 1 to version 2
- Offline operations table creation
- Index creation verification
- Data preservation across migrations
- Large dataset migration handling
- Current version database creation

**Migration Details:**
- **Version 1 → 2:** Adds `offline_operations` table with indexes for:
  - timestamp
  - type
  - status
  - priority

**Key Features:**
- Uses Room's MigrationTestHelper
- Verifies schema integrity
- Ensures zero data loss
- Tests with various data volumes

### 3. OfflineToOnlineSyncIntegrationTest

**Location:** `app/src/androidTest/java/com/voiceledger/ghana/integration/OfflineToOnlineSyncIntegrationTest.kt`

**Purpose:** Tests offline-first architecture and sync behavior.

**Coverage:**
- Queue operations when offline
- Process operations when online
- Operation persistence across sessions
- Retry logic for failed operations
- Network fluctuation handling
- Operation status transitions

**Key Features:**
- Simulates network state changes
- Tests queue manager behavior
- Validates offline-first patterns
- Verifies sync reliability

## Running the Tests

### Prerequisites

1. **Android Device or Emulator**
   - Minimum API level: 24 (Android 7.0)
   - Target API level: 34 (Android 14)
   - Recommended: Use AVD with Google APIs

2. **Development Tools**
   - Android Studio Arctic Fox or later
   - Android SDK with API 34
   - Gradle 8.0+

### Running All Integration Tests

```bash
# Run all instrumentation tests
./gradlew connectedDebugAndroidTest

# Run with specific device
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.device=<device-id>
```

### Running Specific Test Suites

```bash
# Run only TransactionFlowIntegrationTest
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.voiceledger.ghana.integration.TransactionFlowIntegrationTest

# Run only DatabaseMigrationTest
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.voiceledger.ghana.integration.DatabaseMigrationTest

# Run only OfflineToOnlineSyncIntegrationTest
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.voiceledger.ghana.integration.OfflineToOnlineSyncIntegrationTest
```

### Running from Android Studio

1. Open the test file in Android Studio
2. Click the green play button next to the test class or individual test method
3. Select "Run '...' on device/emulator"

### Running on CI/CD

For GitHub Actions or other CI/CD platforms:

```yaml
- name: Run Integration Tests
  run: |
    ./gradlew connectedDebugAndroidTest --no-daemon
    
- name: Upload Test Results
  if: always()
  uses: actions/upload-artifact@v3
  with:
    name: integration-test-results
    path: app/build/reports/androidTests/connected/
```

## Test Configuration

### Hilt Test Runner

The tests use a custom `HiltTestRunner` for proper dependency injection:

**Location:** `app/src/androidTest/java/com/voiceledger/ghana/HiltTestRunner.kt`

**Configuration:** Set in `app/build.gradle.kts`:
```kotlin
testInstrumentationRunner = "com.voiceledger.ghana.HiltTestRunner"
```

### Test Database

Tests use an in-memory Room database with:
- Proper migration support
- Test data isolation
- Automatic cleanup after each test

### Network Simulation

Tests use `NetworkUtils.setNetworkAvailable()` to simulate:
- Offline state
- Online state
- Network fluctuations

## Test Reports

After running tests, find reports at:

- **HTML Report:** `app/build/reports/androidTests/connected/index.html`
- **XML Report:** `app/build/outputs/androidTest-results/connected/`
- **Logcat Output:** Check Android Studio's "Run" tab

## Troubleshooting

### Common Issues

1. **Test Runner Not Found**
   ```
   Solution: Clean and rebuild project
   ./gradlew clean build
   ```

2. **Hilt Injection Fails**
   ```
   Solution: Ensure @HiltAndroidTest annotation is present
   and hiltRule.inject() is called in setUp()
   ```

3. **Database Migration Fails**
   ```
   Solution: Check that DatabaseMigrations.getAllMigrations()
   includes all necessary migrations
   ```

4. **Emulator Connection Issues**
   ```
   Solution: Restart ADB
   adb kill-server
   adb start-server
   ```

### Debug Mode

Run tests with verbose logging:

```bash
./gradlew connectedDebugAndroidTest --info --stacktrace
```

## Best Practices

1. **Test Isolation:** Each test should be independent and not rely on other tests
2. **Data Cleanup:** Use `@After` to clean up test data
3. **Hilt Injection:** Always call `hiltRule.inject()` in `setUp()`
4. **Network State:** Reset network state in `@After` if modified
5. **Timeouts:** Use appropriate delays for async operations

## Coverage Goals

- **Transaction Flow:** >90% coverage of repository operations
- **Database Migration:** 100% migration path validation
- **Offline Sync:** >85% coverage of sync scenarios

## Continuous Integration

These tests are designed to run on CI/CD pipelines:

- **Parallel Execution:** Tests can run in parallel on multiple devices
- **Failure Isolation:** Individual test failures don't affect others
- **Report Generation:** JUnit XML format for CI integration

## Contributing

When adding new integration tests:

1. Follow existing test naming conventions
2. Use descriptive test method names (testFeature_scenario_expectedResult)
3. Add comprehensive comments for complex test scenarios
4. Update this documentation with new test suites
5. Ensure tests are idempotent and isolated

## Support

For issues or questions:
- Check test logs in `app/build/outputs/androidTest-results/`
- Review Hilt setup in test classes
- Verify database schema versions match migration definitions
- Ensure network simulation is properly configured
