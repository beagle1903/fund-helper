package com.burha.fundhelper.di

import com.burha.fundhelper.data.tefas.OkHttpTefasClient
import com.burha.fundhelper.domain.Clock
import com.burha.fundhelper.domain.SystemClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
}