# API Documentation

## Overview

This document provides comprehensive API documentation for the Ghana Voice Ledger application. The app follows a Clean Architecture pattern with clearly defined layers and interfaces.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Domain Layer APIs](#domain-layer-apis)
3. [Data Layer APIs](#data-layer-apis)
4. [ML Component APIs](#ml-component-apis)
5. [Service APIs](#service-apis)
6. [Repository Interfaces](#repository-interfaces)
7. [Data Models](#data-models)
8. [Error Handling](#error-handling)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│              Presentation Layer                      │
│  (ViewModels, Compose UI, Navigation)               │
└────────────────┬────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────┐
│              Domain Layer                            │
│  (Use Cases, Domain Models, Repository Interfaces)  │
└────────────────┬────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────┐
│              Data Layer                              │
│  (Repositories, DAOs, Entities, Remote Data Sources)│
└────────────────┬────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────┐
│         Infrastructure Layer                         │
│  (Room Database, Network, ML Models, Services)      │
└─────────────────────────────────────────────────────┘
```

---

## Domain Layer APIs

### Use Cases

Use cases encapsulate business logic and coordinate repository operations.

#### `RecordTransactionUseCase`

Records a new transaction with validation and business rules.

```kotlin
class RecordTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction): Result<String>
}
```

**Parameters:**
- `transaction: Transaction` - The transaction to record

**Returns:**
- `Result<String>` - Success with transaction ID or Failure with error

**Usage:**
```kotlin
val result = recordTransactionUseCase(transaction)
result.onSuccess { transactionId ->
    // Handle success
}.onFailure { error ->
    // Handle error
}
```

#### `GetDailySummaryUseCase`

Retrieves daily transaction summary for a given date.

```kotlin
class GetDailySummaryUseCase @Inject constructor(
    private val repository: SummaryRepository
) {
    suspend operator fun invoke(date: String): Result<DailySummary>
}
```

**Parameters:**
- `date: String` - Date in format "yyyy-MM-dd"

**Returns:**
- `Result<DailySummary>` - Summary with total sales, transactions, etc.

#### `ProcessVoiceCommandUseCase`

Processes voice command and executes corresponding action.

```kotlin
class ProcessVoiceCommandUseCase @Inject constructor(
    private val transactionProcessor: TransactionProcessor
) {
    suspend operator fun invoke(transcript: String, speakerId: String): Result<CommandResult>
}
```

---

## Data Layer APIs

### Transaction Repository

```kotlin
interface TransactionRepository {
    suspend fun insertTransaction(transaction: Transaction): String
    suspend fun getAllTransactions(): List<Transaction>
    suspend fun getTransactionById(id: String): Transaction?
    suspend fun getTransactionsForDate(date: String): List<Transaction>
    suspend fun getTotalSalesForDate(date: String): Double
    suspend fun getTransactionCountForDate(date: String): Int
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(id: String)
    suspend fun getUnsynced Transactions(): List<Transaction>
    suspend fun markTransactionSynced(id: String)
    fun observeTransactions(): Flow<List<Transaction>>
}
```

### Product Vocabulary Repository

```kotlin
interface ProductVocabularyRepository {
    suspend fun getAllProducts(): List<ProductVocabulary>
    suspend fun findBestMatch(query: String): ProductVocabulary?
    suspend fun isValidProduct(productName: String): Boolean
    suspend fun validatePrice(productName: String, amount: Double): Boolean
    suspend fun incrementFrequency(productId: String)
    suspend fun insertProduct(product: ProductVocabulary)
    suspend fun updateProduct(product: ProductVocabulary)
}
```

### Audio Metadata Repository

```kotlin
interface AudioMetadataRepository {
    suspend fun insertMetadata(metadata: AudioMetadata)
    suspend fun getMetadataForChunk(chunkId: String): AudioMetadata?
    suspend fun getMetadataForTransaction(transactionId: String): List<AudioMetadata>
    suspend fun getMetadataForDateRange(startTime: Long, endTime: Long): List<AudioMetadata>
    suspend fun deleteOldMetadata(cutoffTime: Long): Int
}
```

---

## ML Component APIs

### TransactionStateMachine

Finite state machine for tracking transaction conversation flow.

```kotlin
class TransactionStateMachine @Inject constructor(
    private val patternMatcher: TransactionPatternMatcher
) {
    val currentState: StateFlow<TransactionState>
    val transactionCompleted: StateFlow<Transaction?>
    
    fun processUtterance(
        text: String,
        speakerId: String,
        isSeller: Boolean,
        timestamp: Long = System.currentTimeMillis()
    ): StateTransition
    
    fun getCurrentContext(): TransactionContext?
    fun reset()
    fun forceComplete(): Transaction?
    fun checkTimeout(currentTime: Long = System.currentTimeMillis())
}
```

**State Flow:**
```
IDLE → INQUIRY → PRICE_QUOTE → NEGOTIATION → AGREEMENT → PAYMENT → COMPLETE
                                     ↓
                                 CANCELLED
```

### TransactionProcessor

High-level coordinator for transaction detection.

```kotlin
class TransactionProcessor @Inject constructor(
    private val stateMachine: TransactionStateMachine,
    private val transactionRepository: TransactionRepository,
    private val productVocabularyRepository: ProductVocabularyRepository,
    private val audioMetadataRepository: AudioMetadataRepository
) {
    val processingState: StateFlow<ProcessingState>
    val detectedTransactions: SharedFlow<DetectedTransaction>
    
    suspend fun processUtterance(
        transcript: String,
        speakerId: String,
        isSeller: Boolean,
        confidence: Float,
        timestamp: Long = System.currentTimeMillis(),
        audioChunkId: String? = null
    ): TransactionProcessingResult
    
    fun getCurrentState(): TransactionState
    fun getCurrentContext(): TransactionContext?
    suspend fun forceCompleteTransaction(): Transaction?
    fun resetStateMachine()
    suspend fun getTransactionStats(): TransactionStats
}
```

### SpeechRecognitionManager

Multi-provider speech recognition manager.

```kotlin
class SpeechRecognitionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun transcribe(audioData: ByteArray): TranscriptionResult
    suspend fun transcribeStream(audioStream: Flow<ByteArray>): Flow<TranscriptionResult>
    fun optimizeForMarketEnvironment()
    fun setLanguage(languageCode: String)
    fun cleanup()
}
```

**TranscriptionResult:**
```kotlin
data class TranscriptionResult(
    val transcript: String,
    val confidence: Float,
    val language: String,
    val isFinal: Boolean,
    val alternatives: List<String> = emptyList(),
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)
```

### SpeakerIdentifier

TensorFlow Lite-based speaker identification.

```kotlin
class SpeakerIdentifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun initialize()
    suspend fun identifySpeaker(audioData: ByteArray): SpeakerResult
    suspend fun enrollSpeaker(speakerId: String, audioSamples: List<ByteArray>): Boolean
    suspend fun getSpeakerProfile(speakerId: String): SpeakerProfile?
    fun cleanup()
}
```

**SpeakerResult:**
```kotlin
data class SpeakerResult(
    val speakerId: String,
    val speakerType: SpeakerType,
    val confidence: Float,
    val embedding: FloatArray? = null
)

enum class SpeakerType {
    SELLER, CUSTOMER, UNKNOWN
}
```

### VADManager

Voice Activity Detection with power management.

```kotlin
class VADManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val sleepModeChanges: Flow<SleepMode>
    
    suspend fun initialize(vadType: VADType)
    suspend fun processAudioSample(audioData: ByteArray): VADResult
    fun startProcessing()
    fun stopProcessing()
    fun forceWakeUp()
    fun shouldUsePowerSavingMode(): Boolean
    fun destroy()
}
```

**VADResult:**
```kotlin
data class VADResult(
    val isSpeech: Boolean,
    val confidence: Float,
    val energyLevel: Float?,
    val timestamp: Long = System.currentTimeMillis()
)
```

---

## Service APIs

### VoiceAgentService

Background service for continuous audio monitoring.

```kotlin
class VoiceAgentService : Service() {
    val listeningState: StateFlow<ListeningState>
    var audioProcessingCallback: AudioProcessingCallback?
    
    fun startListening()
    fun pauseListening()
    fun stopListening()
}
```

**ListeningState:**
```kotlin
sealed class ListeningState {
    object STOPPED : ListeningState()
    object LISTENING : ListeningState()
    object PAUSED : ListeningState()
    object SLEEPING : ListeningState()
    data class ERROR(val message: String) : ListeningState()
}
```

**AudioProcessingCallback:**
```kotlin
interface AudioProcessingCallback {
    fun onAudioChunkProcessed(
        audioData: ShortArray,
        chunkId: String,
        timestamp: Long,
        speakerResult: SpeakerResult?
    )
}
```

### OfflineQueueManager

Manages offline operations and synchronization.

```kotlin
class OfflineQueueManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: VoiceLedgerDatabase
) {
    val queueState: StateFlow<OfflineQueueState>
    
    suspend fun enqueueOperation(operation: OfflineOperation)
    suspend fun processAllPendingOperations()
    suspend fun clearFailedOperations()
    fun getOperationsByType(type: OperationType): List<OfflineOperation>
    fun getFailedOperations(): List<OfflineOperation>
    fun configure(maxRetryAttempts: Int, retryDelayMs: Long, maxQueueSize: Int)
    fun cleanup()
}
```

---

## Data Models

### Transaction

```kotlin
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val date: String,
    val amount: Double,
    val currency: String = "GHS",
    val product: String,
    val quantity: Int? = null,
    val unit: String? = null,
    val customerId: String? = null,
    val confidence: Float,
    val transcriptSnippet: String? = null,
    val sellerConfidence: Float? = null,
    val customerConfidence: Float? = null,
    val needsReview: Boolean = false,
    val synced: Boolean = false,
    val originalPrice: Double? = null,
    val finalPrice: Double
)
```

### ProductVocabulary

```kotlin
@Entity(tableName = "product_vocabulary")
data class ProductVocabulary(
    @PrimaryKey val id: String,
    val canonicalName: String,
    val variants: List<String>,
    val category: String,
    val typicalPriceMin: Double? = null,
    val typicalPriceMax: Double? = null,
    val frequency: Int = 0,
    val language: String = "en"
)
```

### AudioMetadata

```kotlin
@Entity(tableName = "audio_metadata")
data class AudioMetadata(
    @PrimaryKey val chunkId: String,
    val timestamp: Long,
    val vadScore: Float,
    val speechDetected: Boolean,
    val speakerDetected: Boolean,
    val speakerId: String? = null,
    val speakerConfidence: Float? = null,
    val audioQuality: Float? = null,
    val durationMs: Long,
    val processingTimeMs: Long,
    val contributedToTransaction: Boolean,
    val transactionId: String? = null,
    val errorMessage: String? = null,
    val batteryLevel: Int?,
    val powerSavingMode: Boolean
)
```

### DailySummary

```kotlin
@Entity(tableName = "daily_summaries")
data class DailySummary(
    @PrimaryKey val date: String,
    val totalSales: Double,
    val transactionCount: Int,
    val topProducts: List<ProductSales>,
    val hourlyBreakdown: Map<Int, Double>,
    val generatedAt: Long,
    val synced: Boolean = false
)
```

---

## Error Handling

### Result Type

The app uses Kotlin's `Result` type for error handling:

```kotlin
sealed class AppError : Throwable() {
    data class NetworkError(override val message: String) : AppError()
    data class DatabaseError(override val message: String) : AppError()
    data class ValidationError(override val message: String) : AppError()
    data class PermissionError(override val message: String) : AppError()
    data class MLModelError(override val message: String) : AppError()
    data class AudioError(override val message: String) : AppError()
}
```

### Common Error Handling Pattern

```kotlin
suspend fun someOperation(): Result<Data> = try {
    val data = performOperation()
    Result.success(data)
} catch (e: Exception) {
    Log.e(TAG, "Operation failed", e)
    Result.failure(AppError.DatabaseError(e.message ?: "Unknown error"))
}
```

---

## API Best Practices

### 1. Always Use Coroutines for Async Operations

```kotlin
// Good
suspend fun fetchTransactions(): List<Transaction> {
    return withContext(Dispatchers.IO) {
        database.transactionDao().getAllTransactions()
    }
}

