package com.voiceledger.ghana.presentation.feedback

import android.app.ActivityManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.voiceledger.ghana.BuildConfig
import com.voiceledger.ghana.data.local.prefs.AppPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps beta feedback on the device.
 *
 * There is no backend to submit to, so "submit" means "store locally" and the
 * history is read back from the same place. When a server exists, this is the one
 * class that changes. Feedback can contain whatever the trader typed, so it is held
 * in app preferences on her device and never leaves it.
 */
@Singleton
class LocalFeedbackRepository @Inject constructor(
    private val prefs: AppPreferences
) : FeedbackRepository {

    override suspend fun submitFeedback(feedback: Feedback) {
        val entries = readRaw()
        entries.put(
            JSONObject()
                .put("type", feedback.type.name)
                .put("rating", feedback.rating)
                .put("category", feedback.category.name)
                .put("description", feedback.description)
                .put("email", feedback.email ?: "")
                .put("timestamp", feedback.timestamp)
        )
        prefs.putString(KEY_FEEDBACK, entries.toString())
    }

    override suspend fun getFeedbackHistory(): List<Feedback> {
        val entries = readRaw()
        return (0 until entries.length()).mapNotNull { i ->
            runCatching {
                val o = entries.getJSONObject(i)
                Feedback(
                    type = FeedbackType.valueOf(o.getString("type")),
                    rating = o.getInt("rating"),
                    category = FeedbackCategory.valueOf(o.getString("category")),
                    description = o.getString("description"),
                    email = o.getString("email").ifBlank { null },
                    deviceInfo = null,
                    timestamp = o.getLong("timestamp")
                )
            }.getOrNull()
        }
    }

    private fun readRaw(): JSONArray =
        runCatching { JSONArray(prefs.getString(KEY_FEEDBACK, "[]")) }.getOrElse { JSONArray() }

    private companion object {
        const val KEY_FEEDBACK = "beta_feedback"
    }
}

/** Device facts that help make sense of a bug report. No identifiers. */
@Singleton
class AndroidDeviceInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : DeviceInfoProvider {

    override suspend fun getDeviceInfo(): DeviceInfo {
        val memoryInfo = ActivityManager.MemoryInfo().also {
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(it)
        }
        val stat = StatFs(Environment.getDataDirectory().path)

        return DeviceInfo(
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            appVersion = BuildConfig.VERSION_NAME,
            buildType = BuildConfig.BUILD_TYPE,
            locale = Locale.getDefault().toLanguageTag(),
            screenDensity = context.resources.displayMetrics.densityDpi.toString(),
            availableMemory = memoryInfo.availMem,
            totalStorage = stat.blockCountLong * stat.blockSizeLong,
            batteryLevel = batteryLevel()
        )
    }

    private fun batteryLevel(): Int? = runCatching {
        (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
            .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }.getOrNull()?.takeIf { it in 0..100 }
}
