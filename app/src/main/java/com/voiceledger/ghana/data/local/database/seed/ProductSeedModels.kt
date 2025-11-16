package com.voiceledger.ghana.data.local.database.seed

import com.voiceledger.ghana.data.local.entity.ProductVocabulary
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * # ProductSeedAsset
 * 
 * **Clean Architecture - Data Layer**
 * 
 * This data class represents the top-level structure of the seed data JSON file located at
 * `assets/seed_data/products.json`. It serves as the container for initial product vocabulary
 * and measurement units that are loaded into the database when it's first created.
 * 
 * ## Kotlin Concepts Demonstrated:
 * 
 * - **@Serializable**: This annotation from kotlinx.serialization enables automatic JSON parsing.
 *   Without it, we'd need to manually parse JSON, which is error-prone and verbose.
 * 
 * - **data class**: A Kotlin feature that automatically generates equals(), hashCode(), toString(),
 *   and copy() methods. Perfect for representing immutable data structures like JSON models.
 * 
 * - **@SerialName**: Maps JSON field names to Kotlin property names. This is crucial for
 *   maintaining clean Kotlin naming conventions while working with external JSON formats.
 * 
 * - **Default parameters**: The `= emptyList()` provides fallback values if JSON fields are missing,
 *   making deserialization more robust and preventing NullPointerExceptions.
 * 
 * - **internal visibility**: Restricts this class to the current module (app), preventing external
 *   access while allowing all files within the module to use it. This enforces encapsulation.
 * 
 * ## Purpose in Clean Architecture:
 * 
 * This class lives in the Data Layer and acts as a Data Transfer Object (DTO) specifically for
 * parsing external JSON assets. It's separate from domain models (business logic) and entities
 * (database models), following the Single Responsibility Principle.
 * 
 * @property products List of fish products from the seed data JSON
 * @property measurementUnits List of measurement units (kg, lbs, pieces, etc.) from the seed data
 */
@Serializable
internal data class ProductSeedAsset(
    @SerialName("products")
    val products: List<ProductSeed> = emptyList(),
    @SerialName("measurementUnits")
    val measurementUnits: List<ProductSeed> = emptyList()
) {
    /**
     * Combines both products and measurement units into a single list.
     * 
     * This convenience method simplifies processing by treating both types uniformly,
     * since they share the same data structure and database entity type.
     * 
     * @return Combined list of all seed entries (products + measurement units)
     */
    fun seeds(): List<ProductSeed> = products + measurementUnits
}

