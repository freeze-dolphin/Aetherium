package io.sn.aetherium.implementations

import com.tairitsu.compose.arcaea.ArcNote
import com.tairitsu.compose.arcaea.Position
import com.tairitsu.compose.arcaea.toPosition
import kotlin.math.sqrt

val noteJumpGetFrame = fun(
    hideTiming: Long,
    duration: Double,
    position: Position,
    progress: Double,
    extraNoteOffset: Long,
    extra: Any?,
): List<ArcNote> {
    val targetPosition = extra!! as Position
    val currentCoord = getParabolaCoordinateAtTime(NoteInfo(position.x, 0.0), NoteInfo(targetPosition.x, duration), progress)
    val offset = (currentCoord.second * ARCAEA_COORD_SYSTEM_ZOOM_CONSTANT + extraNoteOffset).toLong()

    println(offset)

    return listOf(ArcNote((hideTiming + offset - 1), (hideTiming + offset), currentCoord.toPosition().apply {
        y = 0.0
    }, ArcNote.CurveType.S, currentCoord.toPosition().apply {
        y = 0.0
    }, ArcNote.Color.BLUE, true
    ) {
        arctap((hideTiming + offset))
    })
}

private var ARCAEA_COORD_SYSTEM_ZOOM_CONSTANT: Long = 2000

fun adjustZoomingConst(newValue: Long) {
    ARCAEA_COORD_SYSTEM_ZOOM_CONSTANT = newValue
}

private data class NoteInfo(var xInArcCoordSystem: Double, val distanceBetweenNoteAndJudge: Double) {
    val x: Long = (xInArcCoordSystem * ARCAEA_COORD_SYSTEM_ZOOM_CONSTANT).toLong()
    val y: Long = distanceBetweenNoteAndJudge.toLong()
}

/**
 * 返回结果中的 Point, x 为 arc 中的横坐标值, y 为距离判定平面的距离
 */
private fun getParabolaCoordinateAtTime(a: NoteInfo, b: NoteInfo, progress: Double): Pair<Double, Double> {
    val g = 9.81 // 重力加速度
    val t = sqrt(2 * (b.y - a.y) / g) // 计算运动总时间

    // 根据抛物线的方程 x = v0x * t 和 y = a.y + v0y * t - 0.5 * g * t^2 计算位置
    val x = a.x + (b.x - a.x) * progress
    val y = a.y + (b.y - a.y) * progress - 0.5 * g * t * t * progress * progress

    return Pair(x / ARCAEA_COORD_SYSTEM_ZOOM_CONSTANT, y)
}