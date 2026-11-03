package com.liangji.bodymeasure.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.liangji.bodymeasure.core.common.AppClock
import com.liangji.bodymeasure.data.local.datastore.DeviceIdProvider
import com.liangji.bodymeasure.data.local.db.AppDatabase
import com.liangji.bodymeasure.data.repository.RoomMeasurementRepository
import com.liangji.bodymeasure.data.repository.RoomMetricRepository
import com.liangji.bodymeasure.domain.backup.BackupCodec
import com.liangji.bodymeasure.domain.backup.BackupExportResult
import com.liangji.bodymeasure.domain.backup.BackupImportResult
import com.liangji.bodymeasure.domain.backup.BackupPayload
import com.liangji.bodymeasure.domain.backup.BackupPreviewResult
import com.liangji.bodymeasure.domain.backup.CsvExportResult
import com.liangji.bodymeasure.domain.backup.ImportMode
import com.liangji.bodymeasure.domain.model.FormulaHistory
import com.liangji.bodymeasure.domain.model.MeasurementInput
import com.liangji.bodymeasure.domain.model.MeasurementWriteResult
import com.liangji.bodymeasure.domain.model.MetricDraft
import com.liangji.bodymeasure.domain.model.MetricKind
import com.liangji.bodymeasure.domain.model.MetricWriteResult
import com.liangji.bodymeasure.domain.model.UnitDimension
import com.liangji.bodymeasure.testing.derivedMetric
import com.liangji.bodymeasure.testing.measured
import com.liangji.bodymeasure.testing.rawMetric
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37])
class RoomBackupRepositoryTest {
    private val today = LocalDate.of(2026, 10, 30)
    private val clock = FakeClock(Instant.parse("2026-10-30T08:00:00Z"))
    private val deviceId = object : DeviceIdProvider {
        override suspend fun getOrCreate(): String = "test-device"
    }

    private lateinit var database: AppDatabase
    private lateinit var metrics: RoomMetricRepository
    private lateinit var measurements: RoomMeasurementRepository
    private lateinit var backup: RoomBackupRepository
    private lateinit var resolver: ShadowContentResolver

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        metrics = RoomMetricRepository(
            database = database,
            metricDao = database.metricDao(),
            dependencyDao = database.metricDependencyDao(),
            formulaHistoryDao = database.formulaHistoryDao(),
            clock = clock,
            deviceIdProvider = deviceId,
        )
        measurements = RoomMeasurementRepository(
            database = database,
            measurementDao = database.measurementDao(),
            metricDao = database.metricDao(),
            clock = clock,
            deviceIdProvider = deviceId,
        )
        backup = RoomBackupRepository(
            context = context,
            database = database,
            metricDao = database.metricDao(),
            measurementDao = database.measurementDao(),
            dependencyDao = database.metricDependencyDao(),
            formulaHistoryDao = database.formulaHistoryDao(),
            clock = clock,
        )
        resolver = Shadows.shadowOf(context.contentResolver)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun exported_backup_can_restore_everything_through_replace() = runBlocking {
        val weight = createRaw("体重", "kg", UnitDimension.MASS)
        val bmi = createDerived("BMI", "[体重] + 1")
        measurements.save(input(weight, today, 71.2), replaceExisting = false)

        val exportUri = outputUri()
        val output = ByteArrayOutputStream()
        resolver.registerOutputStream(exportUri, output)
        val exported = backup.exportBackup(exportUri.toString()) as BackupExportResult.Success

        assertEquals(2, exported.summary.metricCount)
        assertEquals(1, exported.summary.derivedCount)
        assertEquals(1, exported.summary.measurementCount)
        assertEquals(1, exported.summary.formulaHistoryCount)
        assertEquals(clock.now(), exported.summary.exportedAt)

        createRaw("腰围", "cm", UnitDimension.LENGTH)

        val imported = backup.importBackup(
            inputUri(output.toString(Charsets.UTF_8.name())).toString(),
            ImportMode.REPLACE,
        ) as BackupImportResult.Success

        assertEquals(2, imported.summary.metricsAdded)
        assertEquals(1, imported.summary.measurementsAdded)
        assertEquals(1, imported.summary.historyAdded)

        val active = metrics.observeActiveMetrics().first()
        assertEquals(listOf("体重", "BMI"), active.map { it.name })
        val restoredBmi = active.first { it.kind == MetricKind.DERIVED }
        assertEquals(bmi, restoredBmi.id)
        assertEquals(setOf(weight), database.metricDependencyDao().dependenciesOf(restoredBmi.id).toSet())
        assertEquals(listOf(weight), database.metricDependencyDao().getAll().map { it.dependsOnId })
        assertEquals(1, database.formulaHistoryDao().getAll().size)
        assertEquals(71.2, measurements.getForMetric(weight).single().value, 0.0)
        assertTrue(metrics.observeActiveMetrics().first().none { it.name == "腰围" })
    }

