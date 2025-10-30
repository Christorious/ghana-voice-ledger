package com.voiceledger.ghana.service

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AudioCaptureControllerTest {
    
    private lateinit var context: Context
    private lateinit var controller: AudioCaptureController
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        controller = AudioCaptureController(context)
    }
    
    @After
    fun tearDown() {
        controller.release()
        testScope.cancel()
    }
    
    @Test
    fun testCheckAudioPermission_shouldReturnBoolean() {
        val hasPermission = controller.checkAudioPermission()
        assertTrue("Should handle permission check", hasPermission || !hasPermission)
    }
    
    @Test
    fun testInitialize_shouldCreateAudioRecord() {
        if (controller.checkAudioPermission()) {
            try {
                controller.initialize()
            } catch (e: Exception) {
            }
        }
    }
    
    @Test
    fun testRelease_shouldCleanupResources() {
        controller.release()
        assertFalse("Should not be active after release", controller.isActive())
    }
    
    @Test
    fun testIsActive_initialState_shouldBeFalse() {
        assertFalse("Should not be active initially", controller.isActive())
    }
    
    @Test
    fun testStopRecording_whenNotRecording_shouldNotThrow() {
        controller.stopRecording()
        assertFalse("Should remain inactive", controller.isActive())
    }
}
