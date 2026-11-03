package com.liangji.bodymeasure.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.liangji.bodymeasure.core.common.AppClock
import com.liangji.bodymeasure.data.local.datastore.DeviceIdProvider
import com.liangji.bodymeasure.data.local.db.AppDatabase
import com.liangji.bodymeasure.domain.formula.FormulaErrorCode
import com.liangji.bodymeasure.domain.model.FormulaHistory
import com.liangji.bodymeasure.domain.model.MetricDeleteResult
import com.liangji.bodymeasure.domain.model.MetricDraft
import com.liangji.bodymeasure.domain.model.MetricKind
import com.liangji.bodymeasure.domain.model.MetricWriteResult
import com.liangji.bodymeasure.domain.model.PairingPolicy
import com.liangji.bodymeasure.domain.model.UnitDimension
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [37])
class MetricFormulaRepositoryTest {
    private val clock = FakeClock(Instant.parse("2026-10-30T08:00:00Z"))
    private val deviceId = object : DeviceIdProvider {
        override suspend fun getOrCreate(): String = "test-device"
    }

    private lateinit var database: AppDatabase
    private lateinit var metrics: RoomMetricRepository

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
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun creates_derived_metric_with_canonical_formula_and_edges() = runBlocking {
        val weight = createRaw("体重", "kg", UnitDimension.MASS)
        val height = createRaw("身高", "cm", UnitDimension.LENGTH)

        val result = createDerived("BMI", "[体重] / ([身高] / 100) ^ 2") as MetricWriteResult.Saved

        assertEquals(MetricKind.DERIVED, result.metric.kind)
        assertEquals("{${weight}} / ({${height}} / 100) ^ 2", result.metric.formula)
        assertEquals("[体重] / ([身高] / 100) ^ 2", result.metric.formulaDisplay)
        assertEquals(1, result.metric.formulaVersion)
        assertEquals(
            setOf(weight, height),
            database.metricDependencyDao().dependenciesOf(result.metric.id).toSet(),
        )
        val history = metrics.observeFormulaHistory(result.metric.id).first()
        assertEquals(1, history.size)
        assertEquals(1, history.first().version)
        assertEquals("test-device", history.first().originDeviceId)
    }

    @Test
    fun rejects_unknown_metric_name() = runBlocking {
        val result = createDerived("BMI", "[不存在] + 1")

        val error = (result as MetricWriteResult.InvalidFormula).error
        assertEquals(FormulaErrorCode.UNKNOWN_METRIC, error.code)
    }

    @Test
    fun rejects_unknown_metric_id() = runBlocking {
        val result = createDerived("BMI", "{missing} + 1")

        assertEquals(
            MetricWriteResult.UnknownReference(listOf("missing")),
            result,
        )
    }

    @Test
    fun rejects_self_reference_as_cycle() = runBlocking {
        val id = createDerived("A", "1 + 1").let { (it as MetricWriteResult.Saved).metric.id }

        val result = metrics.update(id, derivedDraft("A", "[A] + 1"))

        assertEquals(MetricWriteResult.CycleDetected(listOf("A", "A")), result)
    }

    @Test
    fun rejects_indirect_cycle() = runBlocking {
        val a = createDerived("A", "1 + 1").let { (it as MetricWriteResult.Saved).metric.id }
        createDerived("B", "[A] + 1")

        val result = metrics.update(a, derivedDraft("A", "[B] + 1"))

        val cycle = (result as MetricWriteResult.CycleDetected).path
        assertEquals(cycle.first(), cycle.last())
        assertEquals(setOf("A", "B"), cycle.toSet())
    }

    @Test
    fun enforces_derived_depth_limit() = runBlocking {
        var previous = "1"
        repeat(5) { index ->
            val result = createDerived("D$index", previous) as MetricWriteResult.Saved
            previous = "[D$index] + 1"
            assertTrue(result.metric.id.isNotBlank())
        }

        val tooDeep = createDerived("D5", previous)

        assertEquals(MetricWriteResult.TooDeep(6), tooDeep)
    }

    @Test
    fun delete_is_blocked_while_referenced() = runBlocking {
        val weight = createRaw("体重", "kg", UnitDimension.MASS)
        val bmi = createDerived("BMI", "[体重] + 1").let { (it as MetricWriteResult.Saved).metric.id }

        assertEquals(
            MetricDeleteResult.ReferencedBy(listOf("BMI")),
            metrics.delete(weight),
        )
        assertTrue(metrics.observeMetric(weight).first() != null)

        assertTrue(metrics.delete(bmi) is MetricDeleteResult.Deleted)
        assertTrue(metrics.delete(weight) is MetricDeleteResult.Deleted)
    }