// Bad - Blocking main thread
fun fetchTransactions(): List<Transaction> {
    return database.transactionDao().getAllTransactions()
}
```

### 2. Use Flows for Reactive Data

```kotlin
// Observe data changes
viewModelScope.launch {
    transactionRepository.observeTransactions().collect { transactions ->
        _transactionState.value = transactions
    }
}
```

### 3. Handle Errors Gracefully

```kotlin
viewModelScope.launch {
    recordTransactionUseCase(transaction)
        .onSuccess { transactionId ->
            _uiState.value = UiState.Success(transactionId)
        }
        .onFailure { error ->
            _uiState.value = UiState.Error(error.message ?: "Unknown error")
        }
}
```

### 4. Use Dependency Injection

```kotlin
@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val recordTransactionUseCase: RecordTransactionUseCase,
    private val getDailySummaryUseCase: GetDailySummaryUseCase
) : ViewModel()
```

---

## Testing APIs

### Unit Testing Repositories

```kotlin
@Test
fun `insert transaction returns valid ID`() = runTest {
    val transaction = createTestTransaction()
    val id = repository.insertTransaction(transaction)
    assertNotNull(id)
    assertTrue(id.isNotEmpty())
}
```

### Testing ViewModels

```kotlin
@Test
fun `recording transaction updates UI state`() = runTest {
    val transaction = createTestTransaction()
    viewModel.recordTransaction(transaction)
    
    val state = viewModel.uiState.value
    assertTrue(state is UiState.Success)
}
```

### Testing ML Components

```kotlin
@Test
fun `state machine transitions correctly`() {
    val transition = stateMachine.processUtterance(
        text = "How much is tilapia?",
        speakerId = "customer_1",
        isSeller = false
    )
    
    assertEquals(TransactionState.INQUIRY, transition.toState)
    assertTrue(transition.confidence > 0.5f)
}
```

---

## Rate Limits and Quotas

### Speech Recognition
- **Google Cloud Speech API**: 60 minutes/month (free tier)
- **Offline TensorFlow Lite**: Unlimited

### Network Requests
- API calls: 1000 requests/hour per device
- Sync operations: Every 15 minutes when online

### Storage
- Local database: Unlimited (device storage dependent)
- Audio metadata: Auto-purge after 30 days
- Transaction history: Retained indefinitely

---

## Versioning

The API follows semantic versioning:
- **Major version**: Breaking changes
- **Minor version**: New features, backward compatible
- **Patch version**: Bug fixes

Current version: **1.0.0**

---

## Support

For API questions or issues:
- [GitHub Issues](https://github.com/voiceledger/ghana-voice-ledger/issues)
- [Developer Discord](https://discord.gg/voiceledger)
- Email: dev-support@voiceledger.com
