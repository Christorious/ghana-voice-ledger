package com.voiceledger.ghana.data.local.database.seed

import android.content.Context
import com.voiceledger.ghana.data.local.entity.ProductVocabulary
import kotlinx.serialization.json.Json

internal class SeedDataLoader(
    private val context: Context,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
) {

    fun loadProductSeeds(): Result<List<ProductVocabulary>> = runCatching {
        context.assets.open(PRODUCTS_ASSET_PATH).use { stream ->
            val content = stream.bufferedReader().use { it.readText() }
            val seedAsset = json.decodeFromString<ProductSeedAsset>(content)
            seedAsset.seeds().map { it.toEntity() }
        }
    }

    companion object {
        private const val PRODUCTS_ASSET_PATH = "seed_data/products.json"
    }
}
