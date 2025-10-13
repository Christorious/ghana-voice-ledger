package com.voiceledger.ghana.di

import com.voiceledger.ghana.domain.repository.ProductVocabularyRepository
import com.voiceledger.ghana.ml.entity.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Hilt module for entity extraction dependencies
 * Provides all entity-related components for dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
object EntityModule {
    
    /**
     * Provides Ghana-specific entity extractor
     */
    @Provides
    @Singleton
    fun provideGhanaEntityExtractor(
        productVocabularyRepository: ProductVocabularyRepository
    ): GhanaEntityExtractor {
        return GhanaEntityExtractor(productVocabularyRepository)
    }
    
    /**
     * Provides entity extractor interface
     */
    @Provides
    @Singleton
    fun provideEntityExtractor(
        ghanaEntityExtractor: GhanaEntityExtractor
    ): EntityExtractor {
        return ghanaEntityExtractor
    }
    
    /**
     * Provides entity normalizer
     */
    @Provides
    @Singleton
    fun provideEntityNormalizer(): EntityNormalizer {
        return EntityNormalizer()
    }
    
    /**
     * Provides entity extraction service
     */
    @Provides
    @Singleton
    fun provideEntityExtractionService(
        entityExtractor: EntityExtractor,
        entityNormalizer: EntityNormalizer,
        productVocabularyRepository: ProductVocabularyRepository
    ): EntityExtractionService {
        return EntityExtractionService(
            entityExtractor = entityExtractor,
            entityNormalizer = entityNormalizer,
            productVocabularyRepository = productVocabularyRepository
        )
    }
    
    /**
     * Provides primary entity extractor for general use
     */
    @Provides
    @Singleton
    @PrimaryEntityExtractor
    fun providePrimaryEntityExtractor(
        entityExtractionService: EntityExtractionService
    ): EntityExtractor {
        return object : EntityExtractor {
            override suspend fun extractAmount(text: String): AmountResult? {
                val result = entityExtractionService.processTranscript(text)
                return result.entities.amount?.let { normalized ->
                    AmountResult(
                        amount = normalized.amount,
                        currency = normalized.currency,
                        confidence = normalized.confidence,
                        originalText = text,
                        normalizedText = text.lowercase(),
                        startIndex = 0,
                        endIndex = text.length
                    )
                }
            }
            
            override suspend fun extractProduct(text: String): ProductResult? {
                val result = entityExtractionService.processTranscript(text)
                return result.entities.product?.let { normalized ->
                    ProductResult(
                        productName = normalized.productName,
                        canonicalName = normalized.productName,
                        category = normalized.category,
                        confidence = normalized.confidence,
                        originalText = text,
                        startIndex = 0,
                        endIndex = text.length,
                        variants = normalized.variants
                    )
                }
            }
            
            override suspend fun extractQuantity(text: String): QuantityResult? {
                val result = entityExtractionService.processTranscript(text)
                return result.entities.quantity?.let { normalized ->
                    QuantityResult(
                        quantity = normalized.quantity,
                        unit = normalized.unit,
                        confidence = normalized.confidence,
                        originalText = text,
                        startIndex = 0,
                        endIndex = text.length
                    )
                }
            }
            
            override suspend fun extractAllEntities(text: String): EntityExtractionResult {
                val result = entityExtractionService.processTranscript(text)
                return result.entities.originalResult
            }
            
            override suspend fun validateEntities(entities: EntityExtractionResult): ValidationResult {
                return entityExtractionService.processTranscript(entities.extractedText).validation
            }
            
            override suspend fun getExtractionConfidence(text: String): Float {
                val result = entityExtractionService.processTranscript(text)
                return result.entities.overallConfidence
            }
        }
    }
    
    /**
     * Provides Ghana-specific entity extractor for specialized use
     */
    @Provides
    @Singleton
    @GhanaSpecificExtractor
    fun provideGhanaSpecificExtractor(
        ghanaEntityExtractor: GhanaEntityExtractor
    ): EntityExtractor {
        return ghanaEntityExtractor
    }
}

/**
 * Qualifier for primary entity extractor (service-based)
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PrimaryEntityExtractor

/**
 * Qualifier for Ghana-specific entity extractor
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GhanaSpecificExtractor