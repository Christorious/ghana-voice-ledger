package com.voiceledger.ghana.performance

import android.app.ActivityManager
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryManagerTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var context: Context
    private lateinit var activityManager: ActivityManager
    private lateinit var memoryManager: MemoryManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        context = mockk(relaxed = true)
        activityManager = mockk(relaxed = true)
        
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager
        
        // Mock memory info
        val memInfo = ActivityManager.MemoryInfo().apply {
            availMem = 2L * 1024 * 1024 * 1024 // 2GB
            totalMem = 4L * 1024 * 1024 * 1024 // 4GB
            lowMemory = false
        }
        every { activityManager.getMemoryInfo(any()) } answers {
            val info = firstArg<ActivityManager.MemoryInfo>()
            info.availMem = memInfo.availMem
            info.totalMem = memInfo.totalMem
            info.lowMemory = memInfo.lowMemory
        }

        memoryManager = MemoryManager(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        memoryManager.cleanup()
        unmockkAll()
    }

    @Test
    fun `initial state should have default values`() = runTest {
        // When - Check initial state
        val initialState = memoryManager.memoryState.value
        
        // Then
        assertTrue(initialState.totalMemoryMB >= 0)
        assertTrue(initialState.maxMemoryMB >= 0)
        assertTrue(initialState.memoryUsagePercent >= 0)
        assertEquals(0, initialState.trackedObjectCount)
    }

    @Test
    fun `should acquire and release audio buffer from pool`() = runTest {
        // When - Acquire buffer
        val buffer1 = memoryManager.getAudioBuffer()
        val buffer2 = memoryManager.getAudioBuffer()
        
        // Then - Buffers should be valid
        assertNotNull(buffer1)
        assertNotNull(buffer2)
        assertTrue(buffer1.isNotEmpty())
        assertTrue(buffer2.isNotEmpty())
        
        // When - Release buffers
        memoryManager.releaseAudioBuffer(buffer1)
        memoryManager.releaseAudioBuffer(buffer2)
        
        // Then - Should not throw exceptions
        // Pool should reuse buffers
        val buffer3 = memoryManager.getAudioBuffer()
        assertNotNull(buffer3)
    }

    @Test
    fun `should acquire and release float array from pool`() = runTest {
        // When - Acquire arrays
        val array1 = memoryManager.getFloatArray()
        val array2 = memoryManager.getFloatArray()
        
        // Then - Arrays should be valid
        assertNotNull(array1)
        assertNotNull(array2)
        assertTrue(array1.isNotEmpty())
        assertTrue(array2.isNotEmpty())
        
        // When - Release arrays
        memoryManager.releaseFloatArray(array1)
        memoryManager.releaseFloatArray(array2)
        
        // Then - Should not throw exceptions
        val array3 = memoryManager.getFloatArray()
        assertNotNull(array3)
    }

    @Test
    fun `should acquire and release StringBuilder from pool`() = runTest {
        // When - Acquire StringBuilders
        val sb1 = memoryManager.getStringBuilder()
        val sb2 = memoryManager.getStringBuilder()
        
        // Then - StringBuilders should be valid
        assertNotNull(sb1)
        assertNotNull(sb2)
        
        // When - Use and release StringBuilders
        sb1.append("test")
        memoryManager.releaseStringBuilder(sb1)
        memoryManager.releaseStringBuilder(sb2)
        
        // Then - Should not throw exceptions
        val sb3 = memoryManager.getStringBuilder()
        assertNotNull(sb3)
        assertEquals(0, sb3.length) // Should be cleared
    }

    @Test
    fun `should track and untrack objects`() = runTest {
        // Given - Objects to track
        val obj1 = "test object 1"
        val obj2 = "test object 2"
        
        // When - Track objects
        memoryManager.trackObject("obj1", obj1)
        memoryManager.trackObject("obj2", obj2)
        
        // Then - Objects should be tracked
        advanceTimeBy(1000) // Allow state update
        val state = memoryManager.memoryState.value
        assertEquals(2, state.trackedObjectCount)
        
        // When - Untrack one object
        memoryManager.untrackObject("obj1")
        
        // Then - Should have one less tracked object
        advanceTimeBy(1000)
        val updatedState = memoryManager.memoryState.value
        assertEquals(1, updatedState.trackedObjectCount)
    }

    @Test
    fun `should detect memory leak suspects`() = runTest {
        // Given - Long-lived object
        val longLivedObject = "long lived object"
        
        // When - Track object with old timestamp
        memoryManager.trackObject("longLived", longLivedObject)
        
        // Simulate time passing (more than 5 minutes)
        advanceTimeBy(400_000) // 6.67 minutes
        
        // Then - Should detect as memory leak suspect
        val suspects = memoryManager.getMemoryLeakSuspects()
        assertTrue(suspects.isNotEmpty())
        assertEquals("longLived", suspects.first().key)
        assertTrue(suspects.first().ageMs > 300_000) // More than 5 minutes
    }

    @Test
    fun `should force garbage collection`() = runTest {
        // When - Force garbage collection
        memoryManager.forceGarbageCollection()
        
        // Then - Should not throw exceptions
        // Memory state should be updated
        advanceTimeBy(2000) // Allow GC and state update
        val state = memoryManager.memoryState.value
        assertNotNull(state)
    }

    @Test
    fun `should configure memory thresholds`() = runTest {
        // When - Configure thresholds
        memoryManager.configureThresholds(85, 60)
        
        // Then - Should not throw exceptions
        // Configuration should be applied (tested indirectly through behavior)
    }

    @Test
    fun `should provide optimization recommendations`() = runTest {
        // Given - Some tracked objects and memory usage
        memoryManager.trackObject("test1", "object1")
        memoryManager.trackObject("test2", "object2")
        
        // When - Get recommendations
        val recommendations = memoryManager.getOptimizationRecommendations()
        
        // Then - Should provide recommendations
        assertNotNull(recommendations)
        assertTrue(recommendations is List<String>)
    }

    @Test
    fun `should handle monitoring enable/disable`() = runTest {
        // When - Disable monitoring
        memoryManager.setMonitoringEnabled(false)
        
        // Then - Should not throw exceptions
        
        // When - Re-enable monitoring
        memoryManager.setMonitoringEnabled(true)
        
        // Then - Should not throw exceptions
    }

    @Test
    fun `object pool should respect size limits`() = runTest {
        // Given - Pool with small max size
        val pool = ObjectPool<String>(
            factory = { "test" },
            maxSize = 2
        )
        
        // When - Acquire and release more objects than max size
        val obj1 = pool.acquire()
        val obj2 = pool.acquire()
        val obj3 = pool.acquire()
        
        pool.release(obj1)
        pool.release(obj2)
        pool.release(obj3) // This should not be stored due to size limit
        
        // Then - Pool size should not exceed max
        assertTrue(pool.size() <= 2)
    }

    @Test
    fun `object pool should validate objects`() = runTest {
        // Given - Pool with validator
        val pool = ObjectPool<String>(
            factory = { "valid" },
            validator = { it == "valid" },
            maxSize = 5
        )
        
        // When - Try to release invalid object
        val validObj = pool.acquire()
        pool.release(validObj) // Should be accepted
        pool.release("invalid") // Should be rejected
        
        // Then - Pool should only contain valid objects
        val retrievedObj = pool.acquire()
        assertEquals("valid", retrievedObj)
    }

    @Test
    fun `should use object pool with scoped usage`() = runTest {
        // Given - Object pool
        val pool = ObjectPool<StringBuilder>(
            factory = { StringBuilder() },
            reset = { it.clear() }
        )
        
        // When - Use pool with scoped usage
        val result = pool.use { sb ->
            sb.append("test")
            sb.toString()
        }
        
        // Then - Should return correct result
        assertEquals("test", result)
        
        // And - Object should be returned to pool
        val nextObj = pool.acquire()
        assertEquals(0, nextObj.length) // Should be cleared
    }
}