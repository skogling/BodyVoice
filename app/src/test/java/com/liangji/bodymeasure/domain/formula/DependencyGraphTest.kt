package com.liangji.bodymeasure.domain.formula

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DependencyGraphTest {
    @Test
    fun acyclic_graph_has_no_cycle() {
        val edges = mapOf("a" to setOf("b", "c"), "b" to setOf("c"), "c" to emptySet())

        assertNull(DependencyGraph.findCycle(edges))
    }

    @Test
    fun detects_self_cycle() {
        assertEquals(listOf("a", "a"), DependencyGraph.findCycle(mapOf("a" to setOf("a"))))
    }

    @Test
    fun reports_cycle_path() {
        val edges = mapOf("a" to setOf("b"), "b" to setOf("c"), "c" to setOf("a"))

        val cycle = DependencyGraph.findCycle(edges)!!

        assertEquals(cycle.first(), cycle.last())
        assertEquals(setOf("a", "b", "c"), cycle.toSet())
    }

    @Test
    fun ignores_repeated_nodes_that_are_not_cycles() {
        val edges = mapOf("a" to setOf("b", "c"), "b" to setOf("d"), "c" to setOf("d"))

        assertNull(DependencyGraph.findCycle(edges))
    }

    @Test
    fun computed_depth_counts_derived_chain() {
        val edges = mapOf(
            "a" to setOf("b"),
            "b" to setOf("c"),
            "c" to setOf("d"),
            "d" to setOf("raw"),
        )
        val derived = setOf("a", "b", "c", "d")

        assertEquals(4, DependencyGraph.derivedDepth("a", derived, edges))
        assertEquals(1, DependencyGraph.derivedDepth("d", derived, edges))
        assertEquals(0, DependencyGraph.derivedDepth("raw", derived, edges))
    }

    @Test
    fun depth_limit_marks_cycles_as_too_deep() {
        val edges = mapOf("a" to setOf("b"), "b" to setOf("a"))
        val derived = setOf("a", "b")

        assertTrue(
            DependencyGraph.derivedDepth("a", derived, edges) > DependencyGraph.MAX_DERIVED_DEPTH,
        )
    }
}
