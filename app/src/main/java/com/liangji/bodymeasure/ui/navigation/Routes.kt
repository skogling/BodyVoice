package com.liangji.bodymeasure.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object MetricsRoute

@Serializable
data class MetricEditRoute(val metricId: String? = null)

@Serializable
data class MetricDetailRoute(val metricId: String)

@Serializable
data object ArchivedMetricsRoute

@Serializable
data object SettingsRoute
