package com.burha.fundhelper.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FollowEntity::class, SnapshotEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun followDao(): FollowDao
    abstract fun snapshotDao(): SnapshotDao
}