/**
 * # ProductSeed
 * 
 * **Clean Architecture - Data Layer (DTO)**
 * 
 * Represents a single product or measurement unit entry from the seed data JSON file.
 * This class maps directly to the JSON structure and contains all the information needed
 * to initialize product vocabulary in the database.
 * 
 * ## Kotlin Concepts Demonstrated:
 * 
 * - **Nullable types**: Properties like `seasonality`, `twiNames`, and `gaNames` use the `?` suffix
 *   to indicate they can be null. This is Kotlin's type-safe approach to handling missing data,
 *   preventing NullPointerExceptions at compile time.
 * 
 * - **Named parameters**: When creating instances, you can specify parameters by name for clarity:
 *   `ProductSeed(id = "tilapia", canonicalName = "Tilapia", ...)`
 * 
 * - **Immutability**: All properties use `val` (read-only), making instances immutable and thread-safe.
 *   This prevents accidental modifications and makes the code more predictable.
 * 
 * ## Multi-Language Support:
 * 
 * The `twiNames` and `gaNames` properties support Ghana's linguistic diversity by storing product
 * names in local languages (Twi and Ga). This enables voice recognition to understand commands
 * in multiple languages, making the app accessible to more vendors.
 * 
 * @property id Unique identifier for the product (e.g., "tilapia", "kg")
 * @property canonicalName Standard English name for the product
 * @property category Product category (e.g., "FISH", "MEASUREMENT_UNIT")
 * @property variants Alternative names or spellings (e.g., ["tilapia", "tee-lah-pya"])
 * @property minPrice Minimum expected price in local currency (for validation)
 * @property maxPrice Maximum expected price in local currency (for validation)
 * @property measurementUnits Applicable measurement units (e.g., ["kg", "pieces"])
 * @property frequency Usage frequency counter (starts at 0, increments with each transaction)
 * @property isActive Whether this product is currently active in the system
 * @property seasonality Optional seasonal availability info (e.g., "November-March")
 * @property twiNames Product names in Twi language for multi-language recognition
 * @property gaNames Product names in Ga language for multi-language recognition
 * @property isLearned Whether this was user-taught (false for seed data)
 * @property learningConfidence Confidence score for learned products (1.0 = perfect confidence)
 */
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
    /**
     * Converts this seed data model into a Room database entity.
     * 
     * ## The Mapper Pattern:
     * 
     * This method follows the "mapper" pattern, which is crucial in Clean Architecture:
     * 1. **Separation of Concerns**: DTOs (this class) for JSON parsing, Entities for database storage
     * 2. **Data Transformation**: Converts JSON-friendly lists into database-friendly comma-separated strings
     * 3. **Normalization**: Cleans up data (trims whitespace, removes duplicates) before storage
     * 
     * ## Kotlin Concepts:
     * 
     * - **Default parameter**: `timestampProvider: () -> Long = System::currentTimeMillis` provides
     *   a default timestamp function but allows tests to inject a custom one. The `= System::currentTimeMillis`
     *   uses Kotlin's method reference syntax (::) to pass a function as a parameter.
     * 
     * - **Function type**: `() -> Long` means "a function that takes no parameters and returns a Long".
     *   This is Kotlin's way of treating functions as first-class citizens.
     * 
     * - **Safe call with takeIf**: `seasonality?.takeIf { it.isNotBlank() }` is a chain of null-safe
     *   operations. It returns null if seasonality is null OR if it's blank, otherwise returns the value.
     * 
     * @param timestampProvider Function that provides the current timestamp (injectable for testing)
     * @return ProductVocabulary entity ready to be inserted into the Room database
     */
    fun toEntity(timestampProvider: () -> Long = System::currentTimeMillis): ProductVocabulary {
        // Get the timestamp once to ensure createdAt and updatedAt are identical for seed data
        val timestamp = timestampProvider()
        
        return ProductVocabulary(
            id = id,
            canonicalName = canonicalName,
            category = category,
            // Convert list to comma-separated string after normalization
            variants = ProductVocabulary.listToString(variants.normalized()),
            minPrice = minPrice,
            maxPrice = maxPrice,
            measurementUnits = ProductVocabulary.listToString(measurementUnits.normalized()),
            frequency = frequency,
            isActive = isActive,
            // Only include seasonality if it's not null AND not blank
            seasonality = seasonality?.takeIf { it.isNotBlank() },
            // Convert multi-language names to comma-separated strings (or null if empty)
            twiNames = twiNames?.normalized()?.toCommaSeparatedOrNull(),
            gaNames = gaNames?.normalized()?.toCommaSeparatedOrNull(),
            createdAt = timestamp,
            updatedAt = timestamp,
            isLearned = isLearned,
            learningConfidence = learningConfidence
        )
    }
}

/**
 * Extension function to normalize a list of strings.
 * 
 * ## Kotlin Extension Functions:
 * 
 * This is an extension function - one of Kotlin's most powerful features. It "extends" the
 * List<String> class with a new method without modifying the original class. The syntax
 * `private fun List<String>.normalized()` means "add a method called normalized() to List<String>".
 * 
 * Inside the function, `this` refers to the List instance, but we can omit it for cleaner code.
 * 
 * ## Purpose:
 * 
 * Cleans up string lists from JSON by:
 * 1. Trimming whitespace from each string
 * 2. Removing empty strings
 * 3. Removing duplicates
 * 
 * ## Functional Programming:
 * 
 * This demonstrates Kotlin's functional programming capabilities with method chaining:
 * - `map { it.trim() }`: Transforms each element (it) by trimming whitespace
 * - `filter { it.isNotEmpty() }`: Keeps only non-empty strings
 * - `distinct()`: Removes duplicates
 * 
 * @receiver List<String> to be normalized
 * @return Cleaned list with trimmed, non-empty, unique strings
 */
private fun List<String>.normalized(): List<String> = map { it.trim() }
    .filter { it.isNotEmpty() }
    .distinct()

/**
 * Extension function to convert a list to a comma-separated string or null if empty.
 * 
 * ## Kotlin Null Safety:
 * 
 * This function demonstrates Kotlin's approach to handling empty data. Instead of returning
 * an empty string (which could cause issues in the database), it returns null, making it
 * explicit that there's no data. This works seamlessly with Room's nullable column types.
 * 
 * ## When to Return Null vs Empty String:
 * 
 * - null: Represents "no data" - appropriate for optional database columns
 * - Empty string: Represents "data exists but is empty" - different semantic meaning
 * 
 * @receiver List<String> to be converted
 * @return Comma-separated string or null if the list is empty after normalization
 */
private fun List<String>.toCommaSeparatedOrNull(): String? {
    val normalized = normalized()
    return if (normalized.isEmpty()) {
        null
    } else {
        ProductVocabulary.listToString(normalized)
    }
}
