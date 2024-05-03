package io.sn.aetherium.objects

import com.tairitsu.compose.arcaea.*
import io.sn.aetherium.objects.exceptions.MissingArgumentException
import io.sn.aetherium.objects.exceptions.ShardHaventInitException
import io.sn.aetherium.objects.serialiation.ShardDigestionArgsInfoSerializer
import io.sn.aetherium.objects.serialiation.UnionSerializer
import kotlinx.serialization.Serializable
import java.io.File
import java.io.Serial
import kotlin.reflect.KClass

@RequiresOptIn(message = "Do not use in scenarios except testing")
internal annotation class TestOnlyApi

@RequiresOptIn(message = "These types are not user friendly, use only in testing units")
internal annotation class UserUnfriendlyTypes

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
    val name: LocalizedString,
    val args: ShardDigestionArgs
) {

    @Serializable(UnionSerializer::class)
    class Union {

        object Restriction {

            /**
             * Special Union data, representing the current timing in chart editors
             */
            val editorCurrentTiming: Union = fromTiming(-600000L)

            fun editorDelayTiming(delay: Long): Union = fromTiming(-600000L - delay)

            fun fromTiming(timing: Long): Union =
                Union(timing).apply {
                    longIsTiming = true
                }

            fun fromPosition(position: Position): Union =
                Union(arrayOf(position.x, position.y)).apply {
                    doubleArrIsPosition = true
                }
        }

        var string: String? = null
        var primitive: PrimitiveUnion? = null
        var stringArr: Array<String>? = null
        var intArr: Array<Int>? = null
        var longArr: Array<Long>? = null

        var longIsTiming = false

        var doubleArr: Array<Double>? = null
        var doubleArrIsPosition: Boolean = false

        var booleanArr: Array<Boolean>? = null
        var chart: Chart? = null

        class PrimitiveUnion {
            var int: Int = 0
            var long: Long = 0
            var double: Double = 0.0
            var boolean: Boolean = false

            var intInited = false
            var longInited = false
            var doubleInited = false
            var booleanInited = false

            constructor(int: Int) {
                this.int = int
                intInited = true
            }

            constructor(long: Long) {
                this.long = long
                longInited = true
            }

            constructor(double: Double) {
                this.double = double
                doubleInited = true
            }

            constructor(boolean: Boolean) {
                this.boolean = boolean
                booleanInited = true
            }
        }

        constructor()

        constructor(string: String) {
            this.string = string
        }

        constructor(primitive: PrimitiveUnion) {
            this.primitive = primitive
        }

        constructor(int: Int) {
            this.primitive = PrimitiveUnion(int)
        }

        constructor(long: Long) {
            this.primitive = PrimitiveUnion(long)
        }

        constructor(double: Double) {
            this.primitive = PrimitiveUnion(double)
        }

        constructor(boolean: Boolean) {
            this.primitive = PrimitiveUnion(boolean)
        }

        constructor(stringArr: Array<String>) {
            this.stringArr = stringArr
        }

        constructor(intArr: Array<Int>) {
            this.intArr = intArr
        }

        constructor(longArr: Array<Long>) {
            this.longArr = longArr
        }

        constructor(doubleArr: Array<Double>) {
            this.doubleArr = doubleArr
        }

        constructor(booleanArr: Array<Boolean>) {
            this.booleanArr = booleanArr
        }

        constructor(chart: Chart) {
            this.chart = chart
        }

    }
}

typealias ShardDigestionArgs = MutableMap<String, ShardDigestion.Union>

