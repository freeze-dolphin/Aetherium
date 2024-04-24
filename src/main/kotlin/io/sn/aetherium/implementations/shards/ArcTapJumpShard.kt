package io.sn.aetherium.implementations.shards

import com.tairitsu.compose.arcaea.Difficulty
import com.tairitsu.compose.arcaea.pos
import io.sn.aetherium.implementations.adjustZoomingConst
import io.sn.aetherium.implementations.noteJumpGetFrame
import io.sn.aetherium.objects.*
import io.sn.aetherium.objects.animcfg.AnimationBasicConfigurtion
import io.sn.aetherium.utils.addAnimation
import io.sn.aetherium.utils.linear

@ShardInfo("arctapjump")
class ArcTapJumpShard : AetheriumShard() {

    override val isInternal: Boolean
        get() = true

    override val digestionInfo: ShardDigestionArgsInfo
        get() = ShardDigestionArgsInfo {
            addInfo("globalOffset", ShardDigestionArgsInfo.Item.Type.LONG)
            addInfo("fps", ShardDigestionArgsInfo.Item.Type.INT)
            addInfo("bpm", ShardDigestionArgsInfo.Item.Type.DOUBLE)
            addInfo("timingList", ShardDigestionArgsInfo.Item.Type.LONG_LIST)
            addInfo("positionList", ShardDigestionArgsInfo.Item.Type.DOUBLE_LIST)
            addInfo("control", ShardDigestionArgsInfo.Item.Type.STRING)
        }

    override fun generator(): Difficulty.() -> Unit = {
        val globalOffset = digestLong("globalOffset")
        val timingList: List<Long> = digestLongList("timingList")
        val positionList: List<Double> = digestDoubleList("positionList")
        val control = digestString("control")

        val animBasicCfg = AnimationBasicConfigurtion(
            digestInt("fps"),
            digestDouble("bpm"),
            globalOffset + 1
        )

        val controlMap = mutableMapOf<Int, Long>()
        control.split(";").forEach {
            val (at, zoom) = it.split(":")
            controlMap[at.toInt()] = zoom.toLong()
        }

        repeat(timingList.size - 1) {
            if (timingList[it + 1] == -1L || timingList[it] == -1L) {
                return@repeat
            }

            if (controlMap.containsKey(it)) {
                adjustZoomingConst(controlMap[it]!!)
            }

            addAnimation(
                animBasicCfg,
                timingList[it],
                timingList[it + 1] - timingList[it],
                Triple(1.0, 1.0, linear), // not used
                Triple(
                    positionList[it] pos 0,
                    positionList[it] pos 0,
                    linear
                ),
                0,
                noteJumpGetFrame,
                positionList[it + 1] pos 0
            )
        }
    }

}