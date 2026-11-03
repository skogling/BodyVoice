package com.liangji.bodymeasure.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.liangji.bodymeasure.domain.model.MeasurementSource

@Entity(
    tableName = "measurement",
    foreignKeys = [
        ForeignKey(
            entity = MetricEntity::class,
            parentColumns = ["id"],
            childColumns = ["metric_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["metric_id", "local_date"], name = "idx_measurement_metric"),
        Index(
            value = ["metric_id", "deleted_at", "local_date"],
            name = "idx_measurement_active",
        ),
        Index(value = ["updated_at"], name = "idx_measurement_sync"),
    ],
)
data class MeasurementEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "metric_id") val metricId: String,
    @ColumnInfo(name = "local_date") val localDate: Long,
    @ColumnInfo(name = "measured_at") val measuredAt: Long,
    val value: Double,
    val note: String? = null,
    val source: MeasurementSource = MeasurementSource.MANUAL,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "origin_device_id") val originDeviceId: String,
)