    @Test
    fun unit_change_is_blocked_while_referenced() = runBlocking {
        val weight = createRaw("体重", "kg", UnitDimension.MASS)
        createDerived("BMI", "[体重] + 1")

        val result = metrics.update(weight, rawDraft("体重", "lb", UnitDimension.MASS))

        assertEquals(MetricWriteResult.ReferencedBy(listOf("BMI")), result)
        assertEquals("kg", metrics.observeMetric(weight).first()!!.unit)
    }

    @Test
    fun formula_change_bumps_version_and_records_history() = runBlocking {
        val id = createDerived("BMI", "1 + 1").let { (it as MetricWriteResult.Saved).metric.id }

        clock.advance(60_000)
        val updated = metrics.update(
            id,
            derivedDraft("BMI", "1 + 2", changeNote = "调整口径"),
        ) as MetricWriteResult.Saved

        assertEquals(2, updated.metric.formulaVersion)
        assertEquals("1 + 2", updated.metric.formula)
        val history = metrics.observeFormulaHistory(id).first()
        assertEquals(listOf(2, 1), history.map(FormulaHistory::version))
        assertEquals("调整口径", history.first().changeNote)

        clock.advance(60_000)
        val renamed = metrics.update(id, derivedDraft("BMI 2", "1 + 2")) as MetricWriteResult.Saved
        assertEquals(2, renamed.metric.formulaVersion)
        assertEquals(2, metrics.observeFormulaHistory(id).first().size)
    }

    @Test
    fun renaming_referenced_metric_refreshes_dependent_display() = runBlocking {
        val weight = createRaw("体重", "kg", UnitDimension.MASS)
        val bmi = createDerived("BMI", "[体重] + 1").let { (it as MetricWriteResult.Saved).metric.id }

        clock.advance(60_000)
        val result = metrics.update(weight, rawDraft("重量", "kg", UnitDimension.MASS))

        assertTrue(result is MetricWriteResult.Saved)
        val dependent = metrics.observeMetric(bmi).first()!!
        assertEquals("[重量] + 1", dependent.formulaDisplay)
        assertEquals("{${weight}} + 1", dependent.formula)
    }

    @Test
    fun kind_cannot_change_after_creation() = runBlocking {
        val weight = createRaw("体重", "kg", UnitDimension.MASS)

        val result = metrics.update(weight, derivedDraft("体重", "1 + 1"))

        assertEquals(MetricWriteResult.KindLocked, result)
    }

    @Test
    fun derived_draft_stores_pairing_policy() = runBlocking {
        val weight = createRaw("体重", "kg", UnitDimension.MASS)

        val result = metrics.create(
            derivedDraft(
                name = "BMI",
                formula = "[体重] + 1",
                pairingPolicy = PairingPolicy.CARRY_FORWARD,
                windowDays = 5,
            ),
        ) as MetricWriteResult.Saved

        assertEquals(PairingPolicy.CARRY_FORWARD, result.metric.pairingPolicy)
        assertEquals(5, result.metric.pairingWindowDays)
    }

    @Test
    fun rejects_blank_formula_for_derived_metric() = runBlocking {
        val result = createDerived("BMI", "   ")

        assertEquals(
            FormulaErrorCode.EMPTY,
            (result as MetricWriteResult.InvalidFormula).error.code,
        )
    }

    private suspend fun createRaw(name: String, unit: String, dimension: UnitDimension): String =
        (metrics.create(rawDraft(name, unit, dimension)) as MetricWriteResult.Saved).metric.id

    private suspend fun createDerived(name: String, formula: String): MetricWriteResult =
        metrics.create(derivedDraft(name, formula))

    private fun rawDraft(
        name: String,
        unit: String,
        dimension: UnitDimension,
    ) = MetricDraft(
        name = name,
        unit = unit,
        unitDimension = dimension,
        precision = 1,
        colorArgb = 0xFFC96F4A,
        iconKey = "generic",
    )

    private fun derivedDraft(
        name: String,
        formula: String,
        changeNote: String? = null,
        pairingPolicy: PairingPolicy = PairingPolicy.NEAREST,
        windowDays: Int = 2,
    ) = MetricDraft(
        name = name,
        unit = "",
        unitDimension = UnitDimension.COMPOSITE,
        precision = 1,
        colorArgb = 0xFF3F7F72,
        iconKey = "generic",
        kind = MetricKind.DERIVED,
        formula = formula,
        pairingPolicy = pairingPolicy,
        pairingWindowDays = windowDays,
        formulaChangeNote = changeNote,
    )

    private class FakeClock(private var instant: Instant) : AppClock {
        override fun now(): Instant = instant

        override fun today(): LocalDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()

        fun advance(millis: Long) {
            instant = instant.plusMillis(millis)
        }
    }
}