    @Test
    fun merge_matches_by_name_and_remaps_references() = runBlocking {
        val localWeight = createRaw("体重", "kg", UnitDimension.MASS)
        val uri = inputUri(BackupCodec.encode(mergePayload()))

        val preview = backup.previewBackup(uri.toString()) as BackupPreviewResult.Success
        assertEquals(2, preview.summary.metricCount)
        assertEquals(1, preview.summary.derivedCount)
        assertEquals(1, preview.summary.measurementCount)
        assertEquals(1, metrics.observeActiveMetrics().first().size)

        val result = backup.importBackup(uri.toString(), ImportMode.MERGE)
            as BackupImportResult.Success

        assertEquals(1, result.summary.metricsAdded)
        assertEquals(1, result.summary.metricsMatched)
        assertEquals(1, result.summary.measurementsAdded)
        assertEquals(0, result.summary.measurementsSkipped)
        assertEquals(1, result.summary.historyAdded)

        val active = metrics.observeActiveMetrics().first()
        assertEquals(listOf("体重", "BMI"), active.map { it.name })
        val bmi = active.first { it.kind == MetricKind.DERIVED }
        assertEquals("{${localWeight}} + 1", bmi.formula)
        assertEquals("[体重] + 1", bmi.formulaDisplay)
        assertEquals(listOf(localWeight), database.metricDependencyDao().dependenciesOf(bmi.id))

        val stored = measurements.getForMetric(localWeight).single()
        assertEquals("backup-weight-entry", stored.id)
        assertEquals(70.0, stored.value, 0.0)

        val history = metrics.observeFormulaHistory(bmi.id).first()
        assertEquals(1, history.size)
        assertEquals("{${localWeight}} + 1", history.first().formula)
        assertEquals("other-device", history.first().originDeviceId)
    }

    @Test
    fun merge_skips_days_that_already_have_a_record() = runBlocking {
        val weight = createRaw("体重", "kg", UnitDimension.MASS)
        measurements.save(input(weight, today, 78.5), replaceExisting = false)
        val payload = mergePayload().copy(
            metrics = mergePayload().metrics.filter { it.kind == MetricKind.RAW },
            formulaHistory = emptyList(),
        )

        val result = backup.importBackup(
            inputUri(BackupCodec.encode(payload)).toString(),
            ImportMode.MERGE,
        ) as BackupImportResult.Success

        assertEquals(0, result.summary.metricsAdded)
        assertEquals(1, result.summary.metricsMatched)
        assertEquals(0, result.summary.measurementsAdded)
        assertEquals(1, result.summary.measurementsSkipped)
        assertEquals(1, measurements.getForMetric(weight).size)
        assertEquals(78.5, measurements.getForMetric(weight).single().value, 0.0)
    }

    @Test
    fun merge_refuses_same_name_with_different_unit() = runBlocking {
        createRaw("体重", "kg", UnitDimension.MASS)
        val payload = mergePayload().copy(
            metrics = listOf(
                rawMetric("backup-weight", name = "体重", unit = "lb"),
            ),
            measurements = emptyList(),
            formulaHistory = emptyList(),
        )

        val result = backup.importBackup(
            inputUri(BackupCodec.encode(payload)).toString(),
            ImportMode.MERGE,
        )

        assertTrue(result is BackupImportResult.Failure)
        assertTrue("无法合并" in (result as BackupImportResult.Failure).message)
        assertEquals(1, metrics.observeActiveMetrics().first().size)
        assertEquals(0, database.measurementDao().getAllActive().size)
        assertEquals(0, database.metricDependencyDao().getAll().size)
    }

    @Test
    fun broken_file_leaves_existing_data_untouched() = runBlocking {
        val weight = createRaw("体重", "kg", UnitDimension.MASS)
        measurements.save(input(weight, today, 78.5), replaceExisting = false)

        val result = backup.importBackup(inputUri("{oops").toString(), ImportMode.MERGE)

        assertTrue(result is BackupImportResult.Failure)
        assertTrue("损坏" in (result as BackupImportResult.Failure).message)
        assertTrue(backup.previewBackup(inputUri("{oops").toString()) is BackupPreviewResult.Failure)
        assertEquals(1, metrics.observeActiveMetrics().first().size)
        assertEquals(1, measurements.getForMetric(weight).size)
    }

