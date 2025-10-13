package com.voiceledger.ghana.data.repository

import com.voiceledger.ghana.data.local.dao.ProductVocabularyDao
import com.voiceledger.ghana.data.local.dao.CategoryStats
import com.voiceledger.ghana.data.local.dao.CategoryPriceRange
import com.voiceledger.ghana.data.local.entity.ProductVocabulary
import com.voiceledger.ghana.domain.repository.ProductVocabularyRepository
import kotlinx.coroutines.flow.Flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ProductVocabularyRepository
 * Handles product recognition and vocabulary management with Ghana-specific features
 */
@Singleton
class ProductVocabularyRepositoryImpl @Inject constructor(
    private val productVocabularyDao: ProductVocabularyDao
) : ProductVocabularyRepository {
    
    override fun getAllActiveProducts(): Flow<List<ProductVocabulary>> {
        return productVocabularyDao.getAllActiveProducts()
    }
    
    override fun getProductsByCategory(category: String): Flow<List<ProductVocabulary>> {
        return productVocabularyDao.getProductsByCategory(category)
    }
    
    override suspend fun getProductByName(name: String): ProductVocabulary? {
        return productVocabularyDao.getProductByName(name)
    }
    
    override suspend fun getProductById(id: String): ProductVocabulary? {
        return productVocabularyDao.getProductById(id)
    }
    
    override suspend fun getProductsByVariant(variant: String): List<ProductVocabulary> {
        return productVocabularyDao.getProductsByVariant(variant.lowercase())
    }
    
    override fun getLearnedProducts(): Flow<List<ProductVocabulary>> {
        return productVocabularyDao.getLearnedProducts()
    }
    
    override fun getPopularProducts(minFrequency: Int): Flow<List<ProductVocabulary>> {
        return productVocabularyDao.getPopularProducts(minFrequency)
    }
    
    override fun getSeasonalProducts(): Flow<List<ProductVocabulary>> {
        return productVocabularyDao.getSeasonalProducts()
    }
    
    override fun getProductsWithTwiNames(): Flow<List<ProductVocabulary>> {
        return productVocabularyDao.getProductsWithTwiNames()
    }
    
    override fun getProductsWithGaNames(): Flow<List<ProductVocabulary>> {
        return productVocabularyDao.getProductsWithGaNames()
    }
    
    override suspend fun getAllCategories(): List<String> {
        return productVocabularyDao.getAllCategories()
    }
    
    override suspend fun getProductsByPriceRange(price: Double): List<ProductVocabulary> {
        return productVocabularyDao.getProductsByPriceRange(price)
    }
    
    override suspend fun searchProducts(searchTerm: String): List<ProductVocabulary> {
        return productVocabularyDao.searchProducts(searchTerm.lowercase())
    }
    
    override suspend fun findSimilarProducts(pattern: String, limit: Int): List<ProductVocabulary> {
        return productVocabularyDao.findSimilarProducts("%${pattern.lowercase()}%", limit)
    }
    
    override suspend fun findBestMatch(productName: String): ProductVocabulary? {
        val cleanName = productName.lowercase().trim()
        
        // First try exact match
        val exactMatch = getProductByName(cleanName)
        if (exactMatch != null) return exactMatch
        
        // Try variant matching
        val variantMatch = matchWithVariants(cleanName)
        if (variantMatch != null) return variantMatch
        
        // Try fuzzy matching
        val fuzzyMatches = fuzzyMatch(cleanName, 2)
        return fuzzyMatches.firstOrNull()
    }
    
    override suspend fun fuzzyMatch(productName: String, maxDistance: Int): List<ProductVocabulary> {
        val allProducts = productVocabularyDao.getAllActiveProducts()
        val matches = mutableListOf<Pair<ProductVocabulary, Int>>()
        
        allProducts.collect { products ->
            products.forEach { product ->
                // Check canonical name
                val canonicalDistance = levenshteinDistance(productName.lowercase(), product.canonicalName.lowercase())
                if (canonicalDistance <= maxDistance) {
                    matches.add(product to canonicalDistance)
                }
                
                // Check variants
                product.getVariantsList().forEach { variant ->
                    val variantDistance = levenshteinDistance(productName.lowercase(), variant.lowercase())
                    if (variantDistance <= maxDistance) {
                        matches.add(product to variantDistance)
                    }
                }
                
                // Check Twi names
                product.twiNames?.split(",")?.forEach { twiName ->
                    val twiDistance = levenshteinDistance(productName.lowercase(), twiName.trim().lowercase())
                    if (twiDistance <= maxDistance) {
                        matches.add(product to twiDistance)
                    }
                }
                
                // Check Ga names
                product.gaNames?.split(",")?.forEach { gaName ->
                    val gaDistance = levenshteinDistance(productName.lowercase(), gaName.trim().lowercase())
                    if (gaDistance <= maxDistance) {
                        matches.add(product to gaDistance)
                    }
                }
            }
        }
        
        return matches
            .distinctBy { it.first.id }
            .sortedBy { it.second }
            .map { it.first }
    }
    
    override suspend fun matchWithVariants(productName: String): ProductVocabulary? {
        val cleanName = productName.lowercase().trim()
        val products = getProductsByVariant(cleanName)
        return products.firstOrNull()
    }
    
    override suspend fun getActiveProductCount(): Int {
        return productVocabularyDao.getActiveProductCount()
    }
    
    override suspend fun getLearnedProductCount(): Int {
        return productVocabularyDao.getLearnedProductCount()
    }
    
    override suspend fun getTotalProductFrequency(): Int {
        return productVocabularyDao.getTotalProductFrequency()
    }
    
    override suspend fun getAverageProductFrequency(): Double {
        return productVocabularyDao.getAverageProductFrequency() ?: 0.0
    }
    
    override suspend fun getMostPopularProduct(): ProductVocabulary? {
        return productVocabularyDao.getMostPopularProduct()
    }
    
    override suspend fun getCategoryStatistics(): List<CategoryStats> {
        return productVocabularyDao.getCategoryStatistics()
    }
    
    override suspend fun getCategoryPriceRanges(): List<CategoryPriceRange> {
        return productVocabularyDao.getCategoryPriceRanges()
    }
    
    override suspend fun insertProduct(product: ProductVocabulary) {
        productVocabularyDao.insertProduct(product)
    }
    
    override suspend fun insertProducts(products: List<ProductVocabulary>) {
        productVocabularyDao.insertProducts(products)
    }
    
    override suspend fun updateProduct(product: ProductVocabulary) {
        val updatedProduct = product.copy(updatedAt = System.currentTimeMillis())
        productVocabularyDao.updateProduct(updatedProduct)
    }
    
    override suspend fun deleteProduct(product: ProductVocabulary) {
        productVocabularyDao.deleteProduct(product)
    }
    
    override suspend fun deleteProductById(id: String) {
        productVocabularyDao.deleteProductById(id)
    }
    
    override suspend fun deactivateProduct(id: String) {
        productVocabularyDao.deactivateProduct(id)
    }
    
    override suspend fun reactivateProduct(id: String) {
        productVocabularyDao.reactivateProduct(id)
    }
    
    override suspend fun learnNewProduct(productName: String, category: String, price: Double): ProductVocabulary {
        val productId = "learned_${UUID.randomUUID()}"
        val currentTime = System.currentTimeMillis()
        
        val newProduct = ProductVocabulary(
            id = productId,
            canonicalName = productName.lowercase().replaceFirstChar { it.uppercase() },
            category = category,
            variants = productName.lowercase(),
            minPrice = price * 0.8, // 20% below observed price
            maxPrice = price * 1.2, // 20% above observed price
            measurementUnits = "piece", // Default unit
            frequency = 1,
            isActive = true,
            seasonality = null,
            twiNames = null,
            gaNames = null,
            createdAt = currentTime,
            updatedAt = currentTime,
            isLearned = true,
            learningConfidence = 0.7f // Initial confidence for learned products
        )
        
        insertProduct(newProduct)
        return newProduct
    }
    
    override suspend fun addVariant(productId: String, variant: String) {
        val product = getProductById(productId)
        product?.let {
            val currentVariants = it.getVariantsList().toMutableList()
            if (!currentVariants.contains(variant.lowercase())) {
                currentVariants.add(variant.lowercase())
                val updatedVariants = ProductVocabulary.listToString(currentVariants)
                productVocabularyDao.updateVariants(productId, updatedVariants)
            }
        }
    }
    
    override suspend fun updatePriceRange(productId: String, minPrice: Double, maxPrice: Double) {
        productVocabularyDao.updatePriceRange(productId, minPrice, maxPrice)
    }
    
    override suspend fun incrementFrequency(productId: String) {
        productVocabularyDao.incrementFrequency(productId)
    }
    
    override suspend fun updateLearningConfidence(productId: String, confidence: Float) {
        productVocabularyDao.updateLearningConfidence(productId, confidence)
    }
    
    override suspend fun correctProductName(wrongName: String, correctName: String) {
        val correctProduct = findBestMatch(correctName)
        correctProduct?.let {
            // Add the wrong name as a variant to help future recognition
            addVariant(it.id, wrongName)
            // Increase confidence since user provided correction
            val newConfidence = (it.learningConfidence + 0.1f).coerceAtMost(1.0f)
            updateLearningConfidence(it.id, newConfidence)
        }
    }
    
    override suspend fun validatePrice(productName: String, price: Double): Boolean {
        val product = findBestMatch(productName)
        return product?.isPriceInRange(price) ?: true // Allow if product not found
    }
    
    override suspend fun suggestCorrection(productName: String): String? {
        val fuzzyMatches = fuzzyMatch(productName, 3)
        return fuzzyMatches.firstOrNull()?.canonicalName
    }
    
    override suspend fun isValidProduct(productName: String): Boolean {
        return findBestMatch(productName) != null
    }
    
    override suspend fun cleanupInactiveProducts(cutoffTime: Long) {
        productVocabularyDao.cleanupInactiveProducts(cutoffTime)
    }
    
    override suspend fun resetNegativeFrequencies() {
        productVocabularyDao.resetNegativeFrequencies()
    }
    
    override suspend fun getLowConfidenceLearnedProducts(cutoffTime: Long, minConfidence: Float): List<ProductVocabulary> {
        return productVocabularyDao.getLowConfidenceLearnedProducts(cutoffTime, minConfidence)
    }
    
    /**
     * Calculate Levenshtein distance between two strings
     * Used for fuzzy matching of product names
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }
        
        for (i in 0..len1) {
            dp[i][0] = i
        }
        
        for (j in 0..len2) {
            dp[0][j] = j
        }
        
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return dp[len1][len2]
    }
}