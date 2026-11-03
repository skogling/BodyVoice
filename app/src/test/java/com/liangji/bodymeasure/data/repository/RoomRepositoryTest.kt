package com.liangji.bodymeasure.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.liangji.bodymeasure.core.common.AppClock
import com.liangji.bodymeasure.data.local.datastore.DeviceIdProvider
import com.liangji.bodymeasure.data.local.db.AppDatabase
import com.liangji.bodymeasure.domain.model.MeasurementInput
import com.liangji.bodymeasure.domain.model.MeasurementWriteResult
import com.liangji.bodymeasure.domain.model.MetricDeleteResult
import com.liangji.bodymeasure.domain.model.MetricDraft
import com.liangji.bodymeasure.domain.model.MetricMove
import com.liangji.bodymeasure.domain.model.MetricWriteResult
import com.liangji.bodymeasure.domain.model.UnitDimension
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37])
class RoomRepositoryTest {
    private val date = LocalDate.of(2026, 10, 29)
    private val clock = FakeClock(Instant.parse("2026-10-29T08:00:00Z"))
    private val deviceId = object : DeviceIdProvider {
        override suspend fun getOrCreate(): String = "test-device"
    }

    private lateinit var database: AppDatabase
    private lateinit var metrics: RoomMetricRepository
    private lateinit var measurements: RoomMeasurementRepository

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
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun create_trims_name_and_assigns_sort_order() = runBlocking {
        val first = metrics.create(draft(name = " 体重 ")) as MetricWriteResult.Saved
        val second = metrics.create(draft(name = "腰围", unit = "cm", dimension = UnitDimension.LENGTH)) as MetricWriteResult.Saved

        assertEquals("体重", first.metric.name)
        assertEquals(0, first.metric.sortOrder)
        assertEquals(1, second.metric.sortOrder)
        assertEquals("test-device", first.metric.originDeviceId)
    }

    @Test
    fun create_rejects_duplicate_name_ignoring_case() = runBlocking {
        metrics.create(draft(name = "Waist"))

        val duplicate = metrics.create(draft(name = " waist "))

        assertTrue(duplicate is MetricWriteResult.DuplicateName)
    }

    @Test
    fun archive_and_delete_update_visible_lists() = runBlocking {
        val id = createMetric()

        assertTrue(metrics.setArchived(id, archived = true))
        assertTrue(metrics.observeActiveMetrics().first().isEmpty())
        assertEquals(1, metrics.observeArchivedMetrics().first().size)

        assertTrue(metrics.setArchived(id, archived = false))
        assertEquals(1, metrics.observeActiveMetrics().first().size)

        assertTrue(metrics.delete(id) is MetricDeleteResult.Deleted)
        assertTrue(metrics.observeActiveMetrics().first().isEmpty())
        assertTrue(metrics.observeArchivedMetrics().first().isEmpty())
        assertNull(metrics.observeMetric(id).first())
    }

    @Test
    fun move_reorders_metrics_and_clamps_edges() = runBlocking {
        val a = (metrics.create(draft(name = "体重")) as MetricWriteResult.Saved).metric.id
        val b = (metrics.create(draft(name = "腰围")) as MetricWriteResult.Saved).metric.id
        val c = (metrics.create(draft(name = "胸围")) as MetricWriteResult.Saved).metric.id

        assertTrue(metrics.move(c, MetricMove.TOP))
        assertEquals(listOf(c, a, b), activeIds())
        assertEquals(listOf(0, 1, 2), activeOrders())

        assertTrue(metrics.move(c, MetricMove.DOWN))
        assertEquals(listOf(a, c, b), activeIds())

        assertTrue(metrics.move(c, MetricMove.UP))
        assertEquals(listOf(c, a, b), activeIds())

        assertTrue(metrics.move(a, MetricMove.DOWN))
        assertEquals(listOf(c, b, a), activeIds())

        assertTrue(metrics.move(a, MetricMove.DOWN))
        assertEquals(listOf(c, b, a), activeIds())
        assertTrue(metrics.move(c, MetricMove.TOP))
        assertEquals(listOf(c, b, a), activeIds())

        assertTrue(!metrics.move("missing", MetricMove.UP))
    }

    @Test
    fun archived_metric_keeps_its_name_reserved() = runBlocking {
        val id = createMetric()
        metrics.setArchived(id, archived = true)

        val duplicate = metrics.create(draft(name = "体重"))

        assertTrue(duplicate is MetricWriteResult.DuplicateName)
    }

