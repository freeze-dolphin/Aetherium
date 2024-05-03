package io.sn.aetherium.utils

import com.tairitsu.compose.arcaea.*
import java.util.function.Consumer

fun genTrace(timing: Long, endTiming: Long, startPosition: Position, endPosition: Position): ArcNote {
    return ArcNote(timing, endTiming, startPosition.toPair(), ArcNote.CurveType.S, endPosition.toPair(), ArcNote.Color.BLUE, true) {}
}

fun genAnimationTrace(timing: Long, startPosition: Position, endPosition: Position): ArcNote {
    return genTrace(timing, timing, startPosition, endPosition)
}

fun Difficulty.quickArctap(time: Number, position: Position): Note {
    return arcNote(time.toLong() - 1, time.toLong(), position.toPair(), this.s, position.toPair()) {
        arctap(time.toLong())
    }
}

fun easePos(aPos: Position, bPos: Position, easingFunction: EasingFunction, progress: Double): Position {
    val deltaX: Double = bPos.x - aPos.x
    val deltaY: Double = bPos.y - aPos.y
    return easingFunction(progress) * deltaX + aPos.x pos easingFunction(progress) * deltaY + aPos.y
}

fun Position.offsetWith(x: Double, y: Double): Position {
    return this.x + x pos this.y + y
}

fun composeNotes(closure: Difficulty.() -> Unit): List<Note> {
    val diff = Difficulty()
    closure.invoke(diff)
    return diff.chart.mainTiming.getNotes()
}

fun composeChart(closure: Difficulty.() -> Unit): Chart {
    val diff = Difficulty()
    closure.invoke(diff)
    return diff.chart
}

fun comment(cons: Consumer<Any>) {
}