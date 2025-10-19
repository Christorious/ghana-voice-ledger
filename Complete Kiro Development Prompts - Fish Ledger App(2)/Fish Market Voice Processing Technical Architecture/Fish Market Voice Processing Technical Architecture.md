- Fish Market Voice Processing Technical Architecture ## System Overview

  ### \*\*High-Level Architecture\*\*

  ```

  [Phone Microphone] → [Audio Preprocessing] → [Voice Activity Detection]

` `↓

[Transaction Extraction] ← [Speaker Identification] ← [Speech-to-Text]

` `↓

[Confidence Scoring] → [Local Database] → [Daily Summary] → [User Interface] ```

\### \*\*Core Design Principles\*\*

1. \*\*On-Device Processing\*\*: Privacy and offline capability
1. \*\*Low Battery Usage\*\*: Optimized for all-day listening
1. \*\*Noise Resilience\*\*: Works in busy market environments
1. \*\*Real-time Processing\*\*: Minimal delay between speech and recognition
1. \*\*Graceful Degradation\*\*: Works even with poor audio quality

\## Detailed Component Architecture

\### \*\*1. Audio Capture & Preprocessing Layer\*\*

\#### \*\*Audio Input Manager\*\*

\```kotlin

// Android Implementation Example

class AudioInputManager {

` `private val audioRecord: AudioRecord

` `private val bufferSize: Int = 8192

` `private val sampleRate: Int = 16000 // Optimized for speech

` `// Continuous audio capture with circular buffer

` `fun startListening() {

` `// Use low-latency audio capture

` `// Implement noise gate to ignore very quiet audio

` `// Circular buffer to keep last 30 seconds

` `}

` `// Smart listening - only process when likely transaction  fun detectSpeechActivity(): Boolean {

` `// Use volume threshold + frequency analysis

` `// Detect multiple speakers (seller + customer)

` `// Filter out pure background noise

` `}

} ```

\#### \*\*Audio Preprocessing Pipeline\*\*

\```

Raw Audio (16kHz, 16-bit)

` `↓

Noise Reduction (Spectral Subtraction)

` `↓

Voice Activity Detection (VAD)

` `↓

Speaker Separation (Basic 2-speaker model)

` `↓

Audio Segmentation (Transaction chunks)

` `↓

Feature Extraction (MFCC coefficients)

\```

\*\*Key Technologies\*\*:

- \*\*WebRTC VAD\*\*: Excellent voice activity detection
- \*\*RNNoise\*\*: Real-time noise suppression optimized for mobile
- \*\*Librosa/TensorFlow\*\*: Audio feature extraction

\### \*\*2. Speech Recognition Engine\*\*

\#### \*\*Multi-Stage Recognition Pipeline\*\* ```python

class FishMarketSTT:

` `def \_\_init\_\_(self):

- Stage 1: General speech recognition

` `self.general\_stt = WhisperModel("base.en")

- Stage 2: Fish market specific model

` `self.fish\_stt = CustomModel("fish\_market\_v1.tflite")

- Stage 3: Local language components

` `self.twi\_stt = LocalLanguageModel("twi\_basic.tflite")

` `def process\_audio(self, audio\_chunk):

- Parallel processing for speed

` `general\_text = self.general\_stt.transcribe(audio\_chunk)  fish\_text = self.fish\_stt.transcribe(audio\_chunk)

` `local\_text = self.twi\_stt.transcribe(audio\_chunk)

- Confidence weighted combination

` `return self.combine\_results(general\_text, fish\_text, local\_text)

\```

\#### \*\*Custom Fish Market Model Training\*\* ```python

- Training Data Structure

training\_samples = {

` `"transaction\_type": "price\_inquiry",

` `"audio\_file": "sample\_001.wav",

` `"transcript": "How much be this tilapia?",

` `"speaker": "customer",

` `"confidence": 0.95,

` `"fish\_type": "tilapia",

` `"price\_mentioned": None,

` `"language\_mix": ["english", "pidgin"]

}

- Model Architecture

class FishMarketASR(tf.keras.Model):

` `def \_\_init\_\_(self):

