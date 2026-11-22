package com.charles.nutrisnap.data.report

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class AiReportPrivacyTest {
    @Test fun `all AI surfaces serialize only fixed metadata`() {
        val json = Json
        for (surface in AiReportSurface.entries) {
            for (category in AiReportCategory.entries) {
                val wire = json.encodeToJsonElement(AiReportRequest.serializer(), buildAiReport(surface, category, "test"))
                    .jsonObject
                assertEquals(setOf("category", "description", "appVersion"), wire.keys)
                assertEquals(surface.label, wire.getValue("description").jsonPrimitive.content)
                assertEquals(category.name.lowercase(), wire.getValue("category").jsonPrimitive.content)
                assertEquals("test", wire.getValue("appVersion").jsonPrimitive.content)
            }
        }
    }
}
