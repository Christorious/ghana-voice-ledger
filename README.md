# Voice Ledger Ghana

A voice-powered financial transaction recording application designed specifically for Ghanaian small business owners and market vendors.

## Overview

Voice Ledger Ghana enables users to record financial transactions using natural voice commands in English, Twi, and other Ghanaian languages. The app uses advanced speech recognition and machine learning to automatically categorize transactions, identify speakers, and generate daily summaries.

## Features

### Core Functionality
- **Voice Transaction Recording**: Record sales, purchases, and expenses using natural speech
- **Multi-Language Support**: English, Twi, Ga, Ewe, and other Ghanaian languages
- **Offline Capability**: Full functionality without internet connection
- **Speaker Identification**: Automatic identification of different speakers/users
- **Smart Categorization**: AI-powered transaction categorization
- **Daily Summaries**: Automated daily business summaries with insights

### Advanced Features
- **Real-time Processing**: Instant transaction processing and feedback
- **Data Privacy**: End-to-end encryption and local data storage
- **Battery Optimization**: Efficient power management for all-day use
- **Accessibility**: Full accessibility support for users with disabilities
- **Export Capabilities**: Export data in multiple formats (CSV, PDF, Excel)

## Technical Architecture

### Technology Stack
- **Platform**: Android (Kotlin)
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: Clean Architecture with MVVM
- **Database**: Room with SQLite
- **Dependency Injection**: Hilt
- **Speech Recognition**: Google Cloud Speech API + Offline TensorFlow Lite
- **Machine Learning**: TensorFlow Lite for on-device processing
- **Audio Processing**: WebRTC VAD, custom audio utilities
- **Security**: AES encryption, biometric authentication

### Key Components
- **Voice Agent Service**: Continuous voice monitoring and processing
- **Speech Recognition Manager**: Multi-provider speech recognition
- **Transaction Processor**: ML-powered transaction parsing and categorization
- **Speaker Identification**: TensorFlow Lite-based speaker recognition
- **Offline Queue Manager**: Handles offline transaction synchronization
- **Security Manager**: Encryption and privacy protection

## Project Structure

```
app/
├── src/main/java/com/voiceledger/ghana/
│   ├── data/                    # Data layer (repositories, DAOs, entities)
│   ├── domain/                  # Domain layer (use cases, models, repositories)
│   ├── presentation/            # UI layer (screens, view models, compose)
│   ├── ml/                      # Machine learning components
│   ├── service/                 # Background services
│   ├── security/                # Security and encryption
│   ├── offline/                 # Offline functionality
│   ├── performance/             # Performance optimization
│   ├── analytics/               # Analytics and monitoring
│   └── di/                      # Dependency injection modules
├── src/test/                    # Unit tests
├── src/androidTest/             # Integration tests
└── src/main/res/               # Resources (layouts, strings, etc.)
```

## Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 34
- Kotlin 1.9.0+
- Java 17+

### Setup Instructions

1. **Clone the repository**
   ```bash
   git clone https://github.com/voiceledger/ghana-voice-ledger.git
   cd ghana-voice-ledger
   ```

2. **Configure local properties**
   ```bash
   cp local.properties.example local.properties
   # Edit local.properties with your SDK path and API keys
   ```

3. **Set up API keys**
   - Google Cloud Speech API key
   - Firebase configuration
   - Analytics keys (see local.properties.example)

4. **Build the project**
   ```bash
   ./gradlew build
   ```

5. **Run tests**
   ```bash
   # Run unit tests
   ./gradlew test
   
   # Run integration tests (requires connected device/emulator)
   ./gradlew connectedDebugAndroidTest
   ```
   
   For detailed integration test documentation, see [INTEGRATION_TESTS.md](INTEGRATION_TESTS.md)

6. **Generate coverage reports**
   ```bash
   ./gradlew jacocoTestReport
   ```

   The HTML report is available at `app/build/reports/jacoco/jacocoTestReport/html/index.html`. To enforce the 70% minimum coverage threshold locally, run:

   ```bash
   ./gradlew jacocoTestCoverageVerification
   ```

   See [docs/COVERAGE.md](docs/COVERAGE.md) for detailed coverage guidance.
