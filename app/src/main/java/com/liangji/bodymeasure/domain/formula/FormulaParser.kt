package com.liangji.bodymeasure.domain.formula

object FormulaParser {
    const val MAX_LENGTH = 512
    const val MAX_NODES = 256
    const val MAX_REFERENCES = 16
    const val MAX_DEPTH = 32

    fun parse(
        text: String,
        resolveMetricName: ((String) -> String?)? = null,
    ): FormulaParseResult {
        if (text.isBlank()) {
            return invalid(FormulaErrorCode.EMPTY, 0, "公式不能为空")
        }
        if (text.length > MAX_LENGTH) {
            return invalid(FormulaErrorCode.TOO_LONG, MAX_LENGTH, "公式不能超过 $MAX_LENGTH 个字符")
        }
        return try {
            val parser = Parser(text, resolveMetricName)
            FormulaParseResult.Valid(
                expression = parser.parse(),
                references = parser.references.values.toList(),
            )
        } catch (failure: FormulaFailure) {
            FormulaParseResult.Invalid(failure.error)
        }
    }

    private fun invalid(code: FormulaErrorCode, position: Int, message: String) =
        FormulaParseResult.Invalid(FormulaError(code, position, message))

    private class FormulaFailure(val error: FormulaError) : RuntimeException(error.message)

    private sealed interface Token {
        val position: Int

        data class Number(val value: Double, override val position: Int) : Token

        data class Reference(val metricId: String, override val position: Int) : Token

        data class Identifier(val name: String, override val position: Int) : Token

        data class Symbol(val symbol: Char, override val position: Int) : Token

        data class End(override val position: Int) : Token
    }

