package com.voiceledger.ghana.di

import android.content.Context
import com.voiceledger.ghana.security.EncryptionService
import com.voiceledger.ghana.security.PrivacyManager
import com.voiceledger.ghana.security.SecureDataStorage
import com.voiceledger.ghana.security.SecurityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for security components
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {
    
    @Provides
    @Singleton
    fun provideEncryptionService(
        @ApplicationContext context: Context
    ): EncryptionService {
        return EncryptionService(context)
    }
    
    @Provides
    @Singleton
    fun providePrivacyManager(
        @ApplicationContext context: Context,
        encryptionService: EncryptionService
    ): PrivacyManager {
        return PrivacyManager(context, encryptionService)
    }
    
    @Provides
    @Singleton
    fun provideSecureDataStorage(
        @ApplicationContext context: Context,
        encryptionService: EncryptionService,
        privacyManager: PrivacyManager
    ): SecureDataStorage {
        return SecureDataStorage(context, encryptionService, privacyManager)
    }
    
    @Provides
    @Singleton
    fun provideSecurityManager(
        @ApplicationContext context: Context,
        encryptionService: EncryptionService,
        privacyManager: PrivacyManager,
        secureDataStorage: SecureDataStorage
    ): SecurityManager {
        return SecurityManager(context, encryptionService, privacyManager, secureDataStorage)
    }
}