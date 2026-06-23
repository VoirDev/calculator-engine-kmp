package dev.voir.calculator.engine

internal fun stripTrailingZeros(text: String): String {
    if ('.' !in text) return text
    var end = text.length
    // Walk backward over fractional zeroes, then remove a dangling decimal point.
    while (end > 0 && text[end - 1] == '0') end--
    if (end > 0 && text[end - 1] == '.') end--
    return when (val trimmed = text.substring(0, end)) {
        "", "-" -> "0"
        else -> trimmed
    }
}
