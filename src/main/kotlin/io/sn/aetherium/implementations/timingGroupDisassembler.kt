package io.sn.aetherium.implementations

import com.tairitsu.compose.arcaea.*
import io.sn.aetherium.utils.composeChart
import kotlin.random.Random

fun TimingGroup.disassemble(partNumber: Int, timingClosure: Int.() -> Timing): List<TimingGroup> {
    val bucket = mutableMapOf<Int, TimingGroup>()
    composeChart {
        (0 until partNumber).forEach {
            val timing = timingClosure(it)
            bucket[it] = timingGroup {
                timing(timing.offset, timing.bpm, timing.beats)
            }
        }
    }


    this.getNotes().forEach {
        val rnd = Random.nextInt(5)
        bucket[rnd]?.addArcNote((it as ArcNote).copy(
            time = 29785,
            endTime = 29785,
            startPosition = it.startPosition.copy(
                y = it.startPosition.y + 0.1
            ),
            endPosition = it.endPosition.copy(
                y = it.endPosition.y + 0.1
            )
        ))
    }

    return bucket.map {
        it.value
    }
}