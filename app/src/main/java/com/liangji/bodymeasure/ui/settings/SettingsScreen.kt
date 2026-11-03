package com.liangji.bodymeasure.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.liangji.bodymeasure.BuildConfig
import com.liangji.bodymeasure.R
import com.liangji.bodymeasure.core.designsystem.AppIcons
import com.liangji.bodymeasure.core.designsystem.BodyMeasureTheme
import com.liangji.bodymeasure.core.designsystem.GlassDivider
import com.liangji.bodymeasure.core.designsystem.GlassIconButton
import com.liangji.bodymeasure.core.designsystem.Motion
import com.liangji.bodymeasure.core.designsystem.SoftGlassSurface
import com.liangji.bodymeasure.core.designsystem.SoftGradientBackground
import com.liangji.bodymeasure.core.designsystem.Spacing
import com.liangji.bodymeasure.domain.backup.BackupSummary
import com.liangji.bodymeasure.domain.backup.ImportMode
import com.liangji.bodymeasure.domain.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenArchive: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val archivedCount by viewModel.archivedCount.collectAsStateWithLifecycle()
    val backup by viewModel.backup.collectAsStateWithLifecycle()

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let { viewModel.exportBackup(it.toString()) } }
    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> uri?.let { viewModel.exportCsv(it.toString()) } }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.previewImport(it.toString()) } }

    SettingsContent(
        themeMode = themeMode,
        archivedCount = archivedCount,
        backup = backup,
        onSelectTheme = viewModel::setThemeMode,
        onOpenArchive = onOpenArchive,
        onExportBackup = { exportBackupLauncher.launch(viewModel.backupFileName()) },
        onExportCsv = { exportCsvLauncher.launch(viewModel.csvFileName()) },
        onImportBackup = {
            importLauncher.launch(
                arrayOf("application/json", "text/plain", "application/octet-stream"),
            )
        },
        onConfirmImport = viewModel::confirmImport,
        onCancelImport = viewModel::cancelImport,
        onDismissNotice = viewModel::dismissNotice,
        onBack = onBack,
    )
}

