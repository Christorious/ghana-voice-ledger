package com.voiceledger.ghana.data.local.database.seed

import com.voiceledger.ghana.data.local.entity.ProductVocabulary
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ProductSeedAsset(
    @SerialName("products")
    val products: List<ProductSeed> = emptyList(),
    @SerialName("measurementUnits")
    val measurementUnits: List<ProductSeed> = emptyList()
) {
    fun seeds(): List<ProductSeed> = products + measurementUnits
}

@Serializable
internal data class ProductSeed(
    @SerialName("id")
    val id: String,
    @SerialName("canonicalName")
    val canonicalName: String,
    @SerialName("category")
    val category: String,
    @SerialName("variants")
    val variants: List<String> = emptyList(),
    @SerialName("minPrice")
    val minPrice: Double,
    @SerialName("maxPrice")
    val maxPrice: Double,
    @SerialName("measurementUnits")
    val measurementUnits: List<String> = emptyList(),
    @SerialName("frequency")
    val frequency: Int = 0,
    @SerialName("isActive")
    val isActive: Boolean = true,
    @SerialName("seasonality")
    val seasonality: String? = null,
    @SerialName("twiNames")
    val twiNames: List<String>? = null,
    @SerialName("gaNames")
    val gaNames: List<String>? = null,
    @SerialName("isLearned")
    val isLearned: Boolean = false,
    @SerialName("learningConfidence")
    val learningConfidence: Float = 1.0f
) {
    fun toEntity(timestampProvider: () -> Long = System::currentTimeMillis): ProductVocabulary {
        val timestamp = timestampProvider()
        return ProductVocabulary(
            id = id,
            canonicalName = canonicalName,
            category = category,
            variants = ProductVocabulary.listToString(variants.normalized()),
            minPrice = minPrice,
            maxPrice = maxPrice,
            measurementUnits = ProductVocabulary.listToString(measurementUnits.normalized()),
            frequency = frequency,
            isActive = isActive,
            seasonality = seasonality?.takeIf { it.isNotBlank() },
            twiNames = twiNames?.normalized()?.toCommaSeparatedOrNull(),
            gaNames = gaNames?.normalized()?.toCommaSeparatedOrNull(),
            createdAt = timestamp,
            updatedAt = timestamp,
            isLearned = isLearned,
            learningConfidence = learningConfidence
        )
    }
}

private fun List<String>.normalized(): List<String> = map { it.trim() }
    .filter { it.isNotEmpty() }
    .distinct()

private fun List<String>.toCommaSeparatedOrNull(): String? {
    val normalized = normalized()
    return if (normalized.isEmpty()) {
        null
    } else {
        ProductVocabulary.listToString(normalized)
    }
}
