# Product Vocabulary Seed Data

This directory contains structured JSON files used to pre-populate the application database with initial product vocabulary data.

## Files

### products.json
Contains initial product vocabulary entries for:
- **Fish products**: Common fish varieties sold in Ghana markets (Tilapia, Mackerel, Sardines, Tuna, Red Fish, Salmon, Catfish, Croaker)
- **Measurement units**: Standard units used for transactions (Bowl, Bucket, Piece, Tin, Size)

## Structure

Each product entry contains:
- `id`: Unique identifier
- `canonicalName`: Standard product name
- `category`: Product category (e.g., "fish", "measurement")
- `variants`: List of alternative names/spellings
- `minPrice`: Minimum typical price in Ghana cedis
- `maxPrice`: Maximum typical price in Ghana cedis
- `measurementUnits`: List of applicable measurement units
- `frequency`: Usage frequency counter (starts at 0)
- `isActive`: Whether the product is currently active
- `seasonality`: Optional seasonal availability
- `twiNames`: Optional list of Twi language names
- `gaNames`: Optional list of Ga language names
- `isLearned`: Whether this entry was learned from user input
- `learningConfidence`: Confidence score for learned entries

## Modifying Seed Data

To add or modify seed products:

1. Edit `products.json` following the existing structure
2. Ensure the JSON is valid (use a JSON validator)
3. Test the changes with unit and integration tests
4. Note: Changes only affect new database installations

## Implementation

The seed data is loaded by:
- `SeedDataLoader`: Reads and parses the JSON file
- `ProductVocabularySeeder`: Handles database insertion with transaction safety
- `DatabaseCallback`: Triggers seeding on database creation

Error handling:
- I/O errors during file reading are logged and handled gracefully
- JSON parsing errors are caught and reported
- In DEBUG mode, errors cause the application to fail fast
- In RELEASE mode, errors are logged but don't crash the app
