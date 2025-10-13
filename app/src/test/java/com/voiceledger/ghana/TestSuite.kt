package com.voiceledger.ghana

import com.voiceledger.ghana.data.repository.TransactionRepositoryImplTest
import com.voiceledger.ghana.ml.entity.GhanaEntityExtractorTest
import com.voiceledger.ghana.ml.speaker.TensorFlowLiteSpeakerIdentifierTest
import com.voiceledger.ghana.ml.transaction.TransactionStateMachineTest
import com.voiceledger.ghana.security.EncryptionServiceTest
import com.voiceledger.ghana.security.PrivacyManagerTest
import com.voiceledger.ghana.security.SecurityManagerTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Comprehensive test suite for Ghana Voice Ledger
 * Runs all unit tests to achieve minimum 80% code coverage
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    // Security Tests
    EncryptionServiceTest::class,
    PrivacyManagerTest::class,
    SecurityManagerTest::class,
    
    // ML Component Tests
    TransactionStateMachineTest::class,
    GhanaEntityExtractorTest::class,
    TensorFlowLiteSpeakerIdentifierTest::class,
    
    // Repository Tests
    TransactionRepositoryImplTest::class
)
class TestSuite