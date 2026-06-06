package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.AlertType
import java.util.Date

/**
 * TypeConverters
 * ──────────────
 * Room TypeConverters translating custom objects (like Java Date)
 * into SQLite-compatible raw primitives (like Long/String).
 */
class TypeConverters {

    @TypeConverter
    fun fromAlertType(value: AlertType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toAlertType(value: String?): AlertType? {
        return value?.let {
            try {
                AlertType.valueOf(it)
            } catch (e: java.lang.IllegalArgumentException) {
                null
            }
        }
    }

    @TypeConverter
    fun fromSyncState(value: com.example.data.model.SyncState?): String? {
        return value?.name
    }

    @TypeConverter
    fun toSyncState(value: String?): com.example.data.model.SyncState? {
        return value?.let {
            try {
                com.example.data.model.SyncState.valueOf(it)
            } catch (e: java.lang.IllegalArgumentException) {
                null
            }
        }
    }

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        return value?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String? {
        return list?.joinToString(",")
    }
}
