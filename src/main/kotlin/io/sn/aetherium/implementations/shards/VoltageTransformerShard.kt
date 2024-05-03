package io.sn.aetherium.implementations.shards

import com.tairitsu.compose.arcaea.*
import io.sn.aetherium.objects.AetheriumShard
import io.sn.aetherium.objects.ShardDigestionArgsInfo
import io.sn.aetherium.objects.ShardInfo
import io.sn.aetherium.utils.offsetWith

@ShardInfo("voltagetransformer")
class VoltageTransformerShard : AetheriumShard() {

    override val isInternal: Boolean
        get() = true

    override val name: LocalizedString
        get() = LocalizedString("Voltage Transformer") {
            zhHans = "变压器"
        }

    override val digestionInfo: ShardDigestionArgsInfo = ShardDigestionArgsInfo {
        addInfo("chart", ShardDigestionArgsInfo.Item.Type.CHART,
            LocalizedString("Chart Objects") {
                zhHans = "物件"
            })
    }

    override fun generator(): Difficulty.() -> Unit = {
        val chartArg = digestChart("chart")

        val amplifier = 0.17 / 237

        /* Chart configuration and main timing command should be contained in testing units only
        this.chart.configuration.sync(chartArg.configuration)
        chartArg.mainTiming.getTimings()[0].let {
            timing(it.offset, it.bpm, it.beats)
        }
         */

        chartArg.mainTiming.getNotes().filterIsInstance<ArcNote>().map {
            if (it.isGuidingLine.not() && it.startPosition == it.endPosition) {
                val deltaTime = it.endTime - it.time
                val segTime = deltaTime / 3
                val t1 = segTime + it.time
                val t2 = segTime * 1.5 + it.time
                val t3 = segTime * 2 + it.time
                arcNote(it.time, t1, it.startPosition, ArcNote.CurveType.S, it.startPosition, it.color)
                arcNote(t1, t2, it.startPosition, ArcNote.CurveType.S, it.startPosition.offsetWith(-amplifier * deltaTime, 0.0), it.color)
                arcNote(t2, t3, it.startPosition.offsetWith(-amplifier * deltaTime, 0.0), ArcNote.CurveType.S, it.startPosition, it.color)
                arcNote(t3, it.endTime, it.startPosition, ArcNote.CurveType.S, it.startPosition, it.color)
            } else {
                this.chart.mainTiming.addArcNote(it)
            }
        }
    }

}