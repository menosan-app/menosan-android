package app.menosan.android.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "taxonomy")
data class TaxonomyEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val version: Int,
    val json: String,
    @ColumnInfo(name = "fetched_at") val fetchedAt: Instant,
) {
    companion object {
        const val SINGLE_ROW_ID = 1
    }
}
