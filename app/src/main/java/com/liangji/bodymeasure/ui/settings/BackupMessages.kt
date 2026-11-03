package com.liangji.bodymeasure.ui.settings

import com.liangji.bodymeasure.domain.backup.BackupSummary
import com.liangji.bodymeasure.domain.backup.ImportMode
import com.liangji.bodymeasure.domain.backup.ImportSummary
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal object BackupMessages {
    private val timeFormat = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm")

    fun exportSuccess(summary: BackupSummary): String = buildString {
        append("共 ${summary.metricCount} 个指标、${summary.measurementCount} 条记录")
        if (summary.formulaHistoryCount > 0) {
            append("、${summary.formulaHistoryCount} 条公式历史")
        }
        append("。\n备份文件已写入你选择的位置。")
    }

    fun csvSuccess(metricCount: Int, measurementCount: Int): String =
        "共 $metricCount 个指标、$measurementCount 条记录。\n可以用 Excel、WPS 或 Numbers 打开。"

    fun importPreview(summary: BackupSummary): String = buildString {
        append("备份包含 ${summary.metricCount} 个指标")
        if (summary.derivedCount > 0) {
            append("（其中 ${summary.derivedCount} 个派生指标）")
        }
        append("、${summary.measurementCount} 条记录。")
        summary.exportedAt?.let {
            append("\n导出时间：")
            append(timeFormat.format(it.atZone(ZoneId.systemDefault())))
        }
        append("\n\n合并：保留现有数据，同指标同一天的记录不会重复。")
        append("\n替换：清空当前全部数据，只保留备份内容。")
    }

    fun importSuccess(summary: ImportSummary): String = when (summary.mode) {
        ImportMode.MERGE -> buildString {
            append("新增 ${summary.metricsAdded} 个指标")
            if (summary.metricsMatched > 0) {
                append("，匹配 ${summary.metricsMatched} 个已有指标")
            }
            append("，新增 ${summary.measurementsAdded} 条记录")
            if (summary.measurementsSkipped > 0) {
                append("，跳过 ${summary.measurementsSkipped} 条重复记录")
            }
            if (summary.historyAdded > 0) {
                append("，补全 ${summary.historyAdded} 条公式历史")
            }
            append("。")
        }

        ImportMode.REPLACE ->
            "已替换为备份内容：${summary.metricsAdded} 个指标、" +
                "${summary.measurementsAdded} 条记录、${summary.historyAdded} 条公式历史。"
    }
}
