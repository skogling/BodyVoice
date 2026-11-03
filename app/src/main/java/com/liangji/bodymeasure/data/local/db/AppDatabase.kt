package com.liangji.bodymeasure.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.liangji.bodymeasure.data.local.entity.FormulaHistoryEntity
import com.liangji.bodymeasure.data.local.entity.MeasurementEntity
import com.liangji.bodymeasure.data.local.entity.MetricDependencyEntity
import com.liangji.bodymeasure.data.local.entity.MetricEntity

@Database(
    entities = [
        MetricEntity::class,
        MeasurementEntity::class,
        MetricDependencyEntity::class,
        FormulaHistoryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun metricDao(): MetricDao

    abstract fun measurementDao(): MeasurementDao

    abstract fun metricDependencyDao(): MetricDependencyDao

    abstract fun formulaHistoryDao(): FormulaHistoryDao
}
