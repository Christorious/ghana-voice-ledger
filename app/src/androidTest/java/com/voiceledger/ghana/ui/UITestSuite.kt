package com.voiceledger.ghana.ui

import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Comprehensive UI test suite for Ghana Voice Ledger
 * Runs all UI tests with Compose testing framework
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    DashboardScreenTest::class,
    NavigationTest::class,
    AccessibilityTest::class,
    ResponsiveDesignTest::class
)
class UITestSuite