    @Test
    fun save_same_day_returns_conflict_without_replacing() = runBlocking {
        val metricId = createMetric()
        val first = measurements.save(input(metricId, value = 78.5), replaceExisting = false)
        assertTrue(first is MeasurementWriteResult.Saved)

        val second = measurements.save(input(metricId, value = 79.0), replaceExisting = false)

        assertTrue(second is MeasurementWriteResult.Conflict)
        assertEquals(
            (first as MeasurementWriteResult.Saved).change.saved.id,
            (second as MeasurementWriteResult.Conflict).existing.id,
        )
        assertEquals(78.5, measurements.getForMetric(metricId).single().value, 0.0)
    }

    @Test
    fun replace_soft_deletes_previous_and_undo_restores_it() = runBlocking {
        val metricId = createMetric()
        val original = measurements.save(input(metricId, value = 78.5), replaceExisting = false)
            as MeasurementWriteResult.Saved

        val replaced = measurements.save(input(metricId, value = 79.0), replaceExisting = true)
            as MeasurementWriteResult.Saved

        assertEquals(original.change.saved.id, replaced.change.replaced!!.id)
        assertEquals(79.0, measurements.getForMetric(metricId).single().value, 0.0)

        measurements.undo(replaced.change)

        val restored = measurements.getForMetric(metricId).single()
        assertEquals(original.change.saved.id, restored.id)
        assertEquals(78.5, restored.value, 0.0)
    }

    @Test
    fun edit_updates_in_place_and_undo_restores_previous_value() = runBlocking {
        val metricId = createMetric()
        val created = measurements.save(input(metricId, value = 78.5), replaceExisting = false)
            as MeasurementWriteResult.Saved

        clock.advance(60_000)
        val edited = measurements.save(
            input(metricId, value = 78.0, existingId = created.change.saved.id),
            replaceExisting = false,
        ) as MeasurementWriteResult.Saved

        assertEquals(created.change.saved.id, edited.change.saved.id)
        assertEquals(78.5, edited.change.previous!!.value, 0.0)
        assertNull(edited.change.replaced)
        assertEquals(78.0, measurements.getForMetric(metricId).single().value, 0.0)

        measurements.undo(edited.change)

        assertEquals(78.5, measurements.getForMetric(metricId).single().value, 0.0)
    }

    @Test
    fun undo_create_removes_the_entry() = runBlocking {
        val metricId = createMetric()
        val saved = measurements.save(input(metricId, value = 78.5), replaceExisting = false)
            as MeasurementWriteResult.Saved

        measurements.undo(saved.change)

        assertTrue(measurements.getForMetric(metricId).isEmpty())
    }

    @Test
    fun delete_and_restore_round_trip() = runBlocking {
        val metricId = createMetric()
        val saved = measurements.save(input(metricId, value = 78.5), replaceExisting = false)
            as MeasurementWriteResult.Saved

        val deleted = measurements.delete(saved.change.saved.id)

        assertEquals(78.5, deleted!!.value, 0.0)
        assertTrue(measurements.getForMetric(metricId).isEmpty())

        measurements.restore(deleted)

        assertEquals(78.5, measurements.getForMetric(metricId).single().value, 0.0)
    }

    @Test
    fun save_rejects_non_finite_and_unknown_metric() = runBlocking {
        val metricId = createMetric()

        assertTrue(
            measurements.save(input(metricId, value = Double.NaN), replaceExisting = false) is
                MeasurementWriteResult.Invalid,
        )
        assertTrue(
            measurements.save(input("missing", value = 1.0), replaceExisting = false) is
                MeasurementWriteResult.NotFound,
        )
    }

    private suspend fun createMetric(): String {
        val result = metrics.create(draft(name = "体重")) as MetricWriteResult.Saved
        return result.metric.id
    }

    private suspend fun activeIds(): List<String> =
        metrics.observeActiveMetrics().first().map { it.id }

    private suspend fun activeOrders(): List<Int> =
        metrics.observeActiveMetrics().first().map { it.sortOrder }

    private fun draft(
        name: String,
        unit: String = "kg",
        dimension: UnitDimension = UnitDimension.MASS,
    ) = MetricDraft(
        name = name,
        unit = unit,
        unitDimension = dimension,
        precision = 1,
        colorArgb = 0xFFC96F4A,
        iconKey = "weight",
    )

    private fun input(
        metricId: String,
        value: Double,
        existingId: String? = null,
    ) = MeasurementInput(
        metricId = metricId,
        date = date,
        value = value,
        existingId = existingId,
    )

    private class FakeClock(private var instant: Instant) : AppClock {
        override fun now(): Instant = instant

        override fun today(): LocalDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()

        fun advance(millis: Long) {
            instant = instant.plusMillis(millis)
        }
    }
}