- Transformer-based architecture optimized for:
- - Code-switching (English + Twi)
- - Fish-specific vocabulary
- - Market noise resilience
- - Low-latency inference

\```

\### \*\*3. Transaction Detection & Parsing\*\*

\#### \*\*Transaction State Machine\*\* ```python

class TransactionStateMachine:

` `def \_\_init\_\_(self):

` `self.state = "IDLE"

` `self.current\_transaction = {}

` `self.confidence\_threshold = 0.7

` `def process\_utterance(self, text, speaker, confidence):

` `if self.state == "IDLE":

` `if self.is\_price\_inquiry(text):

` `self.state = "PRICE\_INQUIRY"

` `self.start\_new\_transaction(text, speaker)

` `elif self.state == "PRICE\_INQUIRY":

` `if self.is\_price\_response(text, speaker):

` `self.state = "PRICE\_GIVEN"

` `self.add\_price\_info(text)

` `elif self.state == "PRICE\_GIVEN":

` `if self.is\_payment\_confirmation(text):

` `self.state = "PAYMENT"

` `self.complete\_transaction()

- Timeout handling - reset if no progress for 2 minutes

` `self.check\_timeout()

` `def is\_price\_inquiry(self, text):

` `patterns = [

` `r"how much.\*fish",

` `r"what.\*price",

` `r"sɛn na ɛyɛ",

` `r"how much be",

` `r"price.\*tilapia"

` `]

` `return any(re.search(pattern, text.lower()) for pattern in patterns)

\```

\#### \*\*Entity Extraction Engine\*\*

\```python

class FishMarketNER:

` `def \_\_init\_\_(self):

- Custom NER model for fish market entities

` `self.fish\_patterns = self.load\_fish\_patterns()

` `self.price\_patterns = self.load\_price\_patterns()

` `self.quantity\_patterns = self.load\_quantity\_patterns()

` `def extract\_entities(self, text):

` `entities = {

` `"fish\_type": self.extract\_fish\_type(text),  "price": self.extract\_price(text),

` `"quantity": self.extract\_quantity(text),

` `"currency": self.extract\_currency(text)  }

` `return entities

` `def extract\_fish\_type(self, text):

- Pattern matching + fuzzy matching for fish names

` `fish\_keywords = {

` `"tilapia": ["tilapia", "tuo", "apateshi"],

` `"tuna": ["tuna", "light meat"],

` `"mackerel": ["mackerel", "kpanla", "titus"],

` `"sardine": ["sardine", "herring"]

` `}

- Use fuzzy string matching for variations

` `from fuzzywuzzy import fuzz

- Implementation details...

\```

\### \*\*4. Speaker Identification System\*\*

\#### \*\*Simple Speaker Diarization\*\* ```python

class SpeakerIdentifier:

` `def \_\_init\_\_(self):

- Lightweight speaker identification

` `self.seller\_voice\_profile = None

` `self.current\_speakers = {}

` `def learn\_seller\_voice(self, audio\_samples):

- Build seller voice profile during setup
- Extract speaker embeddings (x-vectors)
- Store characteristic features

` `pass

` `def identify\_speaker(self, audio\_chunk):

- Real-time speaker identification
- Return: "seller", "customer", or "unknown"

` `embedding = self.extract\_speaker\_embedding(audio\_chunk)

` `if self.is\_similar\_to\_seller(embedding):

` `return "seller"

` `elif self.is\_new\_speaker(embedding):

` `return "customer"

` `else:

` `return "unknown"

