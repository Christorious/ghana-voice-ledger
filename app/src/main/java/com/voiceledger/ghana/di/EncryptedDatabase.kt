package com.voiceledger.ghana.di

import javax.inject.Qualifier

/**
 * Qualifier annotation for encrypted database instances.
 * 
 * This annotation distinguishes between regular and encrypted database
 * providers in the dependency injection graph.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class EncryptedDatabase