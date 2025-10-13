# Design Document

## Overview

The Ghana Voice Ledger is designed as a real-time audio processing system that operates continuously in the background to detect and log sales transactions automatically. The architecture follows Clean Architecture principles with clear separation between presentation, domain, and data layers. The system is optimized for budget Android devices and designed to work efficiently in noisy market environments with minimal battery consumption.

## Architecture

### High-Level Architecture

The application follows a layered architecture pattern:

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐│
│  │   Dashboard     │ │    History      │ │    Settings     ││
│  │   (Compose)     │ │   (Compose)     │ │   (Compose)     ││
│  └─────────────────┘ └─────────────────┘ └─────────────────┘│
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                     Domain Layer                            │
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐│
│  │   Use Cases     │ │   Repositories  │ │     Models      ││
│  │                 │ │  (Interfaces)   │ │                 ││
│  └─────────────────┘ └─────────────────┘ └─────────────────┘│
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                      Data Layer                             │
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐│
│  │  Local Database │ │  Remote APIs    │ │  ML Processors  ││
│  │     (Room)      │ │   (Retrofit)    │ │  (TensorFlow)   ││
│  └─────────────────┘ └─────────────────┘ └─────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### Core Processing Pipeline

The audio processing pipeline consists of sequential stages:

```
Audio Input → VAD → Speaker ID → Speech-to-Text → State Machine → Entity Extraction → Database
```

## Components and Interfaces

### 1. Background Audio Service (VoiceAgentService)

**Purpose**: Continuous audio capture and processing during market hours
**Type**: Android Foreground Service
**Key Responsibilities**:
- Manage AudioRecord lifecycle
- Coordinate with AI processing pipeline
- Handle power management and battery optimization
- Maintain persistent notification

**Interface**:
```kotlin
interface AudioService {
    fun startListening()
    fun pauseListening()
    fun stopListening()
    fun getCurrentStatus(): ListeningStatus
}
```

### 2. Voice Activity Detection (VADProcessor)

**Purpose**: Filter silence and background noise to process only speech segments
**Implementation**: WebRTC VAD or Silero VAD
**Key Features**:
- Real-time speech detection
- Noise threshold adaptation
- Energy-based filtering

**Interface**:
```kotlin
interface VADProcessor {
    fun processSample(audioData: ByteArray): VADResult
    fun setNoiseThreshold(threshold: Float)
}
```#
## 3. Speaker Identification (SpeakerIdentifier)

**Purpose**: Distinguish between seller and customer voices
**Implementation**: TensorFlow Lite with ResNet-based speaker embeddings
**Model Size**: ~3MB optimized for mobile devices
**Key Features**:
- Voice enrollment for seller profile
- Real-time speaker classification
- Customer recognition for repeat visitors
- Confidence scoring

**Interface**:
```kotlin
interface SpeakerIdentifier {
    fun enrollSeller(audioSamples: List<ByteArray>): EnrollmentResult
    fun identifySpeaker(audioData: ByteArray): SpeakerResult
    fun addCustomerProfile(embedding: FloatArray, customerId: String)
}
```

### 4. Speech Recognition (SpeechRecognizer)

**Purpose**: Convert speech to text with multilingual support
**Primary**: Google Cloud Speech-to-Text API (en-GH locale)
**Fallback**: On-device Whisper model for offline operation
**Languages**: English, Twi, Ga with code-switching support

**Interface**:
```kotlin
interface SpeechRecognizer {
    suspend fun transcribe(audioData: ByteArray, language: String): TranscriptionResult
    fun setLanguageModel(languages: List<String>)
    fun isOfflineCapable(): Boolean
}
```

### 5. Transaction State Machine (TransactionStateMachine)

**Purpose**: Track conversation flow and detect completed transactions
**Pattern**: Finite State Machine
**States**: IDLE → INQUIRY → PRICE_QUOTE → NEGOTIATION → AGREEMENT → PAYMENT → COMPLETE

**State Transitions**:
- IDLE → INQUIRY: Price inquiry detected ("how much", "sɛn na ɛyɛ")
- INQUIRY → PRICE_QUOTE: Seller provides price
- PRICE_QUOTE → NEGOTIATION: Customer negotiates ("reduce small", "too much")
- NEGOTIATION → AGREEMENT: Agreement reached ("okay", "fine", "deal")
- AGREEMENT → PAYMENT: Payment confirmation ("here money", "thank you")
- Any state → IDLE: 2-minute timeout or conversation ends

