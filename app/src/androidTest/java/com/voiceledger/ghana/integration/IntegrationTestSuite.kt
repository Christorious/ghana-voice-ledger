package com.voiceledger.ghana.integration

import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Comprehensive integration test suite for Ghana Voice Ledger
 * Runs all integration tests to validate end-to-end functionality
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    AudioProcessingPipelineTest::class,
    OfflineFunctionalityTest::class,
    MultiLanguageIntegrationTest::class,
    BatteryUsageIntegrationTest::class
)
class IntegrationTestSuite