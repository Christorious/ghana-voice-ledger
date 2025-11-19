# Hilt Dependency Injection Graph Documentation

## Overview

This document describes the complete Hilt dependency injection (DI) graph for the Ghana Voice Ledger Android application. It maps all @Module classes, @Provides methods, @Inject constructors, and their dependencies.

## Module Directory

All Hilt modules are located in: `app/src/main/java/com/voiceledger/ghana/di/`

### Module List

| Module | Purpose | Scope | Key Dependencies |
|--------|---------|-------|------------------|
| **DatabaseModule** | Database, DAOs, and repositories | SingletonComponent | Context, SecurityManager |
| **SecurityModule** | Encryption, privacy, and security services | SingletonComponent | Context |
| **VoiceServiceModule** | Voice capture and processing services | SingletonComponent | All ML/Audio components |
| **SpeechModule** | Speech recognition services | SingletonComponent | Context |
| **VADModule** | Voice Activity Detection | SingletonComponent | Context |
| **SpeakerModule** | Speaker identification | SingletonComponent | Context, Repositories |
| **TransactionModule** | Transaction processing ML | SingletonComponent | Repositories, SecurityManager |
| **EntityModule** | Entity extraction services | SingletonComponent | Repositories |
| **AnalyticsModule** | Analytics and monitoring | SingletonComponent | Context |
| **PerformanceModule** | Performance optimization | SingletonComponent | Context, Database |
| **PowerModule** | Power management | SingletonComponent | Context |
| **OfflineModule** | Offline-first architecture | SingletonComponent | Context, Database |
| **SummaryModule** | Daily summary generation | SingletonComponent | Repositories |
| **WorkManagerModule** | WorkManager configuration | SingletonComponent | Context |

## Dependency Graph

### Core Layer (Foundational)

```
Context (@ApplicationContext)
  ├─ DatabaseModule.provideVoiceLedgerDatabase()
  ├─ SecurityModule.provideEncryptionService()
  ├─ VoiceServiceModule.provideAudioCaptureController()
  └─ WorkManagerModule.provideWorkManager()
```

### Security Layer

```
SecurityModule:
  ├─ provideEncryptionService(Context) → EncryptionService
  ├─ providePrivacyManager(Context, EncryptionService) → PrivacyManager
  ├─ provideSecureDataStorage(Context, EncryptionService, PrivacyManager) → SecureDataStorage
  ├─ provideSecurityManager(Context, EncryptionService, PrivacyManager, SecureDataStorage) → SecurityManager
  └─ provideAnalyticsConsentProvider(SecurityManager) → AnalyticsConsentProvider
```

### Database Layer

```
DatabaseModule:
  ├─ provideVoiceLedgerDatabase(Context, SecurityManager) → VoiceLedgerDatabase
  ├─ provideEncryptedVoiceLedgerDatabase(Context, SecurityManager) → VoiceLedgerDatabase (@EncryptedDatabase)
  │  
  ├─ DAO Providers:
  │  ├─ provideTransactionDao(VoiceLedgerDatabase) → TransactionDao
  │  ├─ provideDailySummaryDao(VoiceLedgerDatabase) → DailySummaryDao
  │  ├─ provideSpeakerProfileDao(VoiceLedgerDatabase) → SpeakerProfileDao
  │  ├─ provideProductVocabularyDao(VoiceLedgerDatabase) → ProductVocabularyDao
  │  ├─ provideAudioMetadataDao(VoiceLedgerDatabase) → AudioMetadataDao
  │  └─ provideOfflineOperationDao(VoiceLedgerDatabase) → OfflineOperationDao
  │
  └─ Repository Providers:
     ├─ provideTransactionRepository(TransactionDao, SecurityManager) → TransactionRepository
     ├─ provideDailySummaryRepository(DailySummaryDao, TransactionDao) → DailySummaryRepository
     ├─ provideSpeakerProfileRepository(SpeakerProfileDao) → SpeakerProfileRepository
     ├─ provideProductVocabularyRepository(ProductVocabularyDao) → ProductVocabularyRepository
     ├─ provideAudioMetadataRepository(AudioMetadataDao) → AudioMetadataRepository
     └─ provideTransactionAnalyticsRepository(TransactionDao, AnalyticsConsentProvider) → TransactionAnalyticsRepository
```

