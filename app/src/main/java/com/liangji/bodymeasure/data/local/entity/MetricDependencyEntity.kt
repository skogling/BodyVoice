package com.liangji.bodymeasure.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "metric_dependency",
    primaryKeys = ["metric_id", "depends_on_id"],
    foreignKeys = [
        ForeignKey(
            entity = MetricEntity::class,
            parentColumns = ["id"],
            childColumns = ["metric_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MetricEntity::class,
            parentColumns = ["id"],
            childColumns = ["depends_on_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["depends_on_id"], name = "idx_dependency_target"),
    ],
)
data class MetricDependencyEntity(
    @ColumnInfo(name = "metric_id") val metricId: String,
    @ColumnInfo(name = "depends_on_id") val dependsOnId: String,
)
