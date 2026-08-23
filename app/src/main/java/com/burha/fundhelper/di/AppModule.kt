package com.burha.fundhelper.di

import android.content.Context
import androidx.room.Room
import com.burha.fundhelper.data.local.AppDatabase
import com.burha.fundhelper.data.local.FollowDao
import com.burha.fundhelper.data.local.SnapshotDao
import com.burha.fundhelper.data.tefas.OkHttpTefasClient
import com.burha.fundhelper.domain.Clock
import com.burha.fundhelper.domain.SystemClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun okHttpClient(): OkHttpClient = OkHttpTefasClient.defaultClient()

    @Provides
    @Singleton
    fun clock(): Clock = SystemClock()

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "fund-helper.db").build()

    @Provides
    fun followDao(db: AppDatabase): FollowDao = db.followDao()

    @Provides
    fun snapshotDao(db: AppDatabase): SnapshotDao = db.snapshotDao()
}