### ML/Speech Processing Layer

```
SpeechModule:
  ├─ provideGoogleCloudSpeechRecognizer(Context) → GoogleCloudSpeechRecognizer
  ├─ provideOfflineSpeechRecognizer(Context) → OfflineSpeechRecognizer
  ├─ provideSpeechRecognitionManager(Context, GoogleCloudSpeechRecognizer, OfflineSpeechRecognizer) → SpeechRecognitionManager
  ├─ provideLanguageDetector() → LanguageDetector
  ├─ providePrimarySpeechRecognizer(SpeechRecognitionManager) → SpeechRecognizer (@PrimarySpeechRecognizer)
  ├─ provideOnlineSpeechRecognizer(GoogleCloudSpeechRecognizer) → SpeechRecognizer (@OnlineSpeechRecognizer)
  └─ provideOfflineSpeechRecognizerInterface(OfflineSpeechRecognizer) → SpeechRecognizer (@OfflineSpeechRecognizer)

VADModule:
  ├─ provideVADProcessor(Context) → VADProcessor
  ├─ provideWebRTCVADProcessor(Context) → WebRTCVADProcessor
  └─ provideVADManager(Context, VADProcessor, WebRTCVADProcessor) → VADManager

SpeakerModule:
  └─ provideSpeakerIdentifier(Context, SpeakerProfileRepository, AudioUtils) → SpeakerIdentifier

TransactionModule:
  ├─ provideTransactionPatternMatcher() → TransactionPatternMatcher
  ├─ provideTransactionStateMachine(TransactionPatternMatcher, SecurityManager) → TransactionStateMachine
  └─ provideTransactionProcessor(TransactionStateMachine, TransactionRepository, ProductVocabularyRepository, AudioMetadataRepository) → TransactionProcessor

EntityModule:
  ├─ provideGhanaEntityExtractor(ProductVocabularyRepository) → GhanaEntityExtractor
  ├─ provideEntityExtractor(GhanaEntityExtractor) → EntityExtractor
  ├─ provideEntityNormalizer() → EntityNormalizer
  ├─ provideEntityExtractionService(EntityExtractor, EntityNormalizer, ProductVocabularyRepository) → EntityExtractionService
  ├─ providePrimaryEntityExtractor(EntityExtractionService) → EntityExtractor (@PrimaryEntityExtractor)
  └─ provideGhanaSpecificExtractor(GhanaEntityExtractor) → EntityExtractor (@GhanaSpecificExtractor)
```

### Voice Service Layer

```
VoiceServiceModule:
  ├─ provideAudioCaptureController(Context) → AudioCaptureController
  ├─ provideSpeechProcessingPipeline(VADManager, SpeakerIdentifier, SpeechRecognitionManager, TransactionProcessor, AudioMetadataRepository, CoroutineDispatchers) → SpeechProcessingPipeline
  ├─ provideVoiceNotificationHelper(Context) → VoiceNotificationHelper
  ├─ provideWorkManagerScheduler(Context) → WorkManagerScheduler
  └─ provideVoiceSessionCoordinator(Context, AudioCaptureController, SpeechProcessingPipeline, VoiceNotificationHelper, VADManager, SpeechRecognitionManager, OfflineQueueManager, PowerManager, CoroutineDispatcher) → VoiceSessionCoordinator
```

### Power Management Layer

```
PowerModule:
  ├─ providePowerManager(Context) → PowerManager
  ├─ provideVoiceAgentServiceManager(Context) → VoiceAgentServiceManager
  └─ providePowerOptimizationService(Context, PowerManager, VoiceAgentServiceManager) → PowerOptimizationService
```

