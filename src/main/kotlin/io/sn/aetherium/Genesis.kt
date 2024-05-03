package io.sn.aetherium

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlIndentation
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.file.TomlFileReader
import com.tairitsu.compose.arcaea.Chart
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.sn.aetherium.objects.*
import io.sn.aetherium.objects.exceptions.AetheriumConnectionException
import io.sn.aetherium.objects.exceptions.AetheriumError
import io.sn.aetherium.objects.exceptions.MissingArgumentException
import io.sn.aetherium.utils.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import java.util.jar.JarFile
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.isSubclassOf
import kotlin.system.exitProcess

object Genesis {

    @Serializable
    data class AetheriumConfiguration(
        val connection: AthCfgConnection
    ) {

        @Serializable
        data class AthCfgConnection(
            val host: String, val port: Int
        )
    }

    val json = Json {
        prettyPrint = false
        isLenient = false
    }

    val toml = Toml(
        outputConfig = TomlOutputConfig(
            indentation = TomlIndentation.NONE
        )
    )

    lateinit var configuration: AetheriumConfiguration

    val defaultConfiguration = AetheriumConfiguration(
        AetheriumConfiguration.AthCfgConnection("127.0.0.1", 8809)
    )

    lateinit var controllerBrand: ControllerBrand
    val isControllerBrandInitialized
        get() = ::controllerBrand.isInitialized

    fun terminate() {
        AetheriumCache.queryInstances().values.forEach {
            it.onTermination()
        }
        println("\nSending termination signal to shards...")
    }

}

fun main() {
    file(".", "aetherium.toml").let {
        if (!it.exists()) {
            it.createNewFile()
            it.writeText(Genesis.toml.encodeToString(Genesis.defaultConfiguration))
        }
        Genesis.configuration = TomlFileReader.decodeFromFile(serializer(), it.path)
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        Genesis.terminate()
    })

    val env = applicationEngineEnvironment {
        envConfig()
    }
    embeddedServer(Netty, env).start(true)
}

fun ApplicationEngineEnvironmentBuilder.envConfig() {
    module {
        module()
    }
    connector {
        host = Genesis.configuration.connection.host
        port = Genesis.configuration.connection.port
    }
}

