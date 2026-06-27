package com.charles.nutrisnap.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class ResponseParserTest {

    @Test
    fun `clean JSON parses correctly`() {
        val json = """{"name":"Apple","portionDescription":"1 medium","grams":182,"kcal":95,"proteinG":0,"carbsG":25,"fatG":0,"confidence":0.95}"""
        val result = parseFoodEstimate(json)
        assertTrue(result.isSuccess)
        val estimate = result.getOrThrow()
        assertEquals("Apple", estimate.name)
        assertEquals(95, estimate.kcal)
        assertEquals(0.95f, estimate.confidence)
    }

    @Test
    fun `fenced JSON with markdown parses correctly`() {
        val raw = """Here is the estimate:
```json
{"name":"Banana","portionDescription":"1 medium","grams":118,"kcal":105,"proteinG":1,"carbsG":27,"fatG":0,"confidence":0.92}
```
That's all."""
        val result = parseFoodEstimate(raw)
        assertTrue(result.isSuccess)
        assertEquals("Banana", result.getOrThrow().name)
        assertEquals(105, result.getOrThrow().kcal)
    }

    @Test
    fun `plain code fence without json tag parses correctly`() {
        val raw = """```
{"name":"Toast","portionDescription":"1 slice","grams":30,"kcal":80,"proteinG":3,"carbsG":15,"fatG":1,"confidence":0.85}
```"""
        val result = parseFoodEstimate(raw)
        assertTrue(result.isSuccess)
        assertEquals("Toast", result.getOrThrow().name)
    }

    @Test
    fun `garbled preamble with embedded JSON extracts correctly`() {
        val raw = """I think this is a picture of some food. Let me analyze it.
Based on my analysis: {"name":"Chicken Salad","portionDescription":"1 bowl","grams":300,"kcal":420,"proteinG":35,"carbsG":10,"fatG":28,"confidence":0.88}
Hope that helps!"""
        val result = parseFoodEstimate(raw)
        assertTrue(result.isSuccess)
        assertEquals("Chicken Salad", result.getOrThrow().name)
        assertEquals(420, result.getOrThrow().kcal)
    }

    @Test
    fun `missing fields use defaults via coerce`() {
        val json = """{"name":"Egg","kcal":78,"confidence":0.8}"""
        val result = parseFoodEstimate(json)
        assertTrue(result.isSuccess)
        val estimate = result.getOrThrow()
        assertEquals("Egg", estimate.name)
        assertEquals(78, estimate.kcal)
        assertEquals(0, estimate.proteinG)
        assertEquals(0, estimate.fatG)
        assertEquals(0, estimate.carbsG)
        assertEquals(0, estimate.grams)
        assertEquals("", estimate.portionDescription)
    }

    @Test
    fun `low confidence returns failure`() {
        val json = """{"name":"Blurry blob","kcal":200,"confidence":0.3}"""
        val result = parseFoodEstimate(json)
        assertTrue(result.isFailure)
    }

    @Test
    fun `confidence exactly at threshold returns success`() {
        val json = """{"name":"Edge case","kcal":100,"confidence":0.5}"""
        val result = parseFoodEstimate(json)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `confidence just below threshold returns failure`() {
        val json = """{"name":"Low confidence","kcal":100,"confidence":0.49}"""
        val result = parseFoodEstimate(json)
        assertTrue(result.isFailure)
    }

    @Test
    fun `multi-item meal with items array parses correctly`() {
        val json = """{
            "name":"Bento box","portionDescription":"1 box","grams":500,"kcal":750,"proteinG":30,"carbsG":80,"fatG":35,"confidence":0.91,
            "items":[
                {"name":"Rice","portionDescription":"1 cup","grams":200,"kcal":240,"proteinG":4,"carbsG":50,"fatG":1},
                {"name":"Salmon","portionDescription":"1 fillet","grams":150,"kcal":280,"proteinG":24,"carbsG":0,"fatG":18},
                {"name":"Edamame","portionDescription":"1/2 cup","grams":75,"kcal":120,"proteinG":10,"carbsG":10,"fatG":5}
            ]
        }"""
        val result = parseFoodEstimate(json)
        assertTrue(result.isSuccess)
        val estimate = result.getOrThrow()
        assertEquals("Bento box", estimate.name)
        assertEquals(750, estimate.kcal)
        assertEquals(3, estimate.items.size)
        assertEquals("Rice", estimate.items[0].name)
        assertEquals("Salmon", estimate.items[1].name)
    }

    @Test
    fun `no JSON in input returns failure`() {
        val raw = "This is just some text without any JSON structure at all."
        val result = parseFoodEstimate(raw)
        assertTrue(result.isFailure)
    }

    @Test
    fun `extra fields are ignored`() {
        val json = """{"name":"Smoothie","kcal":250,"confidence":0.82,"extra_field":"ignored","nested":{"should":"be_ignored"}}"""
        val result = parseFoodEstimate(json)
        assertTrue(result.isSuccess)
        assertEquals("Smoothie", result.getOrThrow().name)
        assertEquals(250, result.getOrThrow().kcal)
    }

    @Test
    fun `extractJson strips leading prose`() {
        val raw = "Here is my analysis\n{\"name\":\"Test\",\"kcal\":100,\"confidence\":0.9}"
        val extracted = extractJson(raw)
        assertEquals("""{"name":"Test","kcal":100,"confidence":0.9}""", extracted)
    }

    @Test
    fun `extractJson handles nested braces`() {
        val raw = """{"name":"Nested","kcal":100,"items":[{"name":"A"},{"name":"B"}],"confidence":0.9}"""
        val extracted = extractJson(raw)
        assertEquals(raw, extracted)
    }

    @Test
    fun `extractJson returns null when no JSON found`() {
        val raw = "Just plain text with {unmatched brace"
        assertNull(extractJson(raw))
    }
}
