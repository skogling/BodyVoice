package com.liangji.bodymeasure.testing

import com.liangji.bodymeasure.domain.model.Measurement
import com.liangji.bodymeasure.domain.model.MeasurementSource
import com.liangji.bodymeasure.domain.model.Metric
import com.liangji.bodymeasure.domain.model.MetricKind
import com.liangji.bodymeasure.domain.model.PairingPolicy
import com.liangji.bodymeasure.domain.model.UnitDimension
import java.time.LocalDate

fun rawMetric(
    id: String,
    name: String = id,
    unit: String = "kg",
    precision: Int = 1,
    isConstant: Boolean = false,
): Metric = Metric(
    id = id,
    name = name,
    kind = MetricKind.RAW,
    unit = unit,
    unitDimension = UnitDimension.MASS,
    precision = precision,
    colorArgb = 0xFFC96F4A,
    iconKey = "generic",
    formula = null,
    formulaDisplay = null,
    formulaVersion = 1,
    pairingPolicy = PairingPolicy.NEAREST,
    pairingWindowDays = 2,
    isConstant = isConstant,
    sortOrder = 0,
    isArchived = false,
    createdAt = 0,
    updatedAt = 0,
    deletedAt = null,
    originDeviceId = "test-device",
)

fun measured(
    metricId: String,
    date: LocalDate,
    value: Double,
    id: String = "$metricId-$date",
    note: String? = null,
): Measurement = Measurement(
    id = id,
    metricId = metricId,
    date = date,
    measuredAt = date.toEpochDay() * 86_400_000,
    value = value,
    note = note,
    source = MeasurementSource.MANUAL,
    createdAt = 0,
    updatedAt = 0,
    deletedAt = null,
    originDeviceId = "test-device",
)

fun derivedMetric(
    id: String,
    formula: String,
    name: String = id,
    unit: String = "",
    precision: Int = 1,
    pairingPolicy: PairingPolicy = PairingPolicy.NEAREST,
    pairingWindowDays: Int = 2,
    isConstant: Boolean = false,
): Metric = Metric(
    id = id,
    name = name,
    kind = MetricKind.DERIVED,
    unit = unit,
    unitDimension = UnitDimension.COMPOSITE,
    precision = precision,
    colorArgb = 0xFF3F7F72,
    iconKey = "generic",
    formula = formula,
    formulaDisplay = formula,
    formulaVersion = 1,
    pairingPolicy = pairingPolicy,
    pairingWindowDays = pairingWindowDays,
    isConstant = isConstant,
    sortOrder = 0,
    isArchived = false,
    createdAt = 0,
    updatedAt = 0,
    deletedAt = null,
    originDeviceId = "test-device",
)
