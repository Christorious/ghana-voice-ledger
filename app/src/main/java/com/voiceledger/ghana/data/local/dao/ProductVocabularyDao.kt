package com.voiceledger.ghana.data.local.dao

import androidx.room.*
import com.voiceledger.ghana.data.local.entity.ProductVocabulary
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for ProductVocabulary entity
 * Provides queries for product recognition and vocabulary management
 */
@Dao
interface ProductVocabularyDao {
    
    @Query("SELECT * FROM product_vocabulary WHERE isActive = 1 ORDER BY frequency DESC")
    fun getAllActiveProducts(): Flow<List<ProductVocabulary>>
    
    @Query("SELECT * FROM product_vocabulary WHERE category = :category AND isActive = 1 ORDER BY frequency DESC")
    fun getProductsByCategory(category: String): Flow<List<ProductVocabulary>>
    
    @Query("SELECT * FROM product_vocabulary WHERE canonicalName = :name AND isActive = 1")
    suspend fun getProductByName(name: String): ProductVocabulary?
    
    @Query("SELECT * FROM product_vocabulary WHERE id = :id")
    suspend fun getProductById(id: String): ProductVocabulary?
    
    @Query("SELECT * FROM product_vocabulary WHERE variants LIKE '%' || :variant || '%' AND isActive = 1")
    suspend fun getProductsByVariant(variant: String): List<ProductVocabulary>
    
    @Query("SELECT * FROM product_vocabulary WHERE isLearned = 1 AND isActive = 1 ORDER BY learningConfidence DESC")
    fun getLearnedProducts(): Flow<List<ProductVocabulary>>
    
    @Query("SELECT * FROM product_vocabulary WHERE frequency >= :minFrequency AND isActive = 1 ORDER BY frequency DESC")
    fun getPopularProducts(minFrequency: Int): Flow<List<ProductVocabulary>>
    
    @Query("SELECT DISTINCT category FROM product_vocabulary WHERE isActive = 1 ORDER BY category ASC")
    suspend fun getAllCategories(): List<String>
    
    @Query("SELECT * FROM product_vocabulary WHERE minPrice <= :price AND maxPrice >= :price AND isActive = 1")
    suspend fun getProductsByPriceRange(price: Double): List<ProductVocabulary>
    
    @Query("SELECT * FROM product_vocabulary WHERE seasonality IS NOT NULL AND isActive = 1")
    fun getSeasonalProducts(): Flow<List<ProductVocabulary>>
    
    @Query("SELECT * FROM product_vocabulary WHERE twiNames IS NOT NULL AND isActive = 1")
    fun getProductsWithTwiNames(): Flow<List<ProductVocabulary>>
    
    @Query("SELECT * FROM product_vocabulary WHERE gaNames IS NOT NULL AND isActive = 1")
    fun getProductsWithGaNames(): Flow<List<ProductVocabulary>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductVocabulary)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductVocabulary>)
    
    @Update
    suspend fun updateProduct(product: ProductVocabulary)
    
    @Delete
    suspend fun deleteProduct(product: ProductVocabulary)
    
    @Query("UPDATE product_vocabulary SET isActive = 0 WHERE id = :id")
    suspend fun deactivateProduct(id: String)
    
    @Query("UPDATE product_vocabulary SET isActive = 1 WHERE id = :id")
    suspend fun reactivateProduct(id: String)
    
    @Query("DELETE FROM product_vocabulary WHERE id = :id")
    suspend fun deleteProductById(id: String)
    
    @Query("UPDATE product_vocabulary SET frequency = frequency + 1 WHERE id = :id")
    suspend fun incrementFrequency(id: String)
    
    @Query("UPDATE product_vocabulary SET minPrice = :minPrice, maxPrice = :maxPrice WHERE id = :id")
    suspend fun updatePriceRange(id: String, minPrice: Double, maxPrice: Double)
    
    @Query("UPDATE product_vocabulary SET variants = :variants WHERE id = :id")
    suspend fun updateVariants(id: String, variants: String)
    
    @Query("UPDATE product_vocabulary SET learningConfidence = :confidence WHERE id = :id")
    suspend fun updateLearningConfidence(id: String, confidence: Float)
    
    // Search and matching queries
    @Query("""
        SELECT * FROM product_vocabulary 
        WHERE (canonicalName LIKE '%' || :searchTerm || '%' 
               OR variants LIKE '%' || :searchTerm || '%'
               OR twiNames LIKE '%' || :searchTerm || '%'
               OR gaNames LIKE '%' || :searchTerm || '%')
        AND isActive = 1
        ORDER BY frequency DESC
    """)
    suspend fun searchProducts(searchTerm: String): List<ProductVocabulary>
    
    @Query("""
        SELECT * FROM product_vocabulary 
        WHERE canonicalName LIKE :pattern 
        AND isActive = 1
        ORDER BY frequency DESC
        LIMIT :limit
    """)
    suspend fun findSimilarProducts(pattern: String, limit: Int = 5): List<ProductVocabulary>
    
    // Analytics queries
    @Query("SELECT COUNT(*) FROM product_vocabulary WHERE isActive = 1")
    suspend fun getActiveProductCount(): Int
    
    @Query("SELECT COUNT(*) FROM product_vocabulary WHERE isLearned = 1 AND isActive = 1")
    suspend fun getLearnedProductCount(): Int
    
    @Query("SELECT SUM(frequency) FROM product_vocabulary WHERE isActive = 1")
    suspend fun getTotalProductFrequency(): Int
    
    @Query("SELECT AVG(frequency) FROM product_vocabulary WHERE isActive = 1")
    suspend fun getAverageProductFrequency(): Double?
    
    @Query("SELECT * FROM product_vocabulary WHERE isActive = 1 ORDER BY frequency DESC LIMIT 1")
    suspend fun getMostPopularProduct(): ProductVocabulary?
    
    @Query("""
        SELECT category, COUNT(*) as count, SUM(frequency) as totalFrequency
        FROM product_vocabulary 
        WHERE isActive = 1 
        GROUP BY category 
        ORDER BY totalFrequency DESC
    """)
    suspend fun getCategoryStatistics(): List<CategoryStats>
    
    @Query("""
        SELECT category, AVG(minPrice) as avgMinPrice, AVG(maxPrice) as avgMaxPrice
        FROM product_vocabulary 
        WHERE isActive = 1 
        GROUP BY category
    """)
    suspend fun getCategoryPriceRanges(): List<CategoryPriceRange>
    
    // Maintenance queries
    @Query("DELETE FROM product_vocabulary WHERE isActive = 0 AND updatedAt < :cutoffTime")
    suspend fun cleanupInactiveProducts(cutoffTime: Long)
    
    @Query("UPDATE product_vocabulary SET frequency = 0 WHERE frequency < 0")
    suspend fun resetNegativeFrequencies()
    
    @Query("SELECT * FROM product_vocabulary WHERE updatedAt < :cutoffTime AND isLearned = 1 AND learningConfidence < :minConfidence")
    suspend fun getLowConfidenceLearnedProducts(cutoffTime: Long, minConfidence: Float): List<ProductVocabulary>
}

/**
 * Data class for category statistics results
 */
data class CategoryStats(
    val category: String,
    val count: Int,
    val totalFrequency: Int
)

/**
 * Data class for category price range results
 */
data class CategoryPriceRange(
    val category: String,
    val avgMinPrice: Double,
    val avgMaxPrice: Double
)