@Serializable(ShardDigestionArgsInfoSerializer::class)
data class ShardDigestionArgsInfo(
    val items: MutableList<Item>
) {

    constructor(closure: (ShardDigestionArgsInfo.() -> Unit)) : this(mutableListOf()) {
        closure.invoke(this)
    }

    /**
     * Declare a new argument
     */
    fun addInfo(id: String, type: Item.Type, defaultValue: ShardDigestion.Union? = null) {
        items.add(Item(id, type, defaultValue))
    }

    /**
     * Declare a new argument with localized name
     */
    fun addInfo(id: String, type: Item.Type, localizedString: LocalizedString, defaultValue: ShardDigestion.Union? = null) {
        items.add(Item(id, type, defaultValue, localizedString))
    }

    fun addInfo(
        id: String,
        type: Item.Type,
        defaultValue: ShardDigestion.Union?,
        enName: String,
        closure: LocalizedString.() -> Unit
    ) {
        val localizedString = LocalizedString(enName)
        closure.invoke(localizedString)
        addInfo(id, type, localizedString, defaultValue)
    }

    fun addInfo(
        id: String,
        type: Item.Type,
        enName: String,
        closure: LocalizedString.() -> Unit
    ) {
        val localizedString = LocalizedString(enName)
        closure.invoke(localizedString)
        addInfo(id, type, localizedString, null)
    }

    fun addInfo(
        id: String,
        type: Item.Type,
        defaultValue: ShardDigestion.Union?,
        enName: String,
    ) {
        val localizedString = LocalizedString(enName)
        addInfo(id, type, localizedString, defaultValue)
    }

    fun addInfo(
        id: String,
        type: Item.Type,
        enName: String,
    ) {
        val localizedString = LocalizedString(enName)
        addInfo(id, type, localizedString, null)
    }

    fun contains(name: String): Boolean = items.map { it.id }.contains(name)

    @Serializable
    data class Item(
        val id: String,
        val type: Type,
        val defaultValue: ShardDigestion.Union?,
        val name: LocalizedString? = null
    ) {

        enum class Type {
            STRING, INT, LONG, DOUBLE, BOOLEAN,
            STRING_ARRAY, INT_ARRAY, LONG_ARRAY, DOUBLE_ARRAY, BOOLEAN_ARRAY,
            TIMING, POSITION, CHART
        }
    }
}


interface ChartGenerator {
    fun generator(): Difficulty.() -> Unit

    fun onGenesis() {}

    fun onTermination() {}

    /**
     * Only shards defined within Aetherium should be internal!
     */
    val isInternal: Boolean
        get() = false
}

@Suppress("UNUSED")
abstract class AetheriumShard : ChartGenerator {

    abstract val name: LocalizedString

    private lateinit var args: ShardDigestionArgs
    private lateinit var controllerBrand: ControllerBrand
    private var configFile: File? = null
    private lateinit var id: String
    var inited: Boolean = false

    abstract val digestionInfo: ShardDigestionArgsInfo

    override val isInternal: Boolean
        get() = false

    fun init(id: String, configFile: File?) {
        this.id = id
        this.configFile = configFile
        inited = true
    }

    @TestOnlyApi
    fun testInit() {
        this.id = "`reserved`:fake"
        this.configFile = null
        inited = true
    }

    fun feed(controllerBrand: ControllerBrand, args: ShardDigestionArgs) {
        this.controllerBrand = controllerBrand
        this.args = args
    }

    private fun validateInit() {
        if (!inited) throw ShardHaventInitException("Generation should be after init")
    }

