package com.burha.fundhelper.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FollowEntity::class, SnapshotEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun followDao(): FollowDao
    abstract fun snapshotDao(): SnapshotDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE snapshots ADD COLUMN payCount REAL")
        db.execSQL("ALTER TABLE snapshots ADD COLUMN prevPayCount REAL")
        db.execSQL("ALTER TABLE snapshots ADD COLUMN investorCount REAL")
        db.execSQL("ALTER TABLE snapshots ADD COLUMN prevInvestorCount REAL")
    }
}
