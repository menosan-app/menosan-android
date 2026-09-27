package app.menosan.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [EntryEntity::class, ReportCacheEntity::class, TaxonomyEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MenosanDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun reportCacheDao(): ReportCacheDao
    abstract fun taxonomyDao(): TaxonomyDao

    companion object {
        const val NAME = "menosan.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reports_cache ADD COLUMN analyzed_grams INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE reports_cache ADD COLUMN analyzed_entries INTEGER NOT NULL DEFAULT 0")
                db.execSQL("DELETE FROM reports_cache")
                db.execSQL("DELETE FROM taxonomy")
            }
        }

        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
    }
}