@Composable
internal fun SettingsContent(
    themeMode: ThemeMode,
    archivedCount: Int,
    backup: BackupUiState,
    onSelectTheme: (ThemeMode) -> Unit,
    onOpenArchive: () -> Unit,
    onExportBackup: () -> Unit,
    onExportCsv: () -> Unit,
    onImportBackup: () -> Unit,
    onConfirmImport: (ImportMode) -> Unit,
    onCancelImport: () -> Unit,
    onDismissNotice: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    var showReward by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen)
                .padding(top = Spacing.xs, bottom = Spacing.xxl),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(
                    imageVector = AppIcons.ArrowBack,
                    contentDescription = "返回",
                    onClick = onBack,
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = "设置",
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.ink,
                    modifier = Modifier.semantics { heading() },
                )
            }

            Spacer(Modifier.height(Spacing.xl))

            SectionLabel("外观")
            Spacer(Modifier.height(Spacing.xs))
            SoftGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(Spacing.lg),
            ) {
                Text(
                    text = "主题",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "跟随系统会随澎湃OS的深浅色设置切换。",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
                Spacer(Modifier.height(Spacing.md))
                ThemeSelector(selected = themeMode, onSelect = onSelectTheme)
            }

            Spacer(Modifier.height(Spacing.xl))

            SectionLabel("指标")
            Spacer(Modifier.height(Spacing.xs))
            SoftGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(Spacing.lg),
                onClick = onOpenArchive,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colors.plum.copy(alpha = if (colors.isDark) 0.16f else 0.1f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = AppIcons.Archive,
                            contentDescription = null,
                            tint = colors.plum,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    Spacer(Modifier.width(Spacing.sm))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "已归档指标",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.ink,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "不常记录的指标可以收起来，记录仍然保留。",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.muted,
                        )
                    }
                    if (archivedCount > 0) {
                        Spacer(Modifier.width(Spacing.xs))
                        Text(
                            text = "$archivedCount",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.muted,
                        )
                    }
                    Icon(
                        imageVector = AppIcons.ChevronRight,
                        contentDescription = null,
                        tint = colors.muted,
                        modifier = Modifier
                            .padding(start = Spacing.xxs)
                            .size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            SectionLabel("数据与备份")
            Spacer(Modifier.height(Spacing.xs))
            SoftGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xs),
            ) {
                DataActionRow(
                    icon = AppIcons.Download,
                    accent = colors.teal,
                    title = "导出备份",
                    subtitle = "生成 JSON 文件，换机或重装后可以恢复。",
                    enabled = !backup.isBusy,
                    onClick = onExportBackup,
                )
                GlassDivider()
                DataActionRow(
                    icon = AppIcons.Table,
                    accent = colors.ochre,
                    title = "导出表格",
                    subtitle = "生成 CSV，用 Excel 或 WPS 查看分析。",
                    enabled = !backup.isBusy,
                    onClick = onExportCsv,
                )
                GlassDivider()
                DataActionRow(
                    icon = AppIcons.Upload,
                    accent = colors.clay,
                    title = "从备份导入",
                    subtitle = "读取之前的备份，可合并或替换当前数据。",
                    enabled = !backup.isBusy,
                    onClick = onImportBackup,
                )
            }

            Spacer(Modifier.height(Spacing.xl))

            SectionLabel("数据与隐私")
            Spacer(Modifier.height(Spacing.xs))
            SoftGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(Spacing.lg),
            ) {
                InfoRow(
                    icon = AppIcons.Shield,
                    accent = colors.clay,
                    title = "纯本地存储",
                    subtitle = "身体数据只写入本机数据库，没有账号。",
                )
                Spacer(Modifier.height(Spacing.md))
                GlassDivider()
                Spacer(Modifier.height(Spacing.md))
                InfoRow(
                    icon = AppIcons.Database,
                    accent = colors.teal,
                    title = "备份随时带走",
                    subtitle = "导出的是普通文件，不经过任何服务器。",
                )
                Spacer(Modifier.height(Spacing.md))
                GlassDivider()
                Spacer(Modifier.height(Spacing.md))
                InfoRow(
                    icon = AppIcons.Info,
                    accent = colors.ochre,
                    title = "无网络权限",
                    subtitle = "应用不申请 INTERNET，无法上传或联网同步。",
                )
            }

            Spacer(Modifier.height(Spacing.xl))

            SectionLabel("关于")
            Spacer(Modifier.height(Spacing.xs))
            SoftGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(Spacing.lg),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.mist.copy(alpha = if (colors.isDark) 0.18f else 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "身",
                            style = MaterialTheme.typography.headlineSmall,
                            color = colors.mist,
                        )
                    }
                    Spacer(Modifier.width(Spacing.sm))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "身语",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.ink,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "听见身体 · 看见变化",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.muted,
                        )
                    }
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        text = BuildConfig.VERSION_NAME,
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.muted,
                    )
                }
                Spacer(Modifier.height(Spacing.md))
                GlassDivider()
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = "身语不会上传你的数据。卸载应用会删除全部记录，建议定期导出备份。",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
                Spacer(Modifier.height(Spacing.sm))
                GlassDivider()
                DataActionRow(
                    icon = AppIcons.Heart,
                    accent = colors.clay,
                    title = "赞赏作者",
                    subtitle = "请 skogling 喝杯咖啡，感谢你的支持。",
                    enabled = true,
                    onClick = { showReward = true },
                )
            }
        }
    }

    if (showReward) {
        RewardDialog(onDismiss = { showReward = false })
    }

    if (backup.pendingImport != null) {
        ImportConfirmDialog(
            summary = backup.pendingImport.summary,
            onMerge = { onConfirmImport(ImportMode.MERGE) },
            onReplace = { onConfirmImport(ImportMode.REPLACE) },
            onCancel = onCancelImport,
        )
    }

    backup.notice?.let { notice ->
        AlertDialog(
            onDismissRequest = onDismissNotice,
            containerColor = colors.sheet,
            shape = RoundedCornerShape(24.dp),
            titleContentColor = colors.ink,
            textContentColor = colors.inkSecondary,
            title = {
                Text(
                    text = notice.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (notice.isError) MaterialTheme.colorScheme.error else colors.ink,
                )
            },
            text = {
                Text(text = notice.message, style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                TextButton(onClick = onDismissNotice) {
                    Text("知道了", color = colors.clay)
                }
            },
        )
    }

    backup.task?.let { task -> BackupBusyDialog(task) }
}

private enum class QrSaveState { IDLE, SAVING, SAVED, UNSUPPORTED, FAILED }

@Composable
private fun RewardDialog(onDismiss: () -> Unit) {
    val colors = BodyMeasureTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var saveState by remember { mutableStateOf(QrSaveState.IDLE) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.sheet,
        shape = RoundedCornerShape(24.dp),
        titleContentColor = colors.ink,
        textContentColor = colors.inkSecondary,
        title = {
            Text(text = "赞赏作者", style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "如果身语帮到了你，欢迎请作者喝杯咖啡。",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.md))
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .combinedClickable(
                            onClick = {},
                            onLongClickLabel = "保存赞赏二维码到相册",
                            onLongClick = {
                                if (saveState == QrSaveState.IDLE || saveState == QrSaveState.FAILED) {
                                    saveState = QrSaveState.SAVING
                                    scope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            saveRewardQr(context)
                                        }
                                        saveState = when (result) {
                                            RewardQrSaveResult.SAVED -> QrSaveState.SAVED
                                            RewardQrSaveResult.UNSUPPORTED -> QrSaveState.UNSUPPORTED
                                            RewardQrSaveResult.FAILED -> QrSaveState.FAILED
                                        }
                                    }
                                }
                            },
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.wechat_reward),
                        contentDescription = "微信赞赏二维码",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit,
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = "微信扫一扫 · skogling",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.inkSecondary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = when (saveState) {
                        QrSaveState.IDLE -> "长按二维码可保存到相册"
                        QrSaveState.SAVING -> "正在保存…"
                        QrSaveState.SAVED -> "已保存到相册"
                        QrSaveState.UNSUPPORTED -> "当前系统版本暂不支持保存"
                        QrSaveState.FAILED -> "保存失败，请稍后再试"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (saveState == QrSaveState.SAVED) colors.clay else colors.muted,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "关闭",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.clay,
                )
            }
        },
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = BodyMeasureTheme.colors.muted,
        modifier = Modifier.padding(start = Spacing.xxs),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeSelector(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    FlowRow(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        ThemeMode.entries.forEach { mode ->
            val isSelected = mode == selected
            val chipShape = RoundedCornerShape(12.dp)
            val background by animateColorAsState(
                targetValue = if (isSelected) {
                    colors.clay.copy(alpha = if (colors.isDark) 0.22f else 0.14f)
                } else {
                    Color.Transparent
                },
                animationSpec = tween(Motion.quick),
                label = "themeChipBackground",
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) colors.ink else colors.muted,
                animationSpec = tween(Motion.quick),
                label = "themeChipContent",
            )
            val outlineColor by animateColorAsState(
                targetValue = if (isSelected) colors.clay.copy(alpha = 0.55f) else colors.line,
                animationSpec = tween(Motion.quick),
                label = "themeChipOutline",
            )
            Box(
                modifier = Modifier
                    .clip(chipShape)
                    .background(background)
                    .border(1.dp, outlineColor, chipShape)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(mode) },
                    )
                    .heightIn(min = 42.dp)
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = mode.label(),
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                )
            }
        }
    }
}

