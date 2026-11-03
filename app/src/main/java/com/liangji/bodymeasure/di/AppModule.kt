package com.liangji.bodymeasure.di

import com.liangji.bodymeasure.core.common.AppClock
import com.liangji.bodymeasure.core.common.SystemAppClock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindAppClock(impl: SystemAppClock): AppClock
}
