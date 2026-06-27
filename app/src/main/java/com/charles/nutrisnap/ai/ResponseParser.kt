package com.charles.nutrisnap.ai

import kotlinx.serialization.json.Json

internal data class ParseException(override val message: String) : Exception(message)

internal const val MIN_CONFIDENCE = 0.5f

private val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

/**
 * Extracts the first complete top-level JSON object from a model response by scanning from the first
 * '{' to its matching '}'. This naturally skips any prose preamble or ```json fences without needing
 * to strip lines (stripping the first line corrupts JSON whose opening brace sits on its own line).
 */
internal fun extractJson(raw: String): String? {
    val start = raw.indexOf('{')
    if (start == -1) return null
    var depth = 0
    var inString = false
    var escaped = false
    for (i in start until raw.length) {
        val c = raw[i]
        if (escaped) {
            escaped = false
            continue
        }
        when (c) {
            '\\' -> escaped = true
            '"' -> inString = !inString
            '{' -> if (!inString) depth++
            '}' -> if (!inString) { depth--; if (depth == 0) return raw.substring(start, i + 1) }
        }
    }
    return null
}

internal fun parseFoodEstimate(raw: String): Result<FoodEstimate> {
    val jsonStr = extractJson(raw) ?: return Result.failure(ParseException("No JSON object found"))
    return try {
        val estimate = json.decodeFromString<FoodEstimate>(jsonStr)
        if (estimate.confidence < MIN_CONFIDENCE) {
            Result.failure(ParseException("Confidence ${estimate.confidence} below threshold $MIN_CONFIDENCE"))
        } else {
            Result.success(estimate)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
