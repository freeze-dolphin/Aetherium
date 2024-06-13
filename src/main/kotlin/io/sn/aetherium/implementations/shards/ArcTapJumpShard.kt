package io.sn.aetherium.implementations.shards

import com.tairitsu.compose.arcaea.Difficulty
import com.tairitsu.compose.arcaea.LocalizedString
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
    override val name: LocalizedString
        get() = LocalizedString("Arctap Jump") {
            zhHans = "Arctap 跳跃"
        }

    override val digestionInfo: ShardDigestionArgsInfo = ShardDigestionArgsInfo {
        addInfo(
            "globalOffset",
            ShardDigestionArgsInfo.Item.Type.LONG,
            ShardDigestion.Union.Restriction.songGlobalOffsetPlaceholder,
            "Anim: Global Offset"
        ) {
            zhHans = "帧动画: 歌曲长度"
        }
        addInfo("fps", ShardDigestionArgsInfo.Item.Type.INT, ShardDigestion.Union(60), "Anim: FPS") {
            zhHans = "帧动画: 质量 (FPS)"
        }
        addInfo("bpm", ShardDigestionArgsInfo.Item.Type.DOUBLE, ShardDigestion.Union.Restriction.songBpmPlaceholder, "Anim: BPM") {
            zhHans = "帧动画: BPM"
        }
        addInfo(
            "timingStart",
            ShardDigestionArgsInfo.Item.Type.TIMING,
            ShardDigestion.Union.Restriction.editorCurrentTiming,
            "Start Timing"
        ) {
            zhHans = "起始时间"
        }
        addInfo(
            "timingEnd",
            ShardDigestionArgsInfo.Item.Type.TIMING,
            ShardDigestion.Union.Restriction.editorDelayTiming(1000L),
            "End Timing"
        ) {
            zhHans = "结束时间"
        }
        addInfo("positionStart", ShardDigestionArgsInfo.Item.Type.POSITION, "Start Position") {
            zhHans = "起始位置"
        }
        addInfo("positionEnd", ShardDigestionArgsInfo.Item.Type.POSITION, "End Position") {
            zhHans = "结束位置"
        }
        addInfo("showFirstFrame", ShardDigestionArgsInfo.Item.Type.BOOLEAN, ShardDigestion.Union(false), "Show first frame?") {
            zhHans = "是否显示第一帧"
        }
        addInfo("control", ShardDigestionArgsInfo.Item.Type.STRING, ShardDigestion.Union("0:2000"), "Gravity Control") {
            zhHans = "重力参数"
        }
    }

    override fun generator(): Difficulty.() -> Unit = {
        val timingList = arrayOf(digestTiming("timingStart"), digestTiming("timingEnd"), -1)
        val posStart = digestPosition("positionStart")
        val posEnd = digestPosition("positionEnd")

        val showFirstFrame = digestBoolean("showFirstFrame")

        val positionXList = arrayOf(posStart.x, posEnd.x, 0.0)
        val positionYList = arrayOf(posStart.y, posEnd.y, 0.0)
        val control = digestString("control")

        val animBasicCfg = AnimationBasicConfigurtion(
            digestInt("fps"),
            digestDouble("bpm"),
            digestLong("globalOffset")
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
                    positionXList[it] pos positionYList[it],
                    positionXList[it] pos positionYList[it],
                    linear
                ),
                0,
                showFirstFrame,
                noteJumpGetFrame,
                positionXList[it + 1] pos positionYList[it + 1]
            )
        }
    }

}