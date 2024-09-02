package io.sn.aetherium.implementations.shards

import com.tairitsu.compose.arcaea.ChartConfiguration
import com.tairitsu.compose.arcaea.generateString
import com.tairitsu.compose.arcaea.pos
import com.tairitsu.compose.arcaea.timing
import io.sn.aetherium.objects.ControllerBrand
import io.sn.aetherium.objects.ShardDigestion
import io.sn.aetherium.objects.TestOnlyApi
import io.sn.aetherium.utils.file
import kotlin.test.Test

class ArcTapJumpShardTest {

    @OptIn(TestOnlyApi::class)
    @Test
    fun `test arctapjumpshard`() {
        val shard = ArcTapJumpShard()
        shard.testInit()

        shard.feed(
            ControllerBrand("ArcadePlus", "0.5.3"), mutableMapOf(
                "globalOffset" to ShardDigestion.Union(260279L),
                "fps" to ShardDigestion.Union(240),
                "bpm" to ShardDigestion.Union(60.0),
                "timingStart" to ShardDigestion.Union.Restriction.fromTiming(183333),
                "timingEnd" to ShardDigestion.Union.Restriction.fromTiming(184285),
                "positionStart" to ShardDigestion.Union.Restriction.fromPosition(0.25 pos 0.00),
                "positionEnd" to ShardDigestion.Union.Restriction.fromPosition(0 pos 1),
                "showFirstFrame" to ShardDigestion.Union(true),
                "control" to ShardDigestion.Union("0:2000")
            )
        )

        val jump1 = shard.generate(ChartConfiguration(-660, mutableListOf())) {
            timing(0, 126, 4)
        }

        shard.feed(
            ControllerBrand("ArcadePlus", "0.5.3"), mutableMapOf(
                "globalOffset" to ShardDigestion.Union(260279L),
                "fps" to ShardDigestion.Union(120),
                "bpm" to ShardDigestion.Union(60.0),
                "timingStart" to ShardDigestion.Union.Restriction.fromTiming(183333),
                "timingEnd" to ShardDigestion.Union.Restriction.fromTiming(184285),
                "positionStart" to ShardDigestion.Union.Restriction.fromPosition(0.75 pos 0.00),
                "positionEnd" to ShardDigestion.Union.Restriction.fromPosition(1 pos 1),
                "showFirstFrame" to ShardDigestion.Union(true),
                "control" to ShardDigestion.Union("0:2000")
            )
        )

        // only in testing units there is a need to append chart configuration and main timing command
        val jump2 = shard.generate(ChartConfiguration(-660, mutableListOf())) {
            timing(0, 126, 4)
        }.chart

        // append `jump2` to `jump1`
        jump1.chart.subTiming.putAll(jump2.subTiming)

        // directly save to file
        /*
        file(".", "result", "2.aff").let {
            if (!it.exists()) file(".", "result").mkdirs()
            it.writeText(jump1.generateString())
        }
         */

        // or print to console
        println(jump1.generateString())
    }
}
