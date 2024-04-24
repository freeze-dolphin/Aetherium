package io.sn.aetherium.implementations.shards

import com.tairitsu.compose.arcaea.generateString
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

        val args = mutableMapOf(
            // @formatter:off
            "globalOffset" to ShardDigestion.Union(260279),
            "fps" to ShardDigestion.Union(240),
            "bpm" to ShardDigestion.Union(126.0),
            "timingList" to ShardDigestion.Union(
                arrayOf<Long>(78095, 78571, 79047, 79523, 80000, 80476, 80952, 81428, 81904, 82380, 82857, 83333, 83809, 84285, 84761, 85238, 85714, 85952, 86190, 86428, 86666, 86904, 87142, 87380, 87619, 87857, 88095, 88333, 88571, 88809, 89047, 89285)
            ),
            "positionList" to ShardDigestion.Union(
                arrayOf(-0.25, 0.25, 0.75, 1.25, 1.25, 0.75, 0.25, -0.25, 0.75, 0.25, 1.25, -0.25, 0.75, 0.25, 1.25, -0.25, 0.75, 0.75, 0.25, 0.25, 1.00, 1.00, 0.0, 0.0, 1.25, 1.25, -0.25, -0.25, 1.5, 1.5, -0.5, -0.5)
            ),
            "control" to ShardDigestion.Union("0:2000;16:600")
            // @formatter:on
        )
        shard.feed(ControllerBrand("ArcadePlus", "0.5.3"), args)

        // print generated aff without serialization
        val result = shard.generate().generateString(true)
        println(result)

        // or directly save to file
        file(".", "result", "2.aff").let {
            if (!it.exists()) file(".", "result").mkdirs()
            it.writeText(result)
        }
    }
}
