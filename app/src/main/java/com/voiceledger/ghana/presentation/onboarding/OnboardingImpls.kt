package com.voiceledger.ghana.presentation.onboarding

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.voiceledger.ghana.data.local.prefs.AppPreferences
import javax.inject.Inject
import javax.inject.Singleton

/** Whether the trader has been through first-run setup. */
@Singleton
class OnboardingRepositoryImpl @Inject constructor(
    private val prefs: AppPreferences
) : OnboardingRepository {

    override suspend fun isOnboardingComplete(): Boolean =
        prefs.getBoolean(KEY_ONBOARDING_COMPLETE)

    override suspend fun setOnboardingComplete(complete: Boolean) =
        prefs.putBoolean(KEY_ONBOARDING_COMPLETE, complete)

    private companion object {
        const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    }
}

/**
 * Runtime permission state.
 *
 * [hasPermission] is a real check. [requestPermission] cannot be: showing the system
 * dialog needs an Activity and its result callback, which this interface has no way
 * to reach. It therefore re-checks rather than prompting, and returns the current
 * state. The onboarding screen must launch the request itself with
 * rememberLauncherForActivityResult and call back here afterwards.
 */
@Singleton
class PermissionManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PermissionManager {

    override suspend fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /** Does not prompt — see the class note. Returns the current grant state. */
    override suspend fun requestPermission(permission: String): Boolean = hasPermission(permission)
}

/**
 * Voice enrollment progress.
 *
 * INCOMPLETE: records only whether enrollment finished. It does not capture audio or
 * compute a speaker embedding, because the enrollment pipeline does not exist yet —
 * see the speaker gate work in sikabook-models. [startEnrollment] deliberately does
 * nothing rather than appear to record the trader's voice.
 */
@Singleton
class VoiceEnrollmentManagerImpl @Inject constructor(
    private val prefs: AppPreferences
) : VoiceEnrollmentManager {

    override suspend fun startEnrollment() {
        // No capture pipeline yet. Intentionally a no-op.
    }

    override suspend fun completeEnrollment() =
        prefs.putBoolean(KEY_ENROLLED, true)

    override suspend fun isEnrollmentComplete(): Boolean =
        prefs.getBoolean(KEY_ENROLLED)

    private companion object {
        const val KEY_ENROLLED = "voice_enrollment_complete"
    }
}