**Interface**:
```kotlin
interface TransactionStateMachine {
    fun processUtterance(text: String, speaker: Speaker, timestamp: Long): StateTransition
    fun getCurrentState(): TransactionState
    fun reset()
    fun getPartialTransaction(): PartialTransaction?
}
```

### 6. Entity Extraction (EntityExtractor)

**Purpose**: Extract transaction details (amount, product, quantity)
**Implementation**: Custom NLP with Ghana-specific vocabulary
**Key Features**:
- Currency parsing (GH₵, cedis, pesewas)
- Product recognition with fuzzy matching
- Quantity extraction
- Price validation against typical ranges

**Interface**:
```kotlin
interface EntityExtractor {
    fun extractAmount(text: String): AmountResult?
    fun extractProduct(text: String): ProductResult?
    fun extractQuantity(text: String): QuantityResult?
    fun validateTransaction(transaction: Transaction): ValidationResult
}
```

## Data Models

### Core Entities

**Transaction**:
```kotlin
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val amount: Double,
    val currency: String = "GHS",
    val product: String,
    val quantity: Int?,
    val customerId: String?,
    val confidence: Float,
    val transcriptSnippet: String,
    val sellerConfidence: Float,
    val customerConfidence: Float
)
```

**DailySummary**:
```kotlin
@Entity(tableName = "daily_summaries")
data class DailySummary(
    @PrimaryKey val date: String,
    val totalSales: Double,
    val transactionCount: Int,
    val topProduct: String,
    val peakHours: String,
    val repeatCustomers: Int,
    val averageTransactionValue: Double
)
```

**SpeakerProfile**:
```kotlin
@Entity(tableName = "speaker_profiles")
data class SpeakerProfile(
    @PrimaryKey val id: String,
    val voiceEmbedding: FloatArray,
    val isSeller: Boolean,
    val name: String?,
    val visitCount: Int,
    val lastVisit: Long,
    val averageSpending: Double?
)
```

## Error Handling

### Error Categories and Strategies

**Audio Processing Errors**:
- Microphone permission denied → Request permission with clear explanation
- Audio recording failure → Retry with exponential backoff
- Hardware unavailable → Graceful degradation with user notification

**Network Errors**:
- API rate limiting → Queue requests and retry with backoff
- Connection timeout → Switch to offline mode automatically
- Authentication failure → Refresh tokens and retry

**ML Model Errors**:
- Model loading failure → Fall back to simpler algorithms
- Inference timeout → Skip processing for that audio segment
- Memory pressure → Reduce model precision or batch size

**Database Errors**:
- Corruption → Attempt repair or recreate database
- Storage full → Clean up old data automatically
- Transaction conflicts → Implement retry logic with conflict resolution

## Testing Strategy

### Unit Testing
- **Transaction State Machine**: Test all state transitions and edge cases
- **Entity Extraction**: Validate parsing accuracy with Ghana-specific phrases
- **Speaker Identification**: Mock TensorFlow Lite for deterministic testing
- **Audio Processing**: Test VAD accuracy with synthetic audio samples

### Integration Testing
- **End-to-End Pipeline**: Process sample market conversations
- **Offline Functionality**: Test complete workflow without network
- **Battery Usage**: Profile power consumption over extended periods
- **Multi-language Support**: Validate code-switching scenarios

### Performance Testing
- **Memory Usage**: Ensure <100MB footprint on budget devices
- **Latency**: Verify <3 second transaction detection
- **Battery Life**: Confirm <50% drain over 10 hours
- **Storage**: Monitor database growth and cleanup efficiency

### User Acceptance Testing
- **Real Market Environment**: Test with actual traders in Ghana
- **Device Compatibility**: Validate on Tecno, Infinix, Samsung budget phones
- **Language Accuracy**: Test with native Twi and Ga speakers
- **Usability**: Ensure interface works for users with limited technical literacy## 
Technology Stack

### Core Technologies
- **Language**: Kotlin (100% Kotlin codebase)
- **UI Framework**: Jetpack Compose with Material 3 design
- **Architecture**: Clean Architecture + MVVM pattern
- **Dependency Injection**: Hilt (Dagger-based)
- **Asynchronous Programming**: Kotlin Coroutines + Flow
- **Database**: Room (SQLite wrapper) with SQLCipher encryption
- **Background Processing**: WorkManager + ForegroundService

