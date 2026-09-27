package app.menosan.android.data.local

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun instantToMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun millisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun dateToText(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun textToDate(value: String?): LocalDate? = value?.let(LocalDate::parse)
}
