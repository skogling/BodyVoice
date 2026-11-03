package com.liangji.bodymeasure.ui

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.liangji.bodymeasure.core.designsystem.BodyMeasureTheme
import com.liangji.bodymeasure.core.designsystem.SoftGradientBackground
import com.liangji.bodymeasure.domain.backup.BackupSummary
import com.liangji.bodymeasure.domain.backup.ImportMode
import com.liangji.bodymeasure.domain.model.ThemeMode
import com.liangji.bodymeasure.ui.settings.BackupNotice
import com.liangji.bodymeasure.ui.settings.BackupTask
import com.liangji.bodymeasure.ui.settings.BackupUiState
import com.liangji.bodymeasure.ui.settings.PendingImport
import com.liangji.bodymeasure.ui.settings.SettingsContent
import com.github.takahirom.roborazzi.captureScreenRoboImage
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [37],
    qualifiers = "w393dp-h852dp-xxhdpi",
    application = Application::class,
)
class SettingsBackupUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun importPreviewShowsSummaryAndMerges() {
        var mode: ImportMode? = null
        setSettings(
            backup = BackupUiState(
                pendingImport = PendingImport(uri = "content://backup", summary = summary()),
            ),
            onConfirmImport = { mode = it },
        )

        composeRule.onNodeWithText("导入备份？").assertIsDisplayed()
        composeRule.onNodeWithText("备份包含 6 个指标", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("合并导入").assertIsDisplayed().performClick()

        assertEquals(ImportMode.MERGE, mode)
    }

    @Test
    fun importPreviewCanReplaceEverything() {
        var mode: ImportMode? = null
        setSettings(
            backup = BackupUiState(
                pendingImport = PendingImport(uri = "content://backup", summary = summary()),
            ),
            onConfirmImport = { mode = it },
        )

        composeRule.onNodeWithText("替换全部").performClick()

        assertEquals(ImportMode.REPLACE, mode)
    }

    @Test
    fun importPreviewCanBeCancelled() {
        var cancelled = false
        setSettings(
            backup = BackupUiState(
                pendingImport = PendingImport(uri = "content://backup", summary = summary()),
            ),
            onCancelImport = { cancelled = true },
        )

        composeRule.onNodeWithText("取消").performClick()

        assertTrue(cancelled)
    }

    @Test
    fun noticeShowsMessageAndDismisses() {
        var dismissed = false
        setSettings(
            backup = BackupUiState(
                notice = BackupNotice("导入完成", "新增 2 个指标，新增 8 条记录。"),
            ),
            onDismissNotice = { dismissed = true },
        )

        composeRule.onNodeWithText("导入完成").assertIsDisplayed()
        composeRule.onNodeWithText("新增 2 个指标，新增 8 条记录。").assertIsDisplayed()
        composeRule.onNodeWithText("知道了").performClick()

        assertTrue(dismissed)
    }

    @Test
    fun busyDialogDescribesTheTask() {
        setSettings(backup = BackupUiState(task = BackupTask.IMPORT))

        composeRule.onNodeWithText("正在导入").assertIsDisplayed()
        composeRule.onNodeWithText("写入指标与记录").assertIsDisplayed()
    }

    @Test
    fun importDialogScreenshot() {
        setSettings(
            backup = BackupUiState(
                pendingImport = PendingImport(uri = "content://backup", summary = summary()),
            ),
        )

        composeRule.waitForIdle()
        captureScreenRoboImage("build/screenshots/settings-import-dialog.png")
    }

    @Test
    fun importResultScreenshot() {
        setSettings(
            backup = BackupUiState(
                notice = BackupNotice(
                    "导入完成",
                    "新增 2 个指标，匹配 3 个已有指标，新增 8 条记录，跳过 1 条重复记录。",
                ),
            ),
        )

        composeRule.waitForIdle()
        captureScreenRoboImage("build/screenshots/settings-import-result.png")
    }

    private fun setSettings(
        backup: BackupUiState,
        onConfirmImport: (ImportMode) -> Unit = {},
        onCancelImport: () -> Unit = {},
        onDismissNotice: () -> Unit = {},
    ) {
        composeRule.setContent {
            BodyMeasureTheme(themeMode = ThemeMode.LIGHT) {
                SoftGradientBackground {
                    SettingsContent(
                        themeMode = ThemeMode.SYSTEM,
                        archivedCount = 0,
                        backup = backup,
                        onSelectTheme = {},
                        onOpenArchive = {},
                        onExportBackup = {},
                        onExportCsv = {},
                        onImportBackup = {},
                        onConfirmImport = onConfirmImport,
                        onCancelImport = onCancelImport,
                        onDismissNotice = onDismissNotice,
                        onBack = {},
                    )
                }
            }
        }
    }

    private fun summary() = BackupSummary(
        metricCount = 6,
        derivedCount = 2,
        measurementCount = 184,
        formulaHistoryCount = 3,
        exportedAt = Instant.parse("2026-10-29T09:30:00Z"),
        appVersion = "0.4.0",
    )
}
