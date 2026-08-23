package com.burha.fundhelper.di

import com.burha.fundhelper.data.tefas.OkHttpTefasClient
import com.burha.fundhelper.data.tefas.TefasClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TefasModule {
    @Binds
    @Singleton
    abstract fun tefasClient(impl: OkHttpTefasClient): TefasClient
}