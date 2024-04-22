package io.sn.aetherium.implementations

import com.tairitsu.compose.arcaea.ArcNote
import com.tairitsu.compose.arcaea.Position
import io.sn.aetherium.utils.EasingFunction
import io.sn.aetherium.utils.easePos
import io.sn.aetherium.utils.offsetWith
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

fun genCollapseTraceGroup(
    startTiming: Long,
    endTiming: Long,
    startPos: Position,
    endPos: Position,
    segmentNum: Int,
    easingFunction: EasingFunction,
    amplifier: Double,
    amplitude: Double,
): List<ArcNote> {
    val result = mutableListOf<ArcNote>()

    var resizedAmplifier = Random.nextDouble(-amplitude, amplitude) + amplifier

    val cuttingSeq = getRandomSeq(startTiming, endTiming, segmentNum)
    cuttingSeq.add(0, startTiming)
    cuttingSeq.add(endTiming)

    var curPos = startPos
    var curEndPos = balancePos(startPos, endPos, curPos, 1.0 / segmentNum, easingFunction, resizedAmplifier)

    for (idx in (0..segmentNum)) {
        val progress = (idx + 1).toDouble() / segmentNum
        resizedAmplifier = Random.nextDouble(-amplitude, amplitude) + amplifier
        result.add(
            ArcNote(
                cuttingSeq[idx] + if (Random.nextDouble() < 0.2) 0 else Random.nextInt(
                    max(((cuttingSeq[idx + 1] - cuttingSeq[idx]) / 2).toInt(), 1) * -1,
                    max(((cuttingSeq[idx + 1] - cuttingSeq[idx]) / 2).toInt(), 1) * 1
                ),
                cuttingSeq[idx + 1],
                curPos,
                ArcNote.CurveType.S,
                curEndPos,
                ArcNote.Color.BLUE,
                true
            )
        )
        val tmpPos =
            curPos.copy().offsetWith(Random.nextDouble(amplifier / -2, amplifier / 2), Random.nextDouble(amplifier / -2, amplifier / 2))
        curPos = curEndPos.copy()
        curEndPos = balancePos(startPos, endPos, tmpPos, progress, easingFunction, resizedAmplifier)
    }

    return result
}

private fun balancePos(
    startPos: Position,
    endPos: Position,
    curPos: Position,
    progress: Double,
    easingFunction: EasingFunction,
    amplifier: Double,
): Position {
    val easedPos = easePos(startPos, endPos, easingFunction, progress)

    return curPos.apply {
        val sgnY = (easedPos.y > curPos.y).let {
            if (it) 1 else -1
        }
        val sgnX = (easedPos.x > curPos.x).let {
            if (it) 1 else -1
        }


        y = max(
            0.0,

            y + sgnY * 2 * abs(easedPos.y - curPos.y)
                    + Random.nextDouble(-amplifier / 2, amplifier / 2)

        )
        x = min(
            1.5, max(
                -0.5,

                x + sgnX * 2 * abs(easedPos.x - curPos.x)
                        + Random.nextDouble(-amplifier * 3, amplifier * 3)

            )
        )
    }
}

private fun getRandomSeq(startTiming: Long, endTiming: Long, segmentNum: Int): MutableList<Long> {
    val cuttingPoints = mutableListOf<Long>()
    repeat(segmentNum) {
        var rand: Long
        do {
            rand = Random.nextLong(startTiming + 1, endTiming)
        } while (cuttingPoints.contains(rand))
        cuttingPoints.add(rand)
    }
    cuttingPoints.sort()
    return cuttingPoints
}