\```

\### \*\*5. Confidence Scoring & Quality Control\*\* #### \*\*Multi-Dimensional Confidence Scoring\*\*

\```python

class ConfidenceScorer:

` `def score\_transaction(self, transaction\_data):

` `scores = {

` `"audio\_quality": self.score\_audio\_quality(transaction\_data.audio),

` `"speech\_clarity": self.score\_speech\_clarity(transaction\_data.text),

` `"transaction\_completeness": self.score\_completeness(transaction\_data),  "entity\_confidence": self.score\_entities(transaction\_data.entities),

` `"pattern\_match": self.score\_pattern\_match(transaction\_data)

` `}

- Weighted combination

` `total\_confidence = (

` `scores["audio\_quality"] \* 0.2 +

` `scores["speech\_clarity"] \* 0.25 +

` `scores["transaction\_completeness"] \* 0.3 +  scores["entity\_confidence"] \* 0.15 +

` `scores["pattern\_match"] \* 0.1

` `)

` `return total\_confidence, scores

` `def should\_auto\_record(self, confidence):

` `return confidence > 0.8

` `def should\_flag\_for\_review(self, confidence):

` `return 0.5 < confidence <= 0.8

\```

\### \*\*6. Data Storage & Management\*\*

\#### \*\*Local Database Schema\*\*

\```sql

-- Transactions table

CREATE TABLE transactions (

` `id INTEGER PRIMARY KEY AUTOINCREMENT,

` `timestamp DATETIME DEFAULT CURRENT\_TIMESTAMP,

` `fish\_type TEXT,

` `quantity INTEGER,

` `unit\_price REAL,

` `total\_amount REAL,

` `currency TEXT DEFAULT 'GHS',

` `confidence\_score REAL,

` `raw\_audio\_hash TEXT,

` `transcript TEXT,

` `status TEXT DEFAULT 'confirmed',

` `created\_at DATETIME DEFAULT CURRENT\_TIMESTAMP

);

-- Daily summaries table

CREATE TABLE daily\_summaries (

` `date DATE PRIMARY KEY,

` `total\_sales REAL,

` `transaction\_count INTEGER,

` `top\_fish\_type TEXT,

` `average\_transaction REAL,

` `confidence\_avg REAL

);

-- Audio metadata (for debugging/improvement) CREATE TABLE audio\_logs (

` `id INTEGER PRIMARY KEY,

` `timestamp DATETIME,

` `duration\_seconds REAL,

` `noise\_level REAL,

` `speaker\_count INTEGER,

` `processed BOOLEAN DEFAULT FALSE

);

\```

\#### \*\*Data Synchronization Strategy\*\* ```python

class DataSyncManager:

` `def \_\_init\_\_(self):

` `self.local\_db = SQLiteDatabase("fish\_ledger.db")  self.cloud\_backup = FirebaseDatabase()

` `self.sync\_interval = 3600 # 1 hour

` `def sync\_to\_cloud(self):

- Upload only transaction summaries, not raw audio
- Encrypt sensitive data
- Handle offline/online transitions

` `pending\_transactions = self.local\_db.get\_unsynced()

` `for transaction in pending\_transactions:

- Remove audio data before upload

` `clean\_transaction = self.remove\_audio\_data(transaction)  self.cloud\_backup.save(clean\_transaction)

