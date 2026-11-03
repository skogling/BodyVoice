package com.liangji.bodymeasure.domain.formula

object DependencyGraph {
    const val MAX_DERIVED_DEPTH = 5

    fun findCycle(edges: Map<String, Set<String>>): List<String>? {
        val visiting = HashSet<String>()
        val visited = HashSet<String>()
        val stack = ArrayList<String>()

        fun visit(node: String): List<String>? {
            if (node in visited) return null
            if (node in visiting) {
                val start = stack.indexOf(node)
                return stack.drop(start) + node
            }
            visiting += node
            stack += node
            for (next in edges[node].orEmpty()) {
                visit(next)?.let { return it }
            }
            stack.removeAt(stack.lastIndex)
            visiting -= node
            visited += node
            return null
        }

        for (node in edges.keys) {
            visit(node)?.let { return it }
        }
        return null
    }

    fun derivedDepth(
        metricId: String,
        derivedIds: Set<String>,
        edges: Map<String, Set<String>>,
    ): Int {
        if (metricId !in derivedIds) return 0
        val memo = HashMap<String, Int>()

        fun depth(node: String): Int {
            if (node !in derivedIds) return 0
            memo[node]?.let { return it }
            memo[node] = MAX_DERIVED_DEPTH + 1
            val deepestChild = edges[node].orEmpty()
                .filter { it in derivedIds }
                .maxOfOrNull { depth(it) } ?: 0
            return (deepestChild + 1).also { memo[node] = it }
        }

        return depth(metricId)
    }
}
