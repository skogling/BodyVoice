package com.liangji.bodymeasure.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "formula_history",
    foreignKeys = [
        ForeignKey(
            entity = MetricEntity::class,
            parentColumns = ["id"],
            childColumns = ["metric_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["metric_id", "created_at"],
            name = "idx_formula_history_metric",
        ),
    ],
)
data class FormulaHistoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "metric_id") val metricId: String,
    val version: Int,
    val formula: String,
    @ColumnInfo(name = "formula_display") val formulaDisplay: String,
    val unit: String,
    @ColumnInfo(name = "change_note") val changeNote: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "origin_device_id") val originDeviceId: String,
)
