package com.voiceledger.ghana.domain.repository

import com.voiceledger.ghana.data.local.entity.ProductVocabulary
import com.voiceledger.ghana.data.local.dao.CategoryStats
import com.voiceledger.ghana.data.local.dao.CategoryPriceRange
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for product vocabulary operations
 * Defines the contract for product recognition and vocabulary management
 */
interface ProductVocabularyRepository {
    
    // Query operations
    fun getAllActiveProducts(): Flow<List<ProductVocabulary>>
    fun getProductsByCategory(category: String): Flow<List<ProductVocabulary>>
    suspend fun getProductByName(name: String): ProductVocabulary?
    suspend fun getProductById(id: String): ProductVocabulary?
    suspend fun getProductsByVariant(variant: String): List<ProductVocabulary>
    fun getLearnedProducts(): Flow<List<ProductVocabulary>>
    fun getPopularProducts(minFrequency: Int): Flow<List<ProductVocabulary>>
    fun getSeasonalProducts(): Flow<List<ProductVocabulary>>
    fun getProductsWithTwiNames(): Flow<List<ProductVocabulary>>
    fun getProductsWithGaNames(): Flow<List<ProductVocabulary>>
    suspend fun getAllCategories(): List<String>
    suspend fun getProductsByPriceRange(price: Double): List<ProductVocabulary>
    
    // Search and matching operations
    suspend fun searchProducts(searchTerm: String): List<ProductVocabulary>
    suspend fun findSimilarProducts(pattern: String, limit: Int = 5): List<ProductVocabulary>
    suspend fun findBestMatch(productName: String): ProductVocabulary?
    suspend fun fuzzyMatch(productName: String, maxDistance: Int = 3): List<ProductVocabulary>
    suspend fun matchWithVariants(productName: String): ProductVocabulary?
    
    // Analytics operations
    suspend fun getActiveProductCount(): Int
    suspend fun getLearnedProductCount(): Int
    suspend fun getTotalProductFrequency(): Int
    suspend fun getAverageProductFrequency(): Double
    suspend fun getMostPopularProduct(): ProductVocabulary?
    suspend fun getCategoryStatistics(): List<CategoryStats>
    suspend fun getCategoryPriceRanges(): List<CategoryPriceRange>
    
    // CRUD operations
    suspend fun insertProduct(product: ProductVocabulary)
    suspend fun insertProducts(products: List<ProductVocabulary>)
    suspend fun updateProduct(product: ProductVocabulary)
    suspend fun deleteProduct(product: ProductVocabulary)
    suspend fun deleteProductById(id: String)
    suspend fun deactivateProduct(id: String)
    suspend fun reactivateProduct(id: String)
    
    // Learning operations
    suspend fun learnNewProduct(productName: String, category: String, price: Double): ProductVocabulary
    suspend fun addVariant(productId: String, variant: String)
    suspend fun updatePriceRange(productId: String, minPrice: Double, maxPrice: Double)
    suspend fun incrementFrequency(productId: String)
    suspend fun updateLearningConfidence(productId: String, confidence: Float)
    suspend fun correctProductName(wrongName: String, correctName: String)
    
    // Validation operations
    suspend fun validatePrice(productName: String, price: Double): Boolean
    suspend fun suggestCorrection(productName: String): String?
    suspend fun isValidProduct(productName: String): Boolean
    
    // Maintenance operations
    suspend fun cleanupInactiveProducts(cutoffTime: Long)
    suspend fun resetNegativeFrequencies()
    suspend fun getLowConfidenceLearnedProducts(cutoffTime: Long, minConfidence: Float): List<ProductVocabulary>
}