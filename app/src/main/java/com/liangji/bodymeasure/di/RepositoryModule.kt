package com.liangji.bodymeasure.di

import com.liangji.bodymeasure.data.backup.RoomBackupRepository
import com.liangji.bodymeasure.data.local.datastore.DeviceIdProvider
import com.liangji.bodymeasure.data.local.datastore.DeviceIdStore
import com.liangji.bodymeasure.data.repository.DataStoreSettingsRepository
import com.liangji.bodymeasure.data.repository.RoomMeasurementRepository
import com.liangji.bodymeasure.data.repository.RoomMetricRepository
import com.liangji.bodymeasure.domain.repository.BackupRepository
import com.liangji.bodymeasure.domain.repository.MeasurementRepository
import com.liangji.bodymeasure.domain.repository.MetricRepository
import com.liangji.bodymeasure.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindMetricRepository(impl: RoomMetricRepository): MetricRepository

    @Binds
    @Singleton
    abstract fun bindMeasurementRepository(impl: RoomMeasurementRepository): MeasurementRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: RoomBackupRepository): BackupRepository

    @Binds
    @Singleton
    abstract fun bindDeviceIdProvider(impl: DeviceIdStore): DeviceIdProvider
}