\```

\### \*\*7. Mobile App Integration Architecture\*\*

\#### \*\*Flutter App Structure\*\*

\```dart

// Main app architecture

class FishLedgerApp extends StatelessWidget {

` `@override

` `Widget build(BuildContext context) {

` `return MaterialApp(

` `home: BlocProvider(

` `create: (context) => VoiceProcessingBloc(),  child: MainScreen(),

` `),

` `);

` `}

}

// Voice processing service

class VoiceProcessingService {

` `static const platform = MethodChannel('fish\_ledger/voice');

` `Future<void> startListening() async {

` `// Start native audio processing

` `await platform.invokeMethod('startVoiceProcessing');

` `}

` `Stream<Transaction> get transactionStream {

` `// Stream of detected transactions

` `return platform.invokeMethod('getTransactionStream');  }

}

\```

\#### \*\*Background Processing\*\*

\```kotlin

// Android background service

class VoiceProcessingService : Service() {

` `private lateinit var audioProcessor: AudioProcessor

` `private lateinit var transactionDetector: TransactionDetector

` `override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

` `// Start foreground service for continuous listening

` `startForeground(NOTIFICATION\_ID, createNotification())

` `// Initialize audio processing pipeline

` `audioProcessor.startListening { audioChunk ->  transactionDetector.process(audioChunk)

` `}

` `return START\_STICKY // Restart if killed

` `}

` `private fun createNotification(): Notification {

` `// Show subtle notification that app is listening

` `return NotificationCompat.Builder(this, CHANNEL\_ID)

.setContentTitle("Fish Ledger Active")

.setContentText("Listening for sales...")

.setSmallIcon(R.drawable.ic\_mic)

.build()

` `}

}

\```

\## Performance Optimization

\### \*\*Battery Life Optimization\*\*

\```python

class PowerManager:

` `def \_\_init\_\_(self):

` `self.listening\_mode = "SMART" # FULL, SMART, MINIMAL  self.market\_hours = (6, 18) # 6 AM to 6 PM

` `self.activity\_threshold = 0.3

` `def should\_listen\_actively(self):

` `current\_hour = datetime.now().hour

- Only active listening during market hours

` `if not (self.market\_hours[0] <= current\_hour <= self.market\_hours[1]):

` `return False

- Reduce activity if phone is in pocket (low motion)

` `if self.get\_phone\_activity() < self.activity\_threshold:

` `return False

` `return True

` `def adjust\_processing\_intensity(self):

` `battery\_level = self.get\_battery\_level()

` `if battery\_level < 20:

` `return "MINIMAL" # Basic detection only  elif battery\_level < 50:

` `return "SMART" # Optimized processing  else:

` `return "FULL" # All features active

\```

\### \*\*Memory Management\*\*

\```python

class MemoryManager:

` `def \_\_init\_\_(self):

` `self.max\_audio\_buffer = 30 # seconds  self.max\_transactions\_cache = 100

` `self.cleanup\_interval = 300 # 5 minutes

` `def manage\_audio\_buffer(self):

- Keep only recent audio for context
- Delete processed audio older than 30 seconds
- Compress older audio if needed for debugging

` `pass

` `def cleanup\_old\_data(self):

- Remove old temporary files
- Compress old transaction data
- Clear ML model caches

` `pass

\```

\## Development Implementation Plan

\### \*\*Phase 1: Core Processing Engine (Months 1-2)\*\* ```

Priority Components:

1. Basic audio capture and preprocessing
1. Simple speech-to-text (English only)
1. Pattern matching for price/fish keywords
1. Local SQLite database
1. Basic confidence scoring

Technology Stack:

- Flutter for mobile app
- TensorFlow Lite for on-device ML
- SQLite for local storage
- WebRTC for audio processing ```

\### \*\*Phase 2: Enhanced Recognition (Months 3-4)\*\* ```

Enhanced Components:

1. Multi-language support (Twi integration)
1. Speaker identification
1. Transaction state machine
1. Improved confidence scoring
1. Background service optimization

Additional Technologies:

- Custom trained models
- Advanced audio preprocessing
- Cloud backup integration

\```

\### \*\*Phase 3: Production Ready (Months 5-6)\*\* ```

Production Features:

1. Full error handling and recovery
1. Performance optimization
1. User feedback integration
1. Analytics and monitoring
1. Security and privacy features

Deployment Stack:

- Firebase for backend services
- Crashlytics for error tracking
- Analytics for usage monitoring ```

\## Testing Strategy

\### \*\*Audio Testing Framework\*\*

\```python

class AudioTestSuite:

` `def \_\_init\_\_(self):

` `self.test\_audio\_samples = self.load\_test\_samples()

` `self.ground\_truth\_transactions = self.load\_ground\_truth()

` `def test\_recognition\_accuracy(self):

- Test with various audio conditions

` `results = {}

` `for sample in self.test\_audio\_samples:

` `predicted = self.voice\_processor.process(sample.audio)  expected = sample.ground\_truth

` `accuracy = self.calculate\_accuracy(predicted, expected)  results[sample.id] = accuracy

` `return results

` `def test\_noise\_resilience(self):

- Add synthetic noise to clean samples
- Test performance degradation

` `pass

\```

This architecture prioritizes Luther's principles: it's accessible (works on basic smartphones), practical (solves real problems), and focuses on authentic usage patterns (real market conversations). The key is starting simple and building complexity gradually based on real user feedback.
