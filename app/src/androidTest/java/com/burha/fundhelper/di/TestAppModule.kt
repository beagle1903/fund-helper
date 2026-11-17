package com.burha.fundhelper.di

import android.content.Context
import androidx.room.Room
import com.burha.fundhelper.data.FollowBackup
import com.burha.fundhelper.data.local.AppDatabase
import com.burha.fundhelper.data.local.FollowDao
import com.burha.fundhelper.data.local.SnapshotDao
import com.burha.fundhelper.data.tefas.TefasClient
import com.burha.fundhelper.domain.Clock
import com.burha.fundhelper.domain.SystemClock
import com.burha.fundhelper.fakes.InstrumentedFakeFollowBackup
import com.burha.fundhelper.fakes.InstrumentedFakeTefasClient
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Replaces the production wiring for every instrumented test:
 * in-memory Room (the installed app's database is untouched), fake TEFAS, fake Downloads backup.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AppModule::class, TefasModule::class],
)
object TestAppModule {
    @Provides
    @Singleton
    fun clock(): Clock = SystemClock()

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

    @Provides
    fun followDao(db: AppDatabase): FollowDao = db.followDao()

    @Provides
    fun snapshotDao(db: AppDatabase): SnapshotDao = db.snapshotDao()

    @Provides
    fun followBackup(fake: InstrumentedFakeFollowBackup): FollowBackup = fake

    @Provides
    fun tefasClient(fake: InstrumentedFakeTefasClient): TefasClient = fake
}
