package com.liangji.bodymeasure.domain.formula

sealed interface FormulaExpr {
    data class Number(val value: Double) : FormulaExpr

    data class Reference(val metricId: String) : FormulaExpr

    data class Negate(val operand: FormulaExpr) : FormulaExpr

    data class Binary(
        val operator: Operator,
        val left: FormulaExpr,
        val right: FormulaExpr,
    ) : FormulaExpr

    data class Call(val name: String, val arguments: List<FormulaExpr>) : FormulaExpr
}

enum class Operator(val symbol: Char) {
    ADD('+'),
    SUBTRACT('-'),
    MULTIPLY('*'),
    DIVIDE('/'),
    POWER('^'),
}

data class FormulaReference(
    val metricId: String,
    val position: Int,
)

enum class FormulaErrorCode {
    EMPTY,
    TOO_LONG,
    UNEXPECTED_CHAR,
    UNEXPECTED_TOKEN,
    MISSING_OPERAND,
    MISSING_RIGHT_PAREN,
    MISSING_BRACKET,
    INVALID_NUMBER,
    UNKNOWN_FUNCTION,
    WRONG_ARGUMENT_COUNT,
    UNKNOWN_METRIC,
    TOO_MANY_NODES,
    TOO_MANY_REFERENCES,
    NESTING_TOO_DEEP,
}

data class FormulaError(
    val code: FormulaErrorCode,
    val position: Int,
    val message: String,
) {
    fun displayMessage(): String = "第 ${position + 1} 位：$message"
}

sealed interface FormulaParseResult {
    data class Valid(
        val expression: FormulaExpr,
        val references: List<FormulaReference>,
    ) : FormulaParseResult

    data class Invalid(val error: FormulaError) : FormulaParseResult
}
