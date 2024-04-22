package io.sn.aetherium.objects.exceptions

import io.sn.aetherium.objects.serialiation.AetheriumExceptionSerializer
import kotlinx.serialization.Serializable

@Serializable
data class AetheriumError(
    @Serializable(AetheriumExceptionSerializer::class)
    val exception: Exception
)