> 📘 For a complete onboarding checklist, secrets configuration, recommended build commands, and feature toggle reference, see the top-level [Developer Guide](DEVELOPER_GUIDE.md).

### Configuration

#### Required API Keys
- **Google Cloud Speech API**: For cloud-based speech recognition
- **Firebase**: For analytics and crash reporting
- **TensorFlow Lite**: For offline ML models

#### Optional Configuration
- **Performance Monitoring**: Enable detailed performance tracking
- **Advanced Analytics**: Enhanced user behavior analytics
- **Beta Features**: Access to experimental features

## Development

### Dependency Management
- Dependencies and plugin versions are centralized in `gradle/libs.versions.toml` via the Gradle Version Catalog.
- Use the provided `libs` aliases in build scripts instead of hardcoding version numbers.

### Code Style
- Follow Kotlin coding conventions
- Use ktlint for code formatting
- Maintain 70% test coverage minimum (enforced by JaCoCo)

### Architecture Guidelines
- Follow Clean Architecture principles
- Use MVVM pattern for UI components
- Implement Repository pattern for data access
- Use Dependency Injection with Hilt

### Testing Strategy
- Unit tests for business logic
- Integration tests for data layer
- UI tests for critical user flows
- Performance tests for audio processing

## Security & Privacy

### Data Protection
- All sensitive data encrypted at rest
- Voice recordings processed locally when possible
- No personal data transmitted without explicit consent
- Biometric authentication for app access

### Privacy Features
- Local-first data storage
- Optional cloud backup with encryption
- Granular privacy controls
- Data retention policies

## Performance Optimization

### Battery Efficiency
- Intelligent voice activation detection
- Background processing optimization
- Adaptive quality based on battery level
- Power-aware ML model selection

### Memory Management
- Efficient audio buffer management
- ML model caching strategies
- Database query optimization
- Memory leak prevention

## Accessibility

### Supported Features
- Screen reader compatibility
- Voice navigation
- High contrast themes
- Large text support
- Motor accessibility features

## Localization

### Supported Languages
- English (primary)
- Twi (Akan)
- Ga
- Ewe
- Hausa
- Dagbani

### Cultural Adaptations
- Local currency formatting (Ghana Cedis)
- Cultural business practices
- Local product vocabulary
- Regional dialects support

## Contributing

### Development Process
1. Fork the repository
2. Create a feature branch
3. Implement changes with tests
4. Submit pull request
5. Code review and merge

### Guidelines
- Follow the existing code style
- Add tests for new features
- Update documentation
- Ensure accessibility compliance

## Deployment

### Build Variants
- **Debug**: Development builds with logging
- **Release**: Production builds with optimization
- **Beta**: Testing builds with additional logging

### Release Process
1. Update version numbers
2. Run full test suite
3. Generate signed APK
4. Deploy to Play Store
5. Monitor crash reports

## Support

### Documentation
- [API Documentation](docs/API.md)
- [User Guide](docs/USER_GUIDE.md)
- [Developer Guide](docs/DEVELOPER_GUIDE.md)
- [Code Coverage Guide](docs/COVERAGE.md)
- [Troubleshooting](docs/TROUBLESHOOTING.md)

### Community
- [GitHub Issues](https://github.com/voiceledger/ghana-voice-ledger/issues)
- [Discussions](https://github.com/voiceledger/ghana-voice-ledger/discussions)
- [Discord Community](https://discord.gg/voiceledger)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Google Cloud Speech API team
- TensorFlow Lite community
- Android Jetpack Compose team
- Ghanaian language consultants
- Beta testing community

## Roadmap

### Version 1.0 (Current)
- ✅ Core voice recording functionality
- ✅ Basic transaction categorization
- ✅ Offline support
- ✅ Multi-language support

### Version 1.1 (Planned)
- 🔄 Advanced analytics dashboard
- 🔄 Cloud synchronization
- 🔄 Team collaboration features
- 🔄 Advanced reporting

### Version 2.0 (Future)
- 📋 Integration with banking APIs
- 📋 Advanced ML models
- 📋 Web dashboard
- 📋 API for third-party integrations

---

**Voice Ledger Ghana** - Empowering Ghanaian businesses through voice technology.