    @Test
    fun replace_clears_previous_data() = runBlocking {
        val weight = createRaw("体重", "kg", UnitDimension.MASS)
        createDerived("BMI", "[体重] + 1")
        measurements.save(input(weight, today, 78.5), replaceExisting = false)
        val payload = BackupPayload(
            appVersion = "0.4.0",
            exportedAt = Instant.parse("2026-10-30T08:00:00Z"),
            metrics = listOf(
                rawMetric("waist-new", name = "腰围", unit = "cm").copy(
                    unitDimension = UnitDimension.LENGTH,
                ),
            ),
            measurements = listOf(measured("waist-new", today, 80.0)),
            formulaHistory = emptyList(),
        )

        val result = backup.importBackup(
            inputUri(BackupCodec.encode(payload)).toString(),
            ImportMode.REPLACE,
        ) as BackupImportResult.Success

        assertEquals(1, result.summary.metricsAdded)
        assertEquals(0, result.summary.metricsMatched)
        assertEquals(1, result.summary.measurementsAdded)
        assertEquals(0, result.summary.historyAdded)

        assertEquals(listOf("腰围"), metrics.observeActiveMetrics().first().map { it.name })
        assertNull(metrics.observeMetric(weight).first())
        assertEquals(1, database.measurementDao().getAllActive().size)
        assertEquals(80.0, measurements.getForMetric("waist-new").single().value, 0.0)
        assertEquals(0, database.metricDependencyDao().getAll().size)
        assertEquals(0, database.formulaHistoryDao().getAll().size)
    }

    @Test
    fun csv_export_contains_only_active_rows() = runBlocking {
        val weight = createRaw("体重", "kg", UnitDimension.MASS)
        measurements.save(input(weight, today, 71.2), replaceExisting = false)
        val old = measurements.save(input(weight, today.minusDays(1), 79.0), replaceExisting = false)
            as MeasurementWriteResult.Saved
        measurements.delete(old.change.saved.id)

        val exportUri = outputUri()
        val output = ByteArrayOutputStream()
        resolver.registerOutputStream(exportUri, output)
        val result = backup.exportCsv(exportUri.toString()) as CsvExportResult.Success

        assertEquals(1, result.metricCount)
        assertEquals(1, result.measurementCount)
        val csv = output.toString(Charsets.UTF_8.name())
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("体重,2026-10-30,71.2,kg,"))
        assertFalse(csv.contains("79.0"))
    }

    private fun mergePayload(): BackupPayload = BackupPayload(
        appVersion = "0.4.0",
        exportedAt = Instant.parse("2026-10-30T08:00:00Z"),
        metrics = listOf(
            rawMetric("backup-weight", name = "体重", unit = "kg"),
            derivedMetric(
                id = "backup-bmi",
                formula = "{backup-weight} + 1",
                name = "BMI",
            ).copy(sortOrder = 1, formulaDisplay = "[体重] + 1"),
        ),
        measurements = listOf(
            measured("backup-weight", today, 70.0, id = "backup-weight-entry"),
        ),
        formulaHistory = listOf(
            FormulaHistory(
                id = "backup-history",
                metricId = "backup-bmi",
                version = 1,
                formula = "{backup-weight} + 1",
                formulaDisplay = "[体重] + 1",
                unit = "",
                changeNote = null,
                createdAt = 1_780_000_000_000,
                originDeviceId = "other-device",
            ),
        ),
    )

    private suspend fun createRaw(name: String, unit: String, dimension: UnitDimension): String =
        (metrics.create(
            MetricDraft(
                name = name,
                unit = unit,
                unitDimension = dimension,
                precision = 1,
                colorArgb = 0xFFC96F4A,
                iconKey = "generic",
            ),
        ) as MetricWriteResult.Saved).metric.id

    private suspend fun createDerived(name: String, formula: String): String =
        (metrics.create(
            MetricDraft(
                name = name,
                unit = "",
                unitDimension = UnitDimension.COMPOSITE,
                precision = 1,
                colorArgb = 0xFF3F7F72,
                iconKey = "generic",
                kind = MetricKind.DERIVED,
                formula = formula,
            ),
        ) as MetricWriteResult.Saved).metric.id

    private fun input(metricId: String, date: LocalDate, value: Double) = MeasurementInput(
        metricId = metricId,
        date = date,
        value = value,
        existingId = null,
    )

    private fun outputUri(): Uri = Uri.parse("content://com.liangji.bodymeasure.test/out-${UUID.randomUUID()}")

    private fun inputUri(text: String): Uri {
        val uri = Uri.parse("content://com.liangji.bodymeasure.test/in-${UUID.randomUUID()}")
        resolver.registerInputStreamSupplier(uri) {
            ByteArrayInputStream(text.toByteArray(Charsets.UTF_8))
        }
        return uri
    }

    private class FakeClock(private var instant: Instant) : AppClock {
        override fun now(): Instant = instant

        override fun today(): LocalDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
    }
}
