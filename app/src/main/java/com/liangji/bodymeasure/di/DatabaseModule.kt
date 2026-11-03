package com.liangji.bodymeasure.di

import android.content.Context
import androidx.room.Room
import com.liangji.bodymeasure.data.local.db.AppDatabase
import com.liangji.bodymeasure.data.local.db.FormulaHistoryDao
import com.liangji.bodymeasure.data.local.db.MeasurementDao
import com.liangji.bodymeasure.data.local.db.MetricDao
import com.liangji.bodymeasure.data.local.db.MetricDependencyDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "bodymeasure.db").build()

    @Provides
    fun provideMetricDao(database: AppDatabase): MetricDao = database.metricDao()

    @Provides
    fun provideMeasurementDao(database: AppDatabase): MeasurementDao = database.measurementDao()

    @Provides
    fun provideMetricDependencyDao(database: AppDatabase): MetricDependencyDao =
        database.metricDependencyDao()

    @Provides
    fun provideFormulaHistoryDao(database: AppDatabase): FormulaHistoryDao =
        database.formulaHistoryDao()
}
