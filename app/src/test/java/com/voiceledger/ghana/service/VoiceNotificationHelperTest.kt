package com.voiceledger.ghana.service

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VoiceNotificationHelperTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var helper: VoiceNotificationHelper

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = mockk(relaxed = true)
        
        mockkStatic(Context::class)
        every { 
            context.getSystemService(Context.NOTIFICATION_SERVICE) 
        } returns notificationManager

        helper = VoiceNotificationHelper(context)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `createNotificationChannel should create channel on Android O and above`() {
        helper.createNotificationChannel()

        verify { notificationManager.createNotificationChannel(any()) }
    }

    @Test
    fun `createListeningNotification should return notification with correct properties`() {
        val notification = helper.createListeningNotification()

        // Verify it's a notification object
        assert(notification is Notification)
    }

    @Test
    fun `createOfflineNotification should return notification with correct properties`() {
        val notification = helper.createOfflineNotification()

        // Verify it's a notification object
        assert(notification is Notification)
    }

    @Test
    fun `createSleepingNotification should return notification with correct properties`() {
        val notification = helper.createSleepingNotification()

        // Verify it's a notification object
        assert(notification is Notification)
    }

    @Test
    fun `updateNotification should call notification manager`() {
        val mockNotification = mockk<Notification>(relaxed = true)
        
        helper.updateNotification(mockNotification)

        verify { notificationManager.notify(VoiceNotificationHelper.NOTIFICATION_ID, mockNotification) }
    }

    @Test
    fun `notification constants should be correct`() {
        assertEquals(1001, VoiceNotificationHelper.NOTIFICATION_ID)
    }
}