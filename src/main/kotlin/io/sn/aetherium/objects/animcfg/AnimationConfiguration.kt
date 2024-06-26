package io.sn.aetherium.objects.animcfg

import com.tairitsu.compose.arcaea.ArcNote
import com.tairitsu.compose.arcaea.Position
import io.sn.aetherium.utils.EasingFunction

data class AnimationConfiguration(
    val basicCfg: AnimationBasicConfigurtion,
    val startTiming: Long,
    val duration: Long,
    val radius: Triple<Double, Double, EasingFunction>,
    val position: Triple<Position, Position, EasingFunction>,
    val extraNoteOffset: Long,
    val showFirstFrame: Boolean = true,
    val generateArcNotes: (Long, Double, Position, Double, Long, Any?) -> List<ArcNote>,
    val extra: Any?,
)
