package io.sn.aetherium.objects.serialiation

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object AetheriumExceptionSerializer : KSerializer<Exception> {


    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("AetheriumException") {
        element<String>("type")
        element<String>("message")
        element<List<String>>("trace")
    }

    override fun deserialize(decoder: Decoder): Exception {
        TODO("No need to implement deserializer")
    }

    override fun serialize(encoder: Encoder, value: Exception) {
        val message = value.message ?: ""
        encoder.beginStructure(descriptor).run {
            encodeStringElement(descriptor, 0, value.javaClass.canonicalName)
            encodeStringElement(descriptor, 1, message)
            encodeSerializableElement(descriptor, 2, ListSerializer(String.serializer()), value.stackTrace.map {
                it.toString()
            })
            endStructure(descriptor)
        }
    }
}