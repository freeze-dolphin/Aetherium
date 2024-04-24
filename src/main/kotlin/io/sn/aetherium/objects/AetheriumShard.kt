package io.sn.aetherium.objects

import com.tairitsu.compose.arcaea.Difficulty
import com.tairitsu.compose.arcaea.future
import com.tairitsu.compose.arcaea.mapSet
import io.sn.aetherium.objects.exceptions.MissingArgumentException
import io.sn.aetherium.objects.exceptions.ShardHaventInitException
import java.io.File
import kotlin.reflect.KClass


@Target(AnnotationTarget.CLASS)
@Retention
@MustBeDocumented
annotation class ShardInfo(
    /**
     * This field is used to identify your shard with others
     */
    val id: String,

    /** Whether your shard needs a configuration file
     */
    val hasConfig: Boolean = false,

    /** Whether your shard will handle the loading and init itself
     */
    val manualLoad: Boolean = false
)


data class ShardDigestion(
    val id: String,
    val args: ShardDigestionArgs
) {

    class Union {
        lateinit var string: String
        var int: Int = 0
        var long: Long = 0
        var double: Double = 0.0
        var boolean: Boolean = false
        lateinit var stringList: List<String>
        lateinit var intList: List<Int>
        lateinit var longList: List<Long>
        lateinit var doubleList: List<Double>
        lateinit var booleanList: List<Boolean>
    }
}

typealias ShardDigestionArgs = MutableMap<String, ShardDigestion.Union>

data class ShardDigestionArgsInfo(
    val items: MutableList<Item>
) {

    constructor(closure: (ShardDigestionArgsInfo.() -> Unit)) : this(mutableListOf()) {
        closure.invoke(this)
    }

    /**
     * Declare a new argument
     */
    fun addInfo(name: String, type: Item.Type) {
        items.add(Item(name, type))
    }

    fun contains(name: String): Boolean = items.map { it.name }.contains(name)

    data class Item(
        val name: String,
        val type: Type
    ) {

        enum class Type {
            STRING, INT, LONG, DOUBLE, BOOLEAN,
            STRING_LIST, INT_LIST, LONG_LIST, DOUBLE_LIST, BOOLEAN_LIST,
        }
    }
}


interface ChartGenerator {
    fun generator(): Difficulty.() -> Unit

    fun onRegister() {}

    /**
     * Only shards defined within Aetherium should be internal!
     */
    val isInternal: Boolean
        get() = false
}

@Suppress("UNUSED")
abstract class AetheriumShard : ChartGenerator {

    private lateinit var args: ShardDigestionArgs
    private lateinit var controllerBrand: ControllerBrand
    private var configFile: File? = null
    private lateinit var id: String
    var inited: Boolean = false

    abstract val digestionInfo: ShardDigestionArgsInfo

    override val isInternal: Boolean
        get() = false

    fun init(id: String, controllerBrand: ControllerBrand, configFile: File?) {
        this.id = id
        this.controllerBrand = controllerBrand
        this.configFile = configFile
        inited = true
    }

    fun feed(args: ShardDigestionArgs) {
        this.args = args
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
                        generator().invoke(this)
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
            return args[argName]!!.string
        } else {
            throw MissingArgumentException(argName)
        }
    }

    fun digestInt(argName: String): Int {
        validateInit()
        if (args.containsKey(argName)) {
            return args[argName]!!.int
        } else {
            throw MissingArgumentException(argName)
        }
    }

    fun digestLong(argName: String): Long {
        validateInit()
        if (args.containsKey(argName)) {
            return args[argName]!!.long
        } else {
            throw MissingArgumentException(argName)
        }
    }

    fun digestDouble(argName: String): Double {
        validateInit()
        if (args.containsKey(argName)) {
            return args[argName]!!.double
        } else {
            throw MissingArgumentException(argName)
        }
    }

    fun digestBoolean(argName: String): Boolean {
        validateInit()
        if (args.containsKey(argName)) {
            return args[argName]!!.boolean
        } else {
            throw MissingArgumentException(argName)
        }
    }

    fun digestStringList(argName: String): List<String> {
        validateInit()
        if (args.containsKey(argName)) {
            return args[argName]!!.stringList
        } else {
            throw MissingArgumentException(argName)
        }
    }

    fun digestIntList(argName: String): List<Int> {
        validateInit()
        if (args.containsKey(argName)) {
            return args[argName]!!.intList
        } else {
            throw MissingArgumentException(argName)
        }
    }

    fun digestLongList(argName: String): List<Long> {
        validateInit()
        if (args.containsKey(argName)) {
            return args[argName]!!.longList
        } else {
            throw MissingArgumentException(argName)
        }
    }

    fun digestDoubleList(argName: String): List<Double> {
        validateInit()
        if (args.containsKey(argName)) {
            return args[argName]!!.doubleList
        } else {
            throw MissingArgumentException(argName)
        }
    }

    fun digestBooleanList(argName: String): List<Boolean> {
        validateInit()
        if (args.containsKey(argName)) {
            return args[argName]!!.booleanList
        } else {
            throw MissingArgumentException(argName)
        }
    }

}

private val registerTable = hashMapOf<String, Pair<KClass<out AetheriumShard>, ShardDigestionArgsInfo>>()

private val shardInstanceTable = hashMapOf<String, AetheriumShard>()

fun register(id: String, clazz: KClass<out AetheriumShard>, info: ShardDigestionArgsInfo) {
    registerTable[id] = Pair(clazz, info)
}

fun lookUpShard(id: String): Pair<KClass<out AetheriumShard>, ShardDigestionArgsInfo> {
    return registerTable[id] ?: throw IllegalArgumentException("Unable to find a shard in this id")
}

fun getOrPutInstance(id: String, newInstance: AetheriumShard): AetheriumShard {
    return shardInstanceTable.getOrPut(id) { newInstance }
}