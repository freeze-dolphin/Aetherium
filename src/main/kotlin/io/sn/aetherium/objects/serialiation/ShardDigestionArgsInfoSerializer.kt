package io.sn.aetherium.objects.serialiation

import io.sn.aetherium.objects.ShardDigestionArgsInfo
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ShardDigestionArgsInfoSerializer : KSerializer<ShardDigestionArgsInfo> {

    private val serializer = ListSerializer(ShardDigestionArgsInfo.Item.serializer())

    override val descriptor: SerialDescriptor
        get() = serializer.descriptor

    override fun deserialize(decoder: Decoder): ShardDigestionArgsInfo {
        throw IllegalStateException("No need to implement deserializer")
    }

    override fun serialize(encoder: Encoder, value: ShardDigestionArgsInfo) {
        serializer.serialize(encoder, value.items)
    }
}
