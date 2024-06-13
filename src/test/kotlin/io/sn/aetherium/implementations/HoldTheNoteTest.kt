package io.sn.aetherium.implementations

import com.tairitsu.compose.arcaea.*
import io.sn.aetherium.utils.composeChart
import io.sn.aetherium.utils.quickArctap
import java.io.File
import kotlin.test.Test

class HoldTheNoteTest {

    @Test
    fun `test note holder`() {
        composeChart {
            val bpm = 100.0
            val offset = 0L
            val pinTime = 28071L
            val hoverDistance = 370L
            val hitTime = 29785L

            val delay = 100L

            timing(offset, 140, 4)
            makeHoveringArcTap(offset, bpm, pinTime, hoverDistance, hitTime, 0.5 pos 1.0)

            makeHoveringArcTap(offset, bpm, pinTime + delay, hoverDistance, hitTime, 0 pos 1.0)
            makeHoveringArcTap(offset, bpm, pinTime + delay, hoverDistance, hitTime, 1 pos 1.0)

            makeHoveringArcTap(offset, bpm, pinTime + delay * 2, hoverDistance, hitTime, -0.5 pos 1.0)
            makeHoveringArcTap(offset, bpm, pinTime + delay * 2, hoverDistance, hitTime, 1.5 pos 1.0)

        }.serializeForArcaea().let {
            File("./result/0.aff").writeText(it)
        }
    }

    fun Difficulty.makeHoveringArcTap(offset: Long, bpm: Double, pinTime: Long, hoverDistance: Long, hitTime: Long, position: Position) {
        timingGroup {
            timing(offset, -bpm, 4)
            timing(pinTime, 0, 4)
            timing(hitTime - hoverDistance, bpm, 4)
            quickArctap(hitTime, position)
        }
    }

}