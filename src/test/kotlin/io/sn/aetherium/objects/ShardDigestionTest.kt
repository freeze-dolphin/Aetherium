package io.sn.aetherium.objects

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class ShardDigestionTest {

    @Test
    fun testSerailization() {
        val dig = ShardDigestion("test", mutableMapOf("a" to "b"))
        println(Json.encodeToString(dig))
    }

}