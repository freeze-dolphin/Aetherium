package io.sn.aetherium.objects.serialiation

import com.tairitsu.compose.arcaea.Chart
import com.tairitsu.compose.arcaea.Position
import com.tairitsu.compose.arcaea.pos
import io.sn.aetherium.objects.ShardDigestion
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ArraySerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object UnionSerializer : KSerializer<ShardDigestion.Union> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("ShardDigestion.Union") {
            element<String>("stringValue")
            element<Int>("intValue")
            element<Long>("longValue")
            element<Double>("doubleValue")
            element<Boolean>("booleanValue")
            element<Array<String>>("stringArrayValue")
            element<Array<Int>>("intArrayValue")
            element<Array<Long>>("longArrayValue")
            element<Array<Double>>("doubleArrayValue")
            element<Array<Boolean>>("booleanArrayValue")
            element<Chart>("chartValue")
            element<Position>("positionValue")
            element<Long>("timingValue")
        }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: ShardDigestion.Union) {
        val composite = encoder.beginStructure(descriptor)
        when {
            value.string != null -> composite.encodeStringElement(descriptor, 0, value.string!!)
            value.primitive != null && value.primitive!!.intInited -> composite.encodeIntElement(
                descriptor,
                1,
                value.primitive!!.int
            )

            value.primitive != null && value.primitive!!.longInited -> {
                if (value.longIsTiming) {
                    composite.encodeLongElement(
                        descriptor,
                        12,
                        value.primitive!!.long
                    )
                } else {
                    composite.encodeLongElement(
                        descriptor,
                        2,
                        value.primitive!!.long
                    )
                }
            }

            value.primitive != null && value.primitive!!.doubleInited -> composite.encodeDoubleElement(
                descriptor,
                3,
                value.primitive!!.double
            )

            value.primitive != null && value.primitive!!.booleanInited -> composite.encodeBooleanElement(
                descriptor,
                4,
                value.primitive!!.boolean
            )

            value.stringArr != null -> composite.encodeSerializableElement(
                descriptor,
                5,
                ArraySerializer(String.serializer()),
                value.stringArr!!
            )

            value.intArr != null -> composite.encodeSerializableElement(
                descriptor,
                6,
                ArraySerializer(Int.serializer()),
                value.intArr!!
            )

            value.longArr != null -> composite.encodeSerializableElement(
                descriptor,
                7,
                ArraySerializer(Long.serializer()),
                value.longArr!!
            )

            value.doubleArr != null -> {
                if (value.doubleArrIsPosition) {
                    composite.encodeSerializableElement(descriptor, 11, ArraySerializer(Double.serializer()), value.doubleArr!!)
                } else {
                    composite.encodeSerializableElement(
                        descriptor,
                        8,
                        ArraySerializer(Double.serializer()),
                        value.doubleArr!!
                    )
                }
            }

            value.booleanArr != null -> composite.encodeSerializableElement(
                descriptor,
                9,
                ArraySerializer(Boolean.serializer()),
                value.booleanArr!!
            )

            value.chart != null -> composite.encodeSerializableElement(descriptor, 10, Chart.serializer(), value.chart!!)
        }
        composite.endStructure(descriptor)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): ShardDigestion.Union {
        val composite = decoder.beginStructure(descriptor)

        var string: String? = null
        var primitive: ShardDigestion.Union.PrimitiveUnion? = null
        var stringArr: Array<String>? = null
        var intArr: Array<Int>? = null
        var longArr: Array<Long>? = null
        var doubleArr: Array<Double>? = null
        var booleanArr: Array<Boolean>? = null
        var chart: Chart? = null

        var position: Position? = null
        var timing: ShardDigestion.Union.PrimitiveUnion? = null

        loop@ while (true) {
            when (composite.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop
                11 -> position = composite.decodeSerializableElement(descriptor, 11, ArraySerializer(Double.serializer())).let {
                    it[0] pos it[1]
                }

                12 -> timing = ShardDigestion.Union.PrimitiveUnion(composite.decodeLongElement(descriptor, 12))

                0 -> string = composite.decodeStringElement(descriptor, 0)
                1 -> primitive = ShardDigestion.Union.PrimitiveUnion(composite.decodeIntElement(descriptor, 1))
                2 -> primitive = ShardDigestion.Union.PrimitiveUnion(composite.decodeLongElement(descriptor, 2))
                3 -> primitive = ShardDigestion.Union.PrimitiveUnion(composite.decodeDoubleElement(descriptor, 3))
                4 -> primitive = ShardDigestion.Union.PrimitiveUnion(composite.decodeBooleanElement(descriptor, 4))
                5 -> stringArr = composite.decodeSerializableElement(descriptor, 5, ArraySerializer(String.serializer()))
                6 -> intArr = composite.decodeSerializableElement(descriptor, 6, ArraySerializer(Int.serializer()))
                7 -> longArr = composite.decodeSerializableElement(descriptor, 7, ArraySerializer(Long.serializer()))
                8 -> doubleArr = composite.decodeSerializableElement(descriptor, 8, ArraySerializer(Double.serializer()))
                9 -> booleanArr = composite.decodeSerializableElement(descriptor, 9, ArraySerializer(Boolean.serializer()))
                10 -> chart = composite.decodeSerializableElement(descriptor, 10, Chart.serializer())
            }
        }
        composite.endStructure(descriptor)

        if (position != null) return ShardDigestion.Union.Restriction.fromPosition(position)
        if (timing != null) return ShardDigestion.Union.Restriction.fromTiming(timing.long)

        if (string != null) return ShardDigestion.Union(string)
        if (primitive != null) return ShardDigestion.Union(primitive)
        if (stringArr != null) return ShardDigestion.Union(stringArr)
        if (intArr != null) return ShardDigestion.Union(intArr)
        if (longArr != null) return ShardDigestion.Union(longArr)
        if (doubleArr != null) return ShardDigestion.Union(doubleArr)
        if (booleanArr != null) return ShardDigestion.Union(booleanArr)
        if (chart != null) return ShardDigestion.Union(chart)
        throw IllegalStateException("Unable to deserialize the Union")
    }
}