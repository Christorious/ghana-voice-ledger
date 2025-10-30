# Code Coverage Guide

This guide explains how to generate and use code coverage reports for the Ghana Voice Ledger application.

## Overview

The project uses JaCoCo (Java Code Coverage) for measuring test coverage. Coverage reporting is configured for the debug build type and helps ensure code quality and test completeness.

## Configuration

JaCoCo is configured in the `app/build.gradle.kts` file with the following settings:

- **Minimum Coverage Threshold**: 70%
- **Enabled Build Types**: Debug
- **Coverage Types**: Unit test coverage and Android test coverage
- **JaCoCo Version**: 0.8.11

## Generating Coverage Reports Locally

### Prerequisites

- Android SDK installed and configured
- Java 17+
- Project dependencies synced

### Generate HTML/XML Coverage Reports

Run the following command in the project root directory:

```bash
./gradlew jacocoTestReport
```

This command will:
1. Run all debug unit tests
2. Collect coverage data
3. Generate HTML and XML reports

### Report Locations

After running the coverage task, reports will be available at:

- **HTML Report**: `app/build/reports/jacoco/jacocoTestReport/html/index.html`
- **XML Report**: `app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml`
- **Coverage Data**: `app/build/jacoco/testDebugUnitTest.exec`

### Viewing Coverage Reports

Open the HTML report in your browser:

```bash
# On Linux/macOS
open app/build/reports/jacoco/jacocoTestReport/html/index.html

# On Windows
start app/build/reports/jacoco/jacocoTestReport/html/index.html
```

The HTML report provides:
- Overall coverage statistics
- Package-level coverage breakdown
- Class-level coverage details
- Line-by-line coverage visualization
- Missed branches and complexity metrics

## Coverage Verification

### Enforce Minimum Coverage

To verify that the code meets the minimum coverage threshold (70%):

```bash
./gradlew jacocoTestCoverageVerification
```

This task will:
- Check if overall coverage meets the 70% threshold
- Fail the build if coverage is below the threshold
- Display coverage violations

### Integrated with `check` Task

Coverage verification is automatically included in the standard `check` task:

```bash
./gradlew check
```

This runs:
- All tests
- Lint checks
- Coverage verification

## Coverage Exclusions

The following are excluded from coverage reports:

- Android generated code (`R.class`, `BuildConfig.*`, `Manifest.*`)
- Test files (`**/*Test*.*`)
- Dagger/Hilt generated code (`*_Factory.*`, `*_MembersInjector.*`, `*Dagger*.*`, `*Hilt*.*`)
- Hilt internal classes

These exclusions focus coverage metrics on application code rather than generated or framework code.

## CI/CD Integration

### Automated Coverage Reporting

The CI/CD pipeline automatically:
1. Runs tests with coverage on every push and pull request
2. Generates coverage reports
3. Verifies minimum coverage threshold
4. Uploads coverage artifacts

### Accessing CI Coverage Reports

Coverage reports from CI builds are available as artifacts in GitHub Actions:

1. Navigate to the GitHub Actions tab
2. Select the workflow run
3. Download the `coverage-report` artifact
4. Extract and open `html/index.html`

## Best Practices

### Writing Testable Code

- Follow Clean Architecture principles
- Use dependency injection (Hilt)
- Prefer constructor injection
- Mock external dependencies
- Test business logic in ViewModels and Use Cases

### Improving Coverage

1. **Identify Low Coverage Areas**
   - Review the HTML report
   - Focus on classes with < 70% coverage
   - Prioritize critical business logic

2. **Write Unit Tests**
   ```kotlin
   @Test
   fun `test repository returns correct data`() = runTest {
       // Arrange
       val expected = Transaction(/* ... */)
       coEvery { dao.getTransaction(any()) } returns expected
       
       // Act
       val result = repository.getTransaction(1)
       
       // Assert
       assertEquals(expected, result)
   }
   ```

3. **Write Integration Tests**
   - Test Room DAOs
   - Test API endpoints
   - Test Worker classes

4. **Write UI Tests**
   - Test critical user flows
   - Test navigation
   - Test Compose screens

### Coverage Goals by Layer

- **Domain Layer** (Use Cases, Models): Target 90%+
- **Data Layer** (Repositories, DAOs): Target 80%+
- **Presentation Layer** (ViewModels): Target 80%+
- **UI Layer** (Composables): Target 60%+

## Troubleshooting

### Issue: No Coverage Data Generated

**Solution**: Ensure you're running tests before generating the report:
```bash
./gradlew clean testDebugUnitTest jacocoTestReport
```

### Issue: Coverage Verification Fails

**Solution**: Check the console output for specific coverage percentages and improve test coverage for identified classes.

### Issue: Reports Not Generated

**Solution**: 
1. Check that the debug build type has coverage enabled
2. Verify JaCoCo configuration in `build.gradle.kts`
3. Clean and rebuild: `./gradlew clean build`

### Issue: Incorrect Coverage Numbers

**Solution**: 
1. Ensure Hilt/Dagger generated classes are excluded
2. Check that test classes are excluded
3. Verify exclusion patterns in `coverageExclusions`

## Advanced Usage

### Generate Coverage for Specific Tests

```bash
# Run specific test class
./gradlew testDebugUnitTest --tests="com.voiceledger.ghana.domain.TransactionUseCaseTest" jacocoTestReport
```

### Generate Coverage with Different Thresholds

Modify the `jacocoTestCoverageVerification` task in `build.gradle.kts`:

```kotlin
violationRules {
    rule {
        limit {
            minimum = BigDecimal("0.80") // Change to 80%
        }
    }
    
    // Add per-package rules
    rule {
        element = "PACKAGE"
        limit {
            counter = "LINE"
            minimum = BigDecimal("0.75")
        }
    }
}
```

### Combine Unit and Instrumentation Coverage

For comprehensive coverage including Android tests:

```bash
./gradlew createDebugCoverageReport
```

## Resources

- [JaCoCo Documentation](https://www.jacoco.org/jacoco/trunk/doc/)
- [Android Testing Guide](https://developer.android.com/training/testing)
- [Kotlin Testing Best Practices](https://kotlinlang.org/docs/jvm-test-using-junit.html)

## Support

For issues or questions about code coverage:
- Check the [Troubleshooting](#troubleshooting) section
- Review project documentation
- Open an issue on GitHub
