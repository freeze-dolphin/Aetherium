package io.sn.aetherium.objects

import com.tairitsu.compose.arcaea.Difficulty
import com.tairitsu.compose.arcaea.future
import com.tairitsu.compose.arcaea.mapSet
import io.sn.aetherium.objects.exceptions.MissingArgumentException
import io.sn.aetherium.objects.exceptions.ShardHaventInitException
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention
@MustBeDocumented
annotation class ShardInfo(val id: String, val manualLoad: Boolean = false)

typealias ShardDigestionArgs = MutableMap<String, String>

@Serializable
data class ShardDigestion(
    val id: String,
    @Serializable
    val args: ShardDigestionArgs
)

data class ShardDigestionArgsInfo(
    val items: MutableList<Item>
) {

    constructor(closure: (ShardDigestionArgsInfo.() -> Unit)) : this(mutableListOf()) {
        closure.invoke(this)
    }

    fun addInfo(name: String, type: Item.Type) {
        items.add(Item(name, type))
    }

    fun contains(name: String): Boolean = items.map { it.name }.contains(name)

    data class Item(
        val name: String,
        val type: Type
    ) {

        enum class Type {
            STRING, INT, LONG, DOUBLE, BOOLEAN
        }
    }

}

interface ChartGenerator {
    fun generator(controllerBrand: ControllerBrand, args: ShardDigestionArgs): Difficulty.() -> Unit

    fun load() {}
}

abstract class AetheriumShard : ChartGenerator {

    private lateinit var args: ShardDigestionArgs
    private lateinit var controllerBrand: ControllerBrand
    private var inited: Boolean = false

    abstract val digestionInfo: ShardDigestionArgsInfo

    fun init(controllerBrand: ControllerBrand, args: ShardDigestionArgs) {
        this.controllerBrand = controllerBrand
        this.args = args
        inited = true
    }

    private fun validateInit() {
        if (!inited) throw ShardHaventInitException("Generation should be after init")
    }

    fun generate(): Difficulty {
        validateInit()

        var result: Difficulty? = null
        mapSet {
            difficulties.future {
                mapSet {
                    difficulties.future {
                        generator(controllerBrand, args).invoke(this)
                        result = this
                    }
                }
            }
        }

        return result!!
    }

    fun brand(): ControllerBrand {
        validateInit()
        return controllerBrand
    }

    fun digestString(argName: String): String {
        validateInit()
        if (args.containsKey(argName)) {
            return args[argName]!!
        } else {
            throw MissingArgumentException(argName)
        }
    }

    fun digestLong(argName: String): Long = digestString(argName).toLong()

    fun digestInt(argName: String): Int = digestString(argName).toInt()

    fun digestDouble(argName: String): Double = digestString(argName).toDouble()

    fun digestBoolean(argName: String): Boolean = digestString(argName).toBoolean()

}

private val registerTable = hashMapOf<String, Pair<KClass<out AetheriumShard>, ShardDigestionArgsInfo>>()

fun register(id: String, clazz: KClass<out AetheriumShard>, info: ShardDigestionArgsInfo) {
    registerTable[id] = Pair(clazz, info)
}


fun lookUp(id: String): Pair<KClass<out AetheriumShard>, ShardDigestionArgsInfo> {
    return registerTable[id] ?: throw IllegalArgumentException("Unable to find a shard in this id")
}