@Suppress("unused")
fun Application.module() {
    val counter = PluginLoadCounter()

    run { // load internal plugins
        val reflection = Reflections("io.sn.aetherium.implementations.shards")

        val allClasses = reflection.getAll(Scanners.SubTypes).map { Class.forName(it) }
        allClasses.forEach { clazz ->
            try {
                val klass = clazz.kotlin
                if (klass.isSubclassOf(AetheriumShard::class) && klass.findAnnotations(ShardInfo::class).isNotEmpty()) {
                    @Suppress("UNCHECKED_CAST") counter.add(loadPlugin(klass as KClass<out AetheriumShard>), true)
                }
            } catch (e: Exception) {
                log.error(e.stackTraceToString())
                counter.add(PluginLoadState.FAILED, true)
            }
        }
    }

    run { // load external plugins
        val pluginFolder = file(".", "plugins")
        if (!pluginFolder.exists()) pluginFolder.mkdirs()
        pluginFolder.listFiles()?.forEach {
            try {
                if (it.name.endsWith(".jar")) {
                    val jarfile = JarFile(it)
                    val entry = readAetheriumEntry(jarfile)
                    val entryClass = loadClass(it, entry).kotlin
                    counter.add(loadPlugin(entryClass), false)
                }
            } catch (e: Exception) {
                log.error(e.stackTraceToString())
                counter.add(PluginLoadState.FAILED, false)
            }
        }
    }

    counter.logInfo(this)

    install(ContentNegotiation) {
        json(Genesis.json)
    }

    routing {
        get("/termination") {
            Genesis.terminate()

            call.respondText("Done with termination.\nSee you next time. ;)")
            exitProcess(0)
        }
        get("/connect") {
            try {
                val ua = (call.request.headers["User-Agent"])
                if (ua == null) {
                    throw AetheriumConnectionException("Unable to detect controller brand, please make sure the User-Agent is valid")
                } else {
                    val (controllerName, controllerVersion) = Regex("""(\w+)/([\d.]+)""").find(ua)!!.destructured
                    Genesis.controllerBrand = ControllerBrand(controllerName, controllerVersion)
                    this@module.log.info("Incoming connection: [Brand: ${controllerName}, Version: ${controllerVersion}]")
                    call.respond(AetheriumCache.queryInfos())
                }
            } catch (e: Exception) {
                call.respond(AetheriumError(e))
            }
        }
        post("/generate") {
            try {
                if (!Genesis.isControllerBrandInitialized) throw AetheriumConnectionException("Please GET /connect first to submit controller brand")
                val recv = Genesis.json.parseToJsonElement(call.receiveText()).jsonObject
                val id = recv["id"]!!.jsonPrimitive.content
                val jsonArgs = recv["args"]!!.jsonObject

                val lookupResult = AetheriumCache.lookupShard(id)

                val clazz = lookupResult.shardClass
                val info = lookupResult.digestionArgsInfo
                val localizedName = lookupResult.name

                // Assign parameter
                val shardArgs = mutableMapOf<String, ShardDigestion.Union>()
                info.items.forEach {
                    try {
                        val union = when (it.type) {
                            ShardDigestionArgsInfo.Item.Type.STRING -> ShardDigestion.Union(jsonArgs[it.id]!!.jsonPrimitive.content)
                            ShardDigestionArgsInfo.Item.Type.INT -> ShardDigestion.Union(jsonArgs[it.id]!!.jsonPrimitive.int)
                            ShardDigestionArgsInfo.Item.Type.LONG -> ShardDigestion.Union(jsonArgs[it.id]!!.jsonPrimitive.long)
                            ShardDigestionArgsInfo.Item.Type.DOUBLE -> ShardDigestion.Union(jsonArgs[it.id]!!.jsonPrimitive.double)
                            ShardDigestionArgsInfo.Item.Type.BOOLEAN -> ShardDigestion.Union(jsonArgs[it.id]!!.jsonPrimitive.boolean)
                            ShardDigestionArgsInfo.Item.Type.STRING_ARRAY -> ShardDigestion.Union(jsonArgs[it.id]!!.jsonArray.map { a -> a.jsonPrimitive.content }
                                .toTypedArray())

                            ShardDigestionArgsInfo.Item.Type.INT_ARRAY -> ShardDigestion.Union(jsonArgs[it.id]!!.jsonArray.map { a -> a.jsonPrimitive.int }
                                .toTypedArray())

                            ShardDigestionArgsInfo.Item.Type.LONG_ARRAY -> ShardDigestion.Union(jsonArgs[it.id]!!.jsonArray.map { a -> a.jsonPrimitive.long }
                                .toTypedArray())

                            ShardDigestionArgsInfo.Item.Type.DOUBLE_ARRAY -> ShardDigestion.Union(jsonArgs[it.id]!!.jsonArray.map { a -> a.jsonPrimitive.double }
                                .toTypedArray())

                            ShardDigestionArgsInfo.Item.Type.BOOLEAN_ARRAY -> ShardDigestion.Union(jsonArgs[it.id]!!.jsonArray.map { a -> a.jsonPrimitive.boolean }
                                .toTypedArray())

                            ShardDigestionArgsInfo.Item.Type.TIMING -> ShardDigestion.Union(jsonArgs[it.id]!!.jsonPrimitive.long)

                            ShardDigestionArgsInfo.Item.Type.POSITION -> ShardDigestion.Union(jsonArgs[it.id]!!.jsonArray.map { a -> a.jsonPrimitive.double }
                                .toTypedArray())

                            ShardDigestionArgsInfo.Item.Type.CHART -> ShardDigestion.Union(
                                Chart.fromAff(jsonArgs[it.id]!!.jsonPrimitive.content)
                            )
                        }
                        shardArgs[it.id] = union
                    } catch (e: NullPointerException) {
                        throw MissingArgumentException("Missing argument (type: ${it.type}) for shard `$id`")
                    }
                }

                val shardDigestion = ShardDigestion(id, localizedName, shardArgs)
                val shardDigestionArgs = shardDigestion.args

                // TODO(cleanup the inactive instances in the pool)
                val shard: AetheriumShard = AetheriumCache.getOrPutInstance(id, clazz.constructors.first().call())

                shard.feed(Genesis.controllerBrand, shardDigestionArgs)
                call.respond(shard.generate().chart)
            } catch (e: Exception) {
                call.respond(AetheriumError(e))
            }
        }
    }
}