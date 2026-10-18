package com.voiceledger.ghana.di

import com.google.gson.Gson
import com.voiceledger.ghana.ml.audio.AudioUtils
import com.voiceledger.ghana.offline.NoRemoteTransactionApi
import com.voiceledger.ghana.offline.RemoteTransactionApi
import com.voiceledger.ghana.presentation.feedback.AndroidDeviceInfoProvider
import com.voiceledger.ghana.presentation.feedback.DeviceInfoProvider
import com.voiceledger.ghana.presentation.feedback.FeedbackRepository
import com.voiceledger.ghana.presentation.feedback.LocalFeedbackRepository
import com.voiceledger.ghana.presentation.onboarding.OnboardingRepository
import com.voiceledger.ghana.presentation.onboarding.OnboardingRepositoryImpl
import com.voiceledger.ghana.presentation.onboarding.PermissionManager
import com.voiceledger.ghana.presentation.onboarding.PermissionManagerImpl
import com.voiceledger.ghana.presentation.onboarding.VoiceEnrollmentManager
import com.voiceledger.ghana.presentation.onboarding.VoiceEnrollmentManagerImpl
import com.voiceledger.ghana.presentation.tutorial.TutorialRepository
import com.voiceledger.ghana.presentation.tutorial.TutorialRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Bindings for the interfaces declared alongside their ViewModels.
 *
 * These were referenced throughout the app but never bound, so the Hilt graph could
 * not be built. Each implementation lives next to the interface it satisfies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindingsModule {

    @Binds
    @Singleton
    abstract fun bindTutorialRepository(impl: TutorialRepositoryImpl): TutorialRepository

    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(impl: OnboardingRepositoryImpl): OnboardingRepository

    @Binds
    @Singleton
    abstract fun bindPermissionManager(impl: PermissionManagerImpl): PermissionManager

    @Binds
    @Singleton
    abstract fun bindVoiceEnrollmentManager(impl: VoiceEnrollmentManagerImpl): VoiceEnrollmentManager

    @Binds
    @Singleton
    abstract fun bindFeedbackRepository(impl: LocalFeedbackRepository): FeedbackRepository

    @Binds
    @Singleton
    abstract fun bindDeviceInfoProvider(impl: AndroidDeviceInfoProvider): DeviceInfoProvider

    @Binds
    @Singleton
    abstract fun bindRemoteTransactionApi(impl: NoRemoteTransactionApi): RemoteTransactionApi
}

/** Plain providers that cannot be expressed as @Binds. */
@Module
@InstallIn(SingletonComponent::class)
object AppProvidersModule {

    /** Used by OfflineQueueManager to serialise queued operations. */
    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    /**
     * AudioUtils is an object, so it has no constructor for Hilt to call.
     * SpeakerModule passes it explicitly today; this keeps the graph complete if
     * anything ever asks for it by injection.
     */
    @Provides
    @Singleton
    fun provideAudioUtils(): AudioUtils = AudioUtils
}
