package com.voiceledger.ghana.data.local.database.seed

import android.content.Context
import com.voiceledger.ghana.data.local.entity.ProductVocabulary
import kotlinx.serialization.json.Json

/**
 * # SeedDataLoader
 * 
 * **Clean Architecture - Data Layer**
 * 
 * Responsible for loading seed data from JSON files stored in the app's assets folder.
 * This class handles all the complexities of file I/O, JSON parsing, and error handling,
 * providing a clean, type-safe API for loading initial product vocabulary.
 * 
 * ## Why Load from Assets Instead of Hardcoded SQL?
 * 
 * This approach offers several advantages:
 * 1. **Maintainability**: JSON is easier to read and modify than SQL strings
 * 2. **Type Safety**: Kotlin data classes catch errors at compile time
 * 3. **Validation**: JSON parsing validates structure automatically
 * 4. **Flexibility**: Easy to add new fields without changing SQL statements
 * 5. **Testability**: Can easily mock or replace the JSON source in tests
 * 
 * ## Kotlin Concepts Demonstrated:
 * 
 * - **Primary constructor with default parameters**: The constructor parameters become properties
 *   automatically. The `json` parameter has a default value, so callers don't need to provide it.
 * 
 * - **DSL (Domain-Specific Language)**: The `Json { ... }` syntax is a Kotlin DSL for configuring
 *   the JSON parser. The lambda block configures parsing behavior.
 * 
 * - **internal visibility**: This class is only accessible within the app module, not to external
 *   consumers. This is important for encapsulation in Clean Architecture.
 * 
 * @property context Android context for accessing the assets folder
 * @property json Configured JSON parser (injectable for testing with custom configurations)
 */
internal class SeedDataLoader(
    private val context: Context,
    private val json: Json = Json {
        // Ignore JSON fields that aren't defined in our data classes
        // This makes the code forward-compatible with JSON schema changes
        ignoreUnknownKeys = true
        
        // Treat missing fields as null instead of throwing exceptions
        // This makes deserialization more robust
        explicitNulls = false
    }
) {

    /**
     * Loads product vocabulary seed data from the assets folder.
     * 
     * ## The Result<T> Pattern:
     * 
     * This method returns `Result<List<ProductVocabulary>>` instead of throwing exceptions.
     * Result is Kotlin's built-in type for representing success or failure:
     * 
     * - **Success**: Contains the loaded product list
     * - **Failure**: Contains the exception that occurred
     * 
     * This is better than throwing exceptions because:
     * 1. Callers are forced to handle errors (the compiler won't let them ignore it)
     * 2. It's explicit in the function signature that failure is possible
     * 3. It's easier to compose with other Result-returning functions
     * 
     * ## Kotlin I/O with Resource Management:
     * 
     * The `.use { }` blocks ensure resources (streams, readers) are automatically closed,
     * even if an exception occurs. This is Kotlin's equivalent to Java's try-with-resources.
     * 
     * ## Method Chaining:
     * 
     * This function demonstrates elegant method chaining:
     * 1. Open asset file → 2. Create buffered reader → 3. Read all text → 4. Parse JSON →
     * 5. Extract seeds → 6. Convert to entities
     * 
     * Each step is a single line, making the data flow easy to understand.
     * 
     * ## runCatching:
     * 
     * Wraps the entire operation in Result<T>. Any exception thrown inside becomes
     * Result.failure(exception), while successful completion becomes Result.success(value).
     * 
     * @return Result containing the list of ProductVocabulary entities or an error
     */
    fun loadProductSeeds(): Result<List<ProductVocabulary>> = runCatching {
        // Open the asset file (automatically closed by .use)
        context.assets.open(PRODUCTS_ASSET_PATH).use { stream ->
            // Create a buffered reader for efficient text reading (automatically closed by .use)
            val content = stream.bufferedReader().use { it.readText() }
            
            // Parse JSON into our data model using kotlinx.serialization
            val seedAsset = json.decodeFromString<ProductSeedAsset>(content)
            
            // Convert each seed data object to a Room entity
            seedAsset.seeds().map { it.toEntity() }
        }
    }

    companion object {
        /**
         * Path to the seed data JSON file within the assets folder.
         * 
         * Assets are files packaged with the app that can be accessed at runtime.
         * They're stored in `app/src/main/assets/` during development and become
         * part of the APK when the app is built.
         */
        private const val PRODUCTS_ASSET_PATH = "seed_data/products.json"
    }
}