    private class Parser(
        private val text: String,
        private val resolveMetricName: ((String) -> String?)?,
    ) {
        val references = LinkedHashMap<String, FormulaReference>()
        private var index = 0
        private var nodeCount = 0

        fun parse(): FormulaExpr {
            val expression = expression(depth = 0)
            val token = nextToken()
            if (token !is Token.End) {
                throw failure(FormulaErrorCode.UNEXPECTED_TOKEN, token.position, "这里不应该出现${describe(token)}")
            }
            return expression
        }

        private fun expression(depth: Int): FormulaExpr {
            checkDepth(depth)
            var left = term(depth)
            while (true) {
                val token = peekToken()
                val operator = when {
                    token is Token.Symbol && token.symbol == '+' -> Operator.ADD
                    token is Token.Symbol && token.symbol == '-' -> Operator.SUBTRACT
                    else -> null
                } ?: return left
                nextToken()
                val right = term(depth + 1)
                left = node(FormulaExpr.Binary(operator, left, right), token.position)
            }
        }

        private fun term(depth: Int): FormulaExpr {
            checkDepth(depth)
            var left = unary(depth)
            while (true) {
                val token = peekToken()
                val operator = when {
                    token is Token.Symbol && token.symbol == '*' -> Operator.MULTIPLY
                    token is Token.Symbol && token.symbol == '/' -> Operator.DIVIDE
                    else -> null
                } ?: return left
                nextToken()
                val right = unary(depth + 1)
                left = node(FormulaExpr.Binary(operator, left, right), token.position)
            }
        }

        private fun unary(depth: Int): FormulaExpr {
            checkDepth(depth)
            val token = peekToken()
            if (token is Token.Symbol && (token.symbol == '+' || token.symbol == '-')) {
                nextToken()
                val operand = unary(depth + 1)
                return if (token.symbol == '-') {
                    node(FormulaExpr.Negate(operand), token.position)
                } else {
                    operand
                }
            }
            return power(depth)
        }

        private fun power(depth: Int): FormulaExpr {
            checkDepth(depth)
            val left = primary(depth)
            val token = peekToken()
            if (token is Token.Symbol && token.symbol == '^') {
                nextToken()
                val right = unary(depth + 1)
                return node(FormulaExpr.Binary(Operator.POWER, left, right), token.position)
            }
            return left
        }

        private fun primary(depth: Int): FormulaExpr {
            checkDepth(depth)
            val token = nextToken()
            return when (token) {
                is Token.Number -> node(FormulaExpr.Number(token.value), token.position)
                is Token.Reference -> {
                    references.putIfAbsent(
                        token.metricId,
                        FormulaReference(token.metricId, token.position),
                    )
                    if (references.size > MAX_REFERENCES) {
                        throw failure(
                            FormulaErrorCode.TOO_MANY_REFERENCES,
                            token.position,
                            "最多引用 $MAX_REFERENCES 个指标",
                        )
                    }
                    node(FormulaExpr.Reference(token.metricId), token.position)
                }

                is Token.Identifier -> call(token, depth)
                is Token.Symbol -> when (token.symbol) {
                    '(' -> {
                        val inner = expression(depth + 1)
                        expectClosing(token.position)
                        inner
                    }

                    else -> throw failure(
                        FormulaErrorCode.MISSING_OPERAND,
                        token.position,
                        "缺少数字或指标",
                    )
                }

                is Token.End -> throw failure(
                    FormulaErrorCode.MISSING_OPERAND,
                    token.position,
                    "缺少数字或指标",
                )
            }
        }

        private fun call(token: Token.Identifier, depth: Int): FormulaExpr {
            val name = token.name.lowercase()
            val arity = FUNCTION_ARITY[name]
                ?: throw failure(
                    FormulaErrorCode.UNKNOWN_FUNCTION,
                    token.position,
                    "不支持函数「${token.name}」",
                )
            val open = nextToken()
            if (open !is Token.Symbol || open.symbol != '(') {
                throw failure(
                    FormulaErrorCode.UNEXPECTED_TOKEN,
                    token.position,
                    "函数「${token.name}」后面需要括号",
                )
            }
            val arguments = mutableListOf<FormulaExpr>()
            val close = peekToken()
            if (close is Token.Symbol && close.symbol == ')') {
                nextToken()
            } else {
                while (true) {
                    arguments += expression(depth + 1)
                    val separator = nextToken()
                    if (separator is Token.Symbol && separator.symbol == ',') continue
                    if (separator is Token.Symbol && separator.symbol == ')') break
                    throw failure(
                        FormulaErrorCode.MISSING_RIGHT_PAREN,
                        separator.position,
                        "缺少右括号",
                    )
                }
            }
            if (arguments.size !in arity) {
                throw failure(
                    FormulaErrorCode.WRONG_ARGUMENT_COUNT,
                    token.position,
                    "函数「$name」需要${arityText(arity)}参数，实际给了 ${arguments.size} 个",
                )
            }
            return node(FormulaExpr.Call(name, arguments), token.position)
        }

        private fun expectClosing(openPosition: Int) {
            val token = nextToken()
            if (token !is Token.Symbol || token.symbol != ')') {
                throw failure(
                    FormulaErrorCode.MISSING_RIGHT_PAREN,
                    if (token is Token.End) text.length else token.position,
                    "缺少右括号",
                )
            }
        }

        private fun peekToken(): Token {
            val saved = index
            val token = readToken()
            index = saved
            return token
        }

        private fun nextToken(): Token = readToken()

        private fun readToken(): Token {
            while (index < text.length && text[index].isWhitespace()) index++
            if (index >= text.length) return Token.End(text.length)
            val start = index
            val ch = text[index]
            return when {
                ch.isDigit() || (ch == '.' && index + 1 < text.length && text[index + 1].isDigit()) ->
                    readNumber(start)

                ch == '{' -> readReference(start, closing = '}', displayName = false)
                ch == '[' -> readReference(start, closing = ']', displayName = true)
                ch.isLetter() || ch == '_' -> readIdentifier(start)
                ch in "+-*/^()," -> {
                    index++
                    Token.Symbol(ch, start)
                }

                else -> throw failure(
                    FormulaErrorCode.UNEXPECTED_CHAR,
                    start,
                    "无法识别的字符「$ch」",
                )
            }
        }

        private fun readNumber(start: Int): Token.Number {
            var hasDigit = false
            while (index < text.length && text[index].isDigit()) {
                index++
                hasDigit = true
            }
            if (index < text.length && text[index] == '.') {
                index++
                while (index < text.length && text[index].isDigit()) {
                    index++
                    hasDigit = true
                }
            }
            if (!hasDigit) {
                throw failure(FormulaErrorCode.INVALID_NUMBER, start, "数字格式不正确")
            }
            val value = text.substring(start, index).toDoubleOrNull()
            if (value == null || !value.isFinite()) {
                throw failure(FormulaErrorCode.INVALID_NUMBER, start, "数字格式不正确")
            }
            return Token.Number(value, start)
        }

        private fun readReference(start: Int, closing: Char, displayName: Boolean): Token.Reference {
            index++
            val contentStart = index
            while (index < text.length && text[index] != closing) index++
            if (index >= text.length) {
                throw failure(
                    FormulaErrorCode.MISSING_BRACKET,
                    start,
                    if (closing == '}') "引用缺少右花括号" else "引用缺少右方括号",
                )
            }
            val content = text.substring(contentStart, index).trim()
            index++
            if (content.isEmpty()) {
                throw failure(FormulaErrorCode.UNKNOWN_METRIC, start, "引用的指标不能为空")
            }
            val metricId = if (displayName) {
                resolveMetricName?.invoke(content)
                    ?: throw failure(
                        FormulaErrorCode.UNKNOWN_METRIC,
                        start,
                        "找不到指标「$content」",
                    )
            } else {
                content
            }
            return Token.Reference(metricId, start)
        }

        private fun readIdentifier(start: Int): Token.Identifier {
            while (index < text.length && (text[index].isLetterOrDigit() || text[index] == '_')) {
                index++
            }
            return Token.Identifier(text.substring(start, index), start)
        }

        private fun node(expression: FormulaExpr, position: Int): FormulaExpr {
            nodeCount++
            if (nodeCount > MAX_NODES) {
                throw failure(
                    FormulaErrorCode.TOO_MANY_NODES,
                    position,
                    "公式过于复杂，请拆成多个指标",
                )
            }
            return expression
        }

        private fun checkDepth(depth: Int) {
            if (depth > MAX_DEPTH) {
                throw failure(
                    FormulaErrorCode.NESTING_TOO_DEEP,
                    index,
                    "公式嵌套过深",
                )
            }
        }

        private fun describe(token: Token): String = when (token) {
            is Token.Number -> "数字"
            is Token.Reference -> "指标引用"
            is Token.Identifier -> "「${token.name}」"
            is Token.Symbol -> "「${token.symbol}」"
            is Token.End -> "结尾"
        }

        private fun failure(code: FormulaErrorCode, position: Int, message: String) =
            FormulaFailure(FormulaError(code, position, message))
    }

    private fun arityText(arity: IntRange): String = when {
        arity.first == arity.last -> " ${arity.first} 个"
        arity.last == Int.MAX_VALUE -> "至少 ${arity.first} 个"
        else -> " ${arity.first} 到 ${arity.last} 个"
    }

    private val FUNCTION_ARITY: Map<String, IntRange> = mapOf(
        "abs" to 1..1,
        "sqrt" to 1..1,
        "pow" to 2..2,
        "round" to 1..2,
        "min" to 2..8,
        "max" to 2..8,
        "clamp" to 3..3,
    )
}
