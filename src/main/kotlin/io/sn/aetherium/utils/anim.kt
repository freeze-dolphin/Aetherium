package io.sn.aetherium.utils

import com.tairitsu.compose.arcaea.*
import io.sn.aetherium.objects.animcfg.AnimationBasicConfigurtion
import io.sn.aetherium.objects.animcfg.AnimationConfiguration

fun Difficulty.addAnimation(animCfg: AnimationConfiguration) {
    this.addAnimation(
        animCfg.basicCfg,
        animCfg.startTiming,
        animCfg.duration,
        animCfg.radius,
        animCfg.position,
        animCfg.extraNoteOffset,
        animCfg.showFirstFrame,
        animCfg.generateArcNotes,
        animCfg.extra
    )
}

/**
 * @param basicCfg: control the quality and speed of this animation
 * @param startTiming: the start timing of this animation
 * @param duration: the duration of this animation
 * @param radius: the start and end radius
 * @param position: the start and end position
 * @param extraNoteOffset: extra note offset that added to this animation
 * @param generateNotes: the function that generate [ArcNote];
 *        Parameter Info:
 *        generateArcNotes(
 *             hideTiming: Long,
 *             radius: Double,
 *             position: Position,
 *             progress: Double,
 *             extraNoteOffset: Long,
 *             extra: Any?,
 *        )
 */
fun Difficulty.addAnimation(
    basicCfg: AnimationBasicConfigurtion,
    startTiming: Long,
    duration: Long,
    radius: Triple<Double, Double, EasingFunction>,
    position: Triple<Position, Position, EasingFunction>,
    extraNoteOffset: Long,
    showFirstFrame: Boolean = true,
    generateNotes: (Long, Double, Position, Double, Long, Any?) -> List<Note>,
    extra: Any?,
) {
    val startRadius = radius.first
    val endRadius = radius.second
    val easingRadius = radius.third

    val startPosition = position.first
    val endPosition = position.second
    val easingPosition = position.third

    val radisDiff = endRadius - startRadius
    val positionDiff = endPosition.toList().mapIndexed { idx, d ->
        d - startPosition.toList()[idx]
    }

    val realFrameCount: Double = ((basicCfg.frameCount * duration / 1000.0).toInt() - 1).toDouble()

    for (i in 0..realFrameCount.toInt()) {
        if (i == 0 && !showFirstFrame) continue

        val progress: Double = i / realFrameCount
        val nextProgress: Double = (i + 1) / realFrameCount

        val currentRadius = startRadius + radisDiff * easingRadius.invoke(progress)
        val currentPosition = startPosition.toList().mapIndexed { idx, d ->
            d + positionDiff.map { d2 ->
                d2 * easingPosition.invoke(progress)
            }[idx]
        }

        val showTiming: Long = startTiming + (duration * progress).toLong()
        val hideTiming: Long = startTiming + (duration * nextProgress).toLong()

        timingGroup(
            noinput
        ) {
            timing(0, basicCfg.bpm, 999)
            timing(showTiming - 1, -basicCfg.bpm * basicCfg.noteOffset, 999)
            timing(showTiming, 0, 999)
            timing(hideTiming - 1, -basicCfg.bpm * basicCfg.noteOffset, 999)
            timing(hideTiming, basicCfg.bpm, 999)
            generateNotes.invoke(
                hideTiming + basicCfg.noteOffset + extraNoteOffset,
                currentRadius,
                currentPosition[0] pos currentPosition[1],
                progress,
                extraNoteOffset,
                extra
            ).forEach {
                when (it) {
                    is ArcNote -> addArcNote(it)
                    is HoldNote -> addHoldNote(it)
                    is NormalNote -> addNormalNote(it)
                }
            }
        }
    }
}