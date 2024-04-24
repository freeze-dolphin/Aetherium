package org.example.aetheriumshard

import com.tairitsu.compose.arcaea.Difficulty
import com.tairitsu.compose.arcaea.pos
import io.sn.aetherium.objects.*
import io.sn.aetherium.utils.quickArctap
import kotlin.random.Random

@ShardInfo("example")
class ExampleShard : AetheriumShard() {

    override val digestionInfo: ShardDigestionArgsInfo = ShardDigestionArgsInfo {
        addInfo("timingList", ShardDigestionArgsInfo.Item.Type.LONG_ARRAY)
    }

    override fun onGenesis() {
        // Being called when loading this plugin

        // You don't need to call `register` unless you set `manualLoad` to true in @ShardInfo
        // register("example", ExampleShard::class, digestionInfo())
    }

    override fun generator(): Difficulty.() -> Unit = {
        //  Being called when generating aff

        val timingList = digestLongArray("timingList")

        timingList.forEach {
            quickArctap(it, Random.nextDouble(-0.5, 1.5) pos 0.5)
        }
    }


}