### Offline Layer

```
OfflineModule:
  ├─ provideOfflineQueueManager(Context, VoiceLedgerDatabase) → OfflineQueueManager
  ├─ provideConflictResolver() → ConflictResolver
  └─ provideOfflineTransactionRepository(Context, OfflineQueueManager, VoiceLedgerDatabase) → OfflineTransactionRepository
```

### Analytics Layer

```
AnalyticsModule:
  ├─ provideAnalyticsService(Context) → AnalyticsService
  ├─ provideCrashlyticsService(Context) → CrashlyticsService
  ├─ providePerformanceMonitoringService(Context, AnalyticsService, CrashlyticsService) → PerformanceMonitoringService
  └─ provideUsageDashboardService(Context, AnalyticsService, CrashlyticsService) → UsageDashboardService
```

### Performance Layer

```
PerformanceModule:
  ├─ provideMemoryManager(Context) → MemoryManager
  ├─ provideDatabaseOptimizer(VoiceLedgerDatabase) → DatabaseOptimizer
  └─ providePerformanceMonitor() → PerformanceMonitor
```

### Summary Layer

```
SummaryModule:
  ├─ provideDailySummaryGenerator(TransactionRepository, SpeakerProfileRepository, DailySummaryRepository) → DailySummaryGenerator
  └─ provideSummaryPresentationService(Context, DailySummaryRepository) → SummaryPresentationService
```

## Dependency Flow Analysis

### No Circular Dependencies

The dependency graph is acyclic with proper layering:

1. **Foundation Layer**: Context (no dependencies)
2. **Security Layer**: Depends only on Foundation
3. **Database Layer**: Depends on Foundation + Security
4. **ML/Speech Layer**: Depends on Foundation + Database
5. **Voice Service Layer**: Depends on Foundation + ML/Speech + Offline
6. **Power Management Layer**: Depends on Foundation + Voice Service
7. **Analytics Layer**: Depends on Foundation
8. **Performance Layer**: Depends on Foundation + Database
9. **Summary Layer**: Depends on Database

### Scope Management

All providers are @Singleton scoped to SingletonComponent:
- **Rationale**: Ensures single instances across app lifetime
- **Exception**: None - all database operations require singleton instances
- **Thread Safety**: Managed by Hilt and Room's built-in mechanisms

## Qualifiers Used

| Qualifier | Purpose | Module |
|-----------|---------|--------|
| @EncryptedDatabase | Encrypted database variant | DatabaseModule |
| @PrimarySpeechRecognizer | Manager-based speech recognition | SpeechModule |
| @OnlineSpeechRecognizer | Google Cloud speech only | SpeechModule |
| @OfflineSpeechRecognizer | Offline recognition interface | SpeechModule |
| @PrimaryEntityExtractor | Service-based entity extraction | EntityModule |
| @GhanaSpecificExtractor | Ghana-specific extraction | EntityModule |
| @ApplicationContext | App-level context provider | Hilt built-in |

## Known Limitations & Future Improvements

1. **Feature Toggling**: Google Cloud Speech dependency is now feature-gated
2. **Provider Consolidation**: Multiple modules could be refactored into a single FeatureModule
3. **Scope Refinement**: Could use ViewModelComponent or ActivityComponent for UI-specific dependencies
4. **Lazy Initialization**: Large modules could benefit from lazy provider patterns

## Resolution Process

All dependencies resolve correctly through:
1. @Inject constructors on singleton classes
2. @Provides methods that list all required parameters
3. Type matching between consumer parameters and provider return types
4. Qualifiers disambiguating multiple providers of same type
5. Proper @InstallIn scoping to SingletonComponent

## Conclusion

The current DI graph is well-structured, acyclic, and maintainable. All 13 modules follow consistent patterns and proper dependency inversion principles. The architecture supports easy testing through constructor injection and clear separation of concerns across layers.
