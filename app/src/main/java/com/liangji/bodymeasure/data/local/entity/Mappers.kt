package com.liangji.bodymeasure.data.local.entity

import com.liangji.bodymeasure.domain.model.FormulaHistory
import com.liangji.bodymeasure.domain.model.Measurement
import com.liangji.bodymeasure.domain.model.Metric
import java.time.LocalDate

internal fun MetricEntity.toDomain(): Metric = Metric(
    id = id,
    name = name,
    kind = kind,
    unit = unit,
    unitDimension = unitDimension,
    precision = precision,
    colorArgb = colorArgb,
    iconKey = iconKey,
    formula = formula,
    formulaDisplay = formulaDisplay,
    formulaVersion = formulaVersion,
    pairingPolicy = pairingPolicy,
    pairingWindowDays = pairingWindowDays,
    isConstant = isConstant,
    sortOrder = sortOrder,
    isArchived = isArchived,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    originDeviceId = originDeviceId,
)

internal fun MeasurementEntity.toDomain(): Measurement = Measurement(
    id = id,
    metricId = metricId,
    date = LocalDate.ofEpochDay(localDate),
    measuredAt = measuredAt,
    value = value,
    note = note,
    source = source,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    originDeviceId = originDeviceId,
)

internal fun Measurement.toEntity(): MeasurementEntity = MeasurementEntity(
    id = id,
    metricId = metricId,
    localDate = date.toEpochDay(),
    measuredAt = measuredAt,
    value = value,
    note = note,
    source = source,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    originDeviceId = originDeviceId,
)

internal fun Metric.toEntity(): MetricEntity = MetricEntity(
    id = id,
    name = name,
    kind = kind,
    unit = unit,
    unitDimension = unitDimension,
    precision = precision,
    colorArgb = colorArgb,
    iconKey = iconKey,
    formula = formula,
    formulaDisplay = formulaDisplay,
    formulaVersion = formulaVersion,
    pairingPolicy = pairingPolicy,
    pairingWindowDays = pairingWindowDays,
    isConstant = isConstant,
    sortOrder = sortOrder,
    isArchived = isArchived,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    originDeviceId = originDeviceId,
)

internal fun FormulaHistory.toEntity(): FormulaHistoryEntity = FormulaHistoryEntity(
    id = id,
    metricId = metricId,
    version = version,
    formula = formula,
    formulaDisplay = formulaDisplay,
    unit = unit,
    changeNote = changeNote,
    createdAt = createdAt,
    originDeviceId = originDeviceId,
)

internal fun FormulaHistoryEntity.toDomain(): FormulaHistory = FormulaHistory(
    id = id,
    metricId = metricId,
    version = version,
    formula = formula,
    formulaDisplay = formulaDisplay,
    unit = unit,
    changeNote = changeNote,
    createdAt = createdAt,
    originDeviceId = originDeviceId,
)
