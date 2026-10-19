package com.voiceledger.ghana.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

/** CPU-bound work: VAD scoring, feature extraction, embedding maths. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/** Blocking I/O: the database, audio files, the network. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** The UI thread. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

/**
 * The coroutine dispatchers, as injectable bindings.
 *
 * Several @Provides methods used to declare a CoroutineDispatcher parameter with a
 * Kotlin default value (`= Dispatchers.Default`). Dagger does not read Kotlin
 * defaults: it saw a required CoroutineDispatcher with no binding anywhere in the
 * graph, which is what failed hiltJavaCompileDevDebug. Injecting them properly also
 * means a test can substitute a TestDispatcher instead of waiting on real threads.
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}
