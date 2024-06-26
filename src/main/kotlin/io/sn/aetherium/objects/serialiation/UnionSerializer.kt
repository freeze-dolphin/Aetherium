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
            /*  0   */  element<String>("stringValue")
            /*  1   */  element<Int>("intValue")
            /*  2   */  element<Long>("longValue")
            /*  3   */  element<Double>("doubleValue")
            /*  4   */  element<Boolean>("booleanValue")
            /*  5   */  element<Array<String>>("stringArrayValue")
            /*  6   */  element<Array<Int>>("intArrayValue")
            /*  7   */  element<Array<Long>>("longArrayValue")
            /*  8   */  element<Array<Double>>("doubleArrayValue")
            /*  9   */  element<Array<Boolean>>("booleanArrayValue")
            /*  10  */  element<Chart>("chartValue")
            /*  11  */  element<Position>("positionValue")
            /*  12  */  element<Long>("timingValue")
            /*  13  */  element<String>("placeholder")
        }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: ShardDigestion.Union) {
        val composite = encoder.beginStructure(descriptor)
        when {
            value.placeholder != null -> composite.encodeStringElement(
                descriptor,
                13,
                value.placeholder!!
            )

            value.string != null ->
                composite.encodeStringElement(descriptor, 0, value.string!!)

            value.primitive != null && value.primitive!!.intInited -> {
                composite.encodeIntElement(
                    descriptor,
                    1,
                    value.primitive!!.int
                )
            }

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

            value.primitive != null && value.primitive!!.doubleInited -> {
                composite.encodeDoubleElement(
                    descriptor,
                    3,
                    value.primitive!!.double
                )
            }

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

        var placeholder: ShardDigestion.Union? = null

        loop@ while (true) {
            when (val index = composite.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop
                11 -> position = composite.decodeSerializableElement(descriptor, 11, ArraySerializer(Double.serializer())).let {
                    it[0] pos it[1]
                }

                12 -> timing = ShardDigestion.Union.PrimitiveUnion(composite.decodeLongElement(descriptor, index))

                13 -> placeholder = ShardDigestion.Union(composite.decodeStringElement(descriptor, index))

                0 -> string = composite.decodeStringElement(descriptor, index)

                1 -> primitive = ShardDigestion.Union.PrimitiveUnion(composite.decodeIntElement(descriptor, index))
                2 -> primitive = ShardDigestion.Union.PrimitiveUnion(composite.decodeLongElement(descriptor, index))
                3 -> primitive = ShardDigestion.Union.PrimitiveUnion(composite.decodeDoubleElement(descriptor, index))
                4 -> primitive = ShardDigestion.Union.PrimitiveUnion(composite.decodeBooleanElement(descriptor, index))
                5 -> stringArr = composite.decodeSerializableElement(descriptor, index, ArraySerializer(String.serializer()))
                6 -> intArr = composite.decodeSerializableElement(descriptor, index, ArraySerializer(Int.serializer()))
                7 -> longArr = composite.decodeSerializableElement(descriptor, index, ArraySerializer(Long.serializer()))
                8 -> doubleArr = composite.decodeSerializableElement(descriptor, index, ArraySerializer(Double.serializer()))
                9 -> booleanArr = composite.decodeSerializableElement(descriptor, index, ArraySerializer(Boolean.serializer()))
                10 -> chart = composite.decodeSerializableElement(descriptor, index, Chart.serializer())
            }
        }
        composite.endStructure(descriptor)

        if (position != null) return ShardDigestion.Union.Restriction.fromPosition(position)
        if (timing != null) return ShardDigestion.Union.Restriction.fromTiming(timing.long)
        if (placeholder != null) return ShardDigestion.Union.Restriction.placeholder(placeholder.string!!)

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