    fun generate(
        chartConfiguration: ChartConfiguration = ChartConfiguration(0, mutableListOf()),
        initClosure: Difficulty.() -> Unit = {}
    ): Difficulty {
        validateInit()

        var result: Difficulty? = null
        mapSet {
            difficulties.future {
                mapSet {
                    difficulties.future {
                        this.chart.configuration.sync(chartConfiguration)
                        initClosure.invoke(this)
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
            return args[argName]!!.string!!
        } else {
            throw MissingArgumentException(argName)
        }
    }

    fun digestInt(argName: String): Int {
        validateInit()
        if (args.containsKey(argName)) {
            return args[argName]!!.primitive!!.int
        } else {
            throw MissingArgumentException(argName)
        }
    }

    fun digestLong(argName: String): Long {
        validateInit()
        if (args.containsKey(argName)) {
            return args[argName]!!.primitive!!.long
        } else {
            throw MissingArgumentException(argName)
        }
    }

    fun digestDouble(argName: String): Double {
        validateInit()
        if (args.containsKey(argName)) {
            return args[argName]!!.primitive!!.double
        } else {
            throw MissingArgumentException(argName)
        }
    }

    fun digestBoolean(argName: String): Boolean {
        validateInit()
        if (args.containsKey(argName)) {
            return args[argName]!!.primitive!!.boolean
        } else {
            throw MissingArgumentException(argName)
        }
    }

    @UserUnfriendlyTypes
    fun digestStringList(argName: String): Array<String> {
        validateInit()
        if (args.containsKey(argName)) {
            return args[argName]!!.stringArr!!
        } else {
            throw MissingArgumentException(argName)
        }
    }

    @UserUnfriendlyTypes
    fun digestIntList(argName: String): Array<Int> {
        validateInit()
        if (args.containsKey(argName)) {
            return args[argName]!!.intArr!!
        } else {
            throw MissingArgumentException(argName)
        }
    }

    @UserUnfriendlyTypes
    fun digestLongArray(argName: String): Array<Long> {
        validateInit()
        if (args.containsKey(argName)) {
            return args[argName]!!.longArr!!
        } else {
            throw MissingArgumentException(argName)
        }
    }

    @UserUnfriendlyTypes
    fun digestDoubleArray(argName: String): Array<Double> {
        validateInit()
        if (args.containsKey(argName)) {
            return args[argName]!!.doubleArr!!
        } else {
            throw MissingArgumentException(argName)
        }
    }

    @UserUnfriendlyTypes
    fun digestBooleanList(argName: String): Array<Boolean> {
        validateInit()
        if (args.containsKey(argName)) {
            return args[argName]!!.booleanArr!!
        } else {
            throw MissingArgumentException(argName)
        }
    }

    fun digestTiming(argName: String): Long {
        validateInit()
        if (args.containsKey(argName)) {
            if (args[argName]!!.longIsTiming) {
                return args[argName]!!.primitive!!.long
            }
        }
        throw MissingArgumentException(argName)
    }

    fun digestPosition(argName: String): Position {
        validateInit()
        if (args.containsKey(argName)) {
            if (args[argName]!!.doubleArrIsPosition) {
                return args[argName]!!.doubleArr!!.let {
                    it[0] pos it[1]
                }
            }
        }
        throw MissingArgumentException(argName)
    }

    fun digestChart(argName: String): Chart {
        validateInit()
        if (args.containsKey(argName)) {
            return args[argName]!!.chart!!
        } else {
            throw MissingArgumentException(argName)
        }
    }

}

object AetheriumCache {

    private val registerTable = hashMapOf<String, ShardLookupResult>()

    private val shardInstanceTable = hashMapOf<String, AetheriumShard>()

    data class ShardLookupResult(
        val shardClass: KClass<out AetheriumShard>,
        val digestionArgsInfo: ShardDigestionArgsInfo,
        val name: LocalizedString,
    )

    fun register(id: String, clazz: KClass<out AetheriumShard>, name: LocalizedString, info: ShardDigestionArgsInfo) {
        registerTable[id] = ShardLookupResult(clazz, info, name)
    }

    fun lookupShard(id: String): ShardLookupResult {
        return registerTable[id] ?: throw IllegalArgumentException("Unable to find a shard in this id")
    }

    fun getOrPutInstance(id: String, newInstance: AetheriumShard): AetheriumShard {
        return shardInstanceTable.getOrPut(id) { newInstance }
    }

    fun queryInstances(): HashMap<String, AetheriumShard> {
        return shardInstanceTable
    }

    fun queryInfos(): Map<String, ShardDigestionArgsInfo> {
        return registerTable.keys.associateWith {
            registerTable[it]!!.digestionArgsInfo
        }
    }

}