package com.liangji.bodymeasure.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.liangji.bodymeasure.domain.model.MetricKind
import com.liangji.bodymeasure.domain.model.PairingPolicy
import com.liangji.bodymeasure.domain.model.UnitDimension

@Entity(
    tableName = "metric_def",
    indices = [
        Index(
            value = ["deleted_at", "is_archived", "sort_order"],
            name = "idx_metric_active",
        ),
    ],
)
data class MetricEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: MetricKind,
    val unit: String,
    @ColumnInfo(name = "unit_dimension") val unitDimension: UnitDimension,
    @ColumnInfo(defaultValue = "1") val precision: Int = 1,
    @ColumnInfo(name = "color_argb") val colorArgb: Long,
    @ColumnInfo(name = "icon_key") val iconKey: String,
    val formula: String? = null,
    @ColumnInfo(name = "formula_display") val formulaDisplay: String? = null,
    @ColumnInfo(name = "formula_version", defaultValue = "1") val formulaVersion: Int = 1,
    @ColumnInfo(name = "pairing_policy", defaultValue = "'NEAREST'")
    val pairingPolicy: PairingPolicy = PairingPolicy.NEAREST,
    @ColumnInfo(name = "pairing_window_days", defaultValue = "2") val pairingWindowDays: Int = 2,
    @ColumnInfo(name = "is_constant", defaultValue = "0") val isConstant: Boolean = false,
    @ColumnInfo(name = "sort_order", defaultValue = "0") val sortOrder: Int = 0,
    @ColumnInfo(name = "is_archived", defaultValue = "0") val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "origin_device_id") val originDeviceId: String,
)