### Machine Learning
- **On-Device ML**: TensorFlow Lite 2.13+
- **Speaker Recognition**: Custom ResNet-based embedding model
- **Voice Activity Detection**: WebRTC VAD or Silero VAD
- **Speech Recognition**: Google Cloud Speech-to-Text API (primary)
- **Offline Speech**: Whisper.cpp Android port (fallback)

### External Services
- **Speech API**: Google Cloud Speech-to-Text with en-GH locale
- **Payment Processing**: Paystack Ghana (MTN MoMo, Vodafone Cash)
- **Analytics**: Firebase Analytics for usage tracking
- **Crash Reporting**: Firebase Crashlytics
- **Performance Monitoring**: Firebase Performance

### Audio Processing
- **Audio Capture**: Android AudioRecord API
- **Format**: PCM 16-bit, 16kHz sample rate, mono channel
- **Buffer Management**: 1-second chunks (16,000 samples)
- **Audio Processing**: Custom DSP pipeline for noise reduction

## Security and Privacy

### Data Protection
- **Audio Data**: Never stored permanently, deleted immediately after processing
- **Database Encryption**: SQLCipher with AES-256 encryption
- **Network Security**: Certificate pinning for all API calls
- **API Keys**: Stored in BuildConfig, obfuscated with R8

### Privacy Controls
- **Data Deletion**: Complete data wipe functionality
- **Export Options**: Secure data export in standard formats
- **Consent Management**: Clear opt-in for cloud services
- **Transparency**: Detailed privacy policy in English and Twi

### Compliance
- **GDPR**: Right to deletion, data portability, consent management
- **Local Regulations**: Compliance with Ghana Data Protection Act
- **Audio Recording**: Clear disclosure and consent for voice recording

## Performance Optimizations

### Battery Optimization
- **Smart Sleep**: VAD-triggered deep sleep during silence
- **CPU Throttling**: Reduce processing during low battery
- **Background Limits**: Respect Android battery optimization settings
- **Wake Lock Management**: Minimal wake lock usage with proper release

### Memory Management
- **Object Pooling**: Reuse audio buffers and processing objects
- **Weak References**: Prevent memory leaks in listeners
- **Garbage Collection**: Minimize allocations in audio processing loop
- **Model Loading**: Lazy loading of ML models when needed

### Storage Optimization
- **Data Compression**: Compress transaction data and embeddings
- **Automatic Cleanup**: Remove old data based on retention policies
- **Efficient Indexing**: Optimize database queries with proper indexes
- **Cache Management**: LRU cache for frequently accessed data

## Deployment Architecture

### Build Configuration
- **Minimum SDK**: API 26 (Android 8.0) for budget device support
- **Target SDK**: API 34 (Android 14) for latest features
- **Build Tools**: Gradle 8.0+ with Kotlin DSL
- **Code Obfuscation**: R8 with custom ProGuard rules

### Release Strategy
- **Beta Testing**: Closed beta with 20 Ghana-based traders
- **Staged Rollout**: 10% → 50% → 100% over 2 weeks
- **A/B Testing**: Feature flags for gradual feature rollout
- **Crash Monitoring**: Real-time crash detection and alerting

### Monitoring and Analytics
- **Performance Metrics**: Transaction detection accuracy, battery usage
- **Business Metrics**: Daily active users, transaction volume
- **Error Tracking**: Comprehensive error logging and reporting
- **User Feedback**: In-app feedback collection and analysis

## Offline-First Design

### Local Processing Capabilities
- **Voice Activity Detection**: Fully on-device using WebRTC VAD
- **Speaker Identification**: TensorFlow Lite model runs locally
- **Basic Speech Recognition**: Whisper.cpp for offline transcription
- **Transaction Detection**: State machine operates without network

### Sync Strategy
- **Queue Management**: Store pending operations in local queue
- **Conflict Resolution**: Last-write-wins with user override options
- **Batch Sync**: Efficient bulk upload when connection restored
- **Incremental Sync**: Only sync changed data to minimize bandwidth

### Offline Indicators
- **Status Display**: Clear offline/online indicators in UI
- **Feature Availability**: Disable cloud-dependent features when offline
- **Data Freshness**: Show last sync timestamp for user awareness
- **Manual Sync**: Allow user-triggered sync when connection available

This design provides a robust, scalable foundation for the Ghana Voice Ledger app that addresses all requirements while maintaining optimal performance on budget Android devices in challenging market environments.