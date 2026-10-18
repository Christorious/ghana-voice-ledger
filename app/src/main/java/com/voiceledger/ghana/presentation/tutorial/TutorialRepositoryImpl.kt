package com.voiceledger.ghana.presentation.tutorial

import com.voiceledger.ghana.data.local.prefs.AppPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers which tutorials the trader has finished or skipped, in plain
 * preferences. Nothing here is sensitive.
 */
@Singleton
class TutorialRepositoryImpl @Inject constructor(
    private val prefs: AppPreferences
) : TutorialRepository {

    private fun completedKey(type: TutorialType) = "tutorial_${type.name}_completed"
    private fun skippedKey(type: TutorialType) = "tutorial_${type.name}_skipped"
    private fun stepsKey(type: TutorialType) = "tutorial_${type.name}_steps"
    private fun seenKey(type: TutorialType) = "tutorial_${type.name}_last_seen"

    override suspend fun markTutorialCompleted(type: TutorialType) {
        prefs.putBoolean(completedKey(type), true)
        prefs.putBoolean(skippedKey(type), false)
        prefs.putLong(seenKey(type), System.currentTimeMillis())
    }

    override suspend fun markTutorialSkipped(type: TutorialType) {
        prefs.putBoolean(skippedKey(type), true)
        prefs.putLong(seenKey(type), System.currentTimeMillis())
    }

    override suspend fun isTutorialCompleted(type: TutorialType): Boolean =
        prefs.getBoolean(completedKey(type))

    override suspend fun getTutorialProgress(type: TutorialType): TutorialProgress {
        val completed = prefs.getBoolean(completedKey(type))
        val totalSteps = prefs.getInt(stepsKey(type), 0)
        return TutorialProgress(
            type = type,
            isCompleted = completed,
            isSkipped = prefs.getBoolean(skippedKey(type)),
            completedSteps = if (completed) totalSteps else 0,
            totalSteps = totalSteps,
            lastAccessedDate = prefs.getLong(seenKey(type))
        )
    }
}
