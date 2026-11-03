package com.liangji.bodymeasure.data.local.db

import androidx.room.TypeConverter
import com.liangji.bodymeasure.domain.model.MeasurementSource
import com.liangji.bodymeasure.domain.model.MetricKind
import com.liangji.bodymeasure.domain.model.PairingPolicy
import com.liangji.bodymeasure.domain.model.UnitDimension

class Converters {
    @TypeConverter
    fun metricKindToString(value: MetricKind): String = value.name

    @TypeConverter
    fun stringToMetricKind(value: String): MetricKind = MetricKind.valueOf(value)

    @TypeConverter
    fun unitDimensionToString(value: UnitDimension): String = value.name

    @TypeConverter
    fun stringToUnitDimension(value: String): UnitDimension = UnitDimension.valueOf(value)

    @TypeConverter
    fun pairingPolicyToString(value: PairingPolicy): String = value.name

    @TypeConverter
    fun stringToPairingPolicy(value: String): PairingPolicy = PairingPolicy.valueOf(value)

    @TypeConverter
    fun measurementSourceToString(value: MeasurementSource): String = value.name

    @TypeConverter
    fun stringToMeasurementSource(value: String): MeasurementSource =
        MeasurementSource.valueOf(value)
}
