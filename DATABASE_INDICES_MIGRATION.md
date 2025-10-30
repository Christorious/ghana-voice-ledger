# Database Indices Migration (Version 1 → 2)

## Overview
This migration adds performance indices for high-frequency query columns across all database entities. The indices were chosen based on analysis of DAO query patterns and will significantly improve query performance for common operations.

## Summary of Changes

### Database Version
- **Previous Version**: 1
- **New Version**: 2

### Migration Path
- Migration implementation: `DatabaseMigrations.MIGRATION_1_2`
- All existing data is preserved during migration
- Indices are created with `IF NOT EXISTS` clauses for safety

## Indices Added by Entity

### 1. Transaction Entity
Added indices for high-frequency filter and sort operations:

| Index Name | Columns | Purpose |
|------------|---------|---------|
| `index_transactions_needsReview` | `needsReview` | Filter transactions needing manual review |
| `index_transactions_synced` | `synced` | Filter unsynced transactions for offline queue |
| `index_transactions_date_customerId` | `date, customerId` | Customer transaction history by date |
| `index_transactions_date_needsReview` | `date, needsReview` | Daily review queries |
| `index_transactions_synced_timestamp` | `synced, timestamp` | Unsynced transactions ordered by time |
| `index_transactions_customerId_timestamp` | `customerId, timestamp` | Customer history chronologically ordered |

**Existing indices** (preserved from v1):
- `timestamp`, `product`, `customerId`, `date`

### 2. DailySummary Entity
Added indices for sync operations and temporal queries:

| Index Name | Columns | Purpose |
|------------|---------|---------|
| `index_daily_summaries_synced` | `synced` | Filter unsynced daily summaries |
| `index_daily_summaries_timestamp` | `timestamp` | Temporal ordering of summaries |
| `index_daily_summaries_date_synced` | `date, synced` | Date range sync status queries |

**Primary key**: `date` (no separate index needed)

### 3. AudioMetadata Entity
Added indices for speech detection and analytics:

| Index Name | Columns | Purpose |
|------------|---------|---------|
| `index_audio_metadata_speechDetected` | `speechDetected` | Filter audio with detected speech |
| `index_audio_metadata_contributedToTransaction` | `contributedToTransaction` | Find audio contributing to transactions |
| `index_audio_metadata_transactionId` | `transactionId` | Lookup audio by transaction |
| `index_audio_metadata_powerSavingMode` | `powerSavingMode` | Power optimization analytics |

**Existing indices** (preserved from v1):
- `timestamp`, `speakerDetected`, `vadScore`

### 4. SpeakerProfile Entity
Added indices for customer management and filtering:

| Index Name | Columns | Purpose |
|------------|---------|---------|
| `index_speaker_profiles_isActive` | `isActive` | Filter active profiles (in most queries) |
| `index_speaker_profiles_synced` | `synced` | Find unsynced profiles |
| `index_speaker_profiles_createdAt` | `createdAt` | Sort by profile creation time |
| `index_speaker_profiles_isSeller_isActive` | `isSeller, isActive` | Seller/customer filtering (composite) |
| `index_speaker_profiles_lastVisit_isActive` | `lastVisit, isActive` | Recent customer queries |

**Existing indices** (preserved from v1):
- `isSeller`, `lastVisit`, `visitCount`

### 5. ProductVocabulary Entity
Added indices for popularity and learning analytics:

| Index Name | Columns | Purpose |
|------------|---------|---------|
| `index_product_vocabulary_frequency` | `frequency` | Sort by usage frequency |
| `index_product_vocabulary_isLearned` | `isLearned` | Filter learned products |
| `index_product_vocabulary_updatedAt` | `updatedAt` | Temporal queries on product updates |

**Existing indices** (preserved from v1):
- `canonicalName`, `category`, `isActive`

## Query Performance Impact

### Before (Version 1)
- Boolean filter queries (e.g., `needsReview = 1`) required full table scans
- Composite queries (e.g., date + customerId) used only single-column indices
- Sync status queries were unoptimized

### After (Version 2)
- Boolean filters use dedicated indices for O(log n) lookup
- Composite indices eliminate redundant filtering steps
- Sync operations are optimized for offline-first architecture

### Expected Improvements
- **Review queries**: ~10-100x faster depending on review percentage
- **Sync operations**: ~50-200x faster for finding unsynced items
- **Customer history**: ~5-20x faster with composite date+customer index
- **Analytics queries**: ~10-50x faster with proper index coverage

## Migration Testing

### Test Coverage
The `DatabaseMigrationTest` class provides comprehensive testing:

1. **Data Preservation Test** (`migrate1To2_preservesData`)
   - Inserts sample data in v1 schema
   - Migrates to v2
   - Verifies all data integrity preserved

2. **Index Creation Test** (`migrate1To2_createsIndices`)
   - Verifies all new indices are created
   - Checks `sqlite_master` table for index entries

3. **Query Performance Test** (`migrate1To2_indicesImproveQueryPerformance`)
   - Uses `EXPLAIN QUERY PLAN` to verify index usage
   - Tests common query patterns
   - Ensures SQLite optimizer uses the new indices

4. **Empty Database Test** (`migrate1To2_withEmptyDatabase_succeeds`)
   - Tests migration on empty database
   - Verifies schema changes work without data

### Running Tests
```bash
./gradlew :app:connectedDebugAndroidTest \
  --tests "com.voiceledger.ghana.data.local.database.DatabaseMigrationTest"
```

## Room Schema Export

Room schema JSONs are automatically generated during build:
- Location: `app/schemas/`
- Files: `com.voiceledger.ghana.data.local.database.VoiceLedgerDatabase/1.json` (original)
- Files: `com.voiceledger.ghana.data.local.database.VoiceLedgerDatabase/2.json` (new)

These are used by Room's migration testing framework to validate schema changes.

## Integration Notes

### Backward Compatibility
- Migration is automatic and non-breaking
- No data loss occurs
- Indices are optional optimizations (queries work without them)

### Production Rollout
1. Release app with migration code
2. On app upgrade, migration runs automatically
3. Monitor App Center for any migration failures
4. Performance improvements are immediate after migration

### Monitoring
- Track migration success rate via App Center
- Monitor query performance metrics
- Compare p50/p95/p99 latencies before and after

## Developer Notes

### Adding Future Indices
When adding new indices in future migrations:

1. Analyze DAO queries to identify frequently filtered/sorted columns
2. Add `@Index` annotations to entity classes
3. Increment database version in `VoiceLedgerDatabase`
4. Add `CREATE INDEX` statements to new migration
5. Write migration tests verifying data preservation and index creation
6. Update this documentation

### Index Best Practices
- ✅ Index columns used in `WHERE` clauses
- ✅ Index columns used in `ORDER BY` clauses
- ✅ Create composite indices for common multi-column filters
- ✅ Consider cardinality (high cardinality = better index)
- ❌ Don't over-index (indices have write overhead)
- ❌ Don't index columns rarely used in queries
- ❌ Don't create redundant indices (e.g., if composite exists)

## References

- [Room Documentation - Indices](https://developer.android.com/training/data-storage/room/defining-data)
- [SQLite Index Documentation](https://www.sqlite.org/lang_createindex.html)
- [Database Migrations - Room](https://developer.android.com/training/data-storage/room/migrating-db-versions)