@Composable
private fun DataActionRow(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = if (colors.isDark) 0.16f else 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.width(Spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) colors.ink else colors.muted,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
        }
        Icon(
            imageVector = AppIcons.ChevronRight,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier
                .padding(start = Spacing.xxs)
                .size(18.dp),
        )
    }
}

@Composable
private fun ImportConfirmDialog(
    summary: BackupSummary,
    onMerge: () -> Unit,
    onReplace: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = BodyMeasureTheme.colors
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = colors.sheet,
        shape = RoundedCornerShape(24.dp),
        titleContentColor = colors.ink,
        textContentColor = colors.inkSecondary,
        title = {
            Text(text = "导入备份？", style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Text(
                text = BackupMessages.importPreview(summary),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            Row {
                TextButton(onClick = onReplace) {
                    Text(
                        text = "替换全部",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                TextButton(onClick = onMerge) {
                    Text(
                        text = "合并导入",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.clay,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(
                    text = "取消",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.muted,
                )
            }
        },
    )
}

@Composable
private fun BackupBusyDialog(task: BackupTask) {
    val colors = BodyMeasureTheme.colors
    AlertDialog(
        onDismissRequest = {},
        containerColor = colors.sheet,
        shape = RoundedCornerShape(24.dp),
        titleContentColor = colors.ink,
        textContentColor = colors.inkSecondary,
        title = {
            Text(text = task.title(), style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = colors.clay,
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(text = task.detail(), style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {},
    )
}

private fun BackupTask.title(): String = when (this) {
    BackupTask.EXPORT_BACKUP -> "正在导出备份"
    BackupTask.EXPORT_CSV -> "正在导出表格"
    BackupTask.PREVIEW_IMPORT -> "正在读取备份"
    BackupTask.IMPORT -> "正在导入"
}

private fun BackupTask.detail(): String = when (this) {
    BackupTask.EXPORT_BACKUP -> "把指标和记录写成 JSON 文件"
    BackupTask.EXPORT_CSV -> "整理记录并生成 CSV 文件"
    BackupTask.PREVIEW_IMPORT -> "检查备份内容"
    BackupTask.IMPORT -> "写入指标与记录"
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
) {
    val colors = BodyMeasureTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = if (colors.isDark) 0.16f else 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.width(Spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
        }
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "跟随系统"
    ThemeMode.LIGHT -> "浅色"
    ThemeMode.DARK -> "深色"
}

@Preview(name = "设置 · 浅色", widthDp = 360, heightDp = 900, showBackground = true)
@Composable
private fun SettingsLightPreview() {
    BodyMeasureTheme(themeMode = ThemeMode.LIGHT) {
        SoftGradientBackground {
            SettingsContent(
                themeMode = ThemeMode.SYSTEM,
                archivedCount = 2,
                backup = BackupUiState(),
                onSelectTheme = {},
                onOpenArchive = {},
                onExportBackup = {},
                onExportCsv = {},
                onImportBackup = {},
                onConfirmImport = {},
                onCancelImport = {},
                onDismissNotice = {},
                onBack = {},
            )
        }
    }
}

@Preview(name = "设置 · 深色", widthDp = 360, heightDp = 900, showBackground = true)
@Composable
private fun SettingsDarkPreview() {
    BodyMeasureTheme(themeMode = ThemeMode.DARK) {
        SoftGradientBackground {
            SettingsContent(
                themeMode = ThemeMode.DARK,
                archivedCount = 0,
                backup = BackupUiState(),
                onSelectTheme = {},
                onOpenArchive = {},
                onExportBackup = {},
                onExportCsv = {},
                onImportBackup = {},
                onConfirmImport = {},
                onCancelImport = {},
                onDismissNotice = {},
                onBack = {},
            )
        }
    }
}
