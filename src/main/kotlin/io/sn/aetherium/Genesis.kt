package io.sn.aetherium

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.sn.aetherium.objects.*
import io.sn.aetherium.objects.exceptions.*
import io.sn.aetherium.utils.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import java.util.jar.JarFile
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.isSubclassOf
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

val json = Json {
    prettyPrint = false
    isLenient = false
}

var controllerBrand = ControllerBrand("Unknown", "0.0")

@Suppress("unused")
fun Application.module() {
    var pluginCounterInternal = 0
    var pluginCounterExternal = 0
    var pluginCounterInternalErr = 0
    var pluginCounterExternalErr = 0
    var pluginCounterInternalMan = 0
    var pluginCounterExternalMan = 0

    run { // load internal plugins
        file(".", "plugins", "internal").let {
            if (!it.exists()) it.mkdirs()
        }
        val reflection = Reflections("io.sn.aetherium.implementations.shards")

        val allClasses = reflection.getAll(Scanners.SubTypes).map { Class.forName(it) }
        allClasses.forEach { clazz ->
            try {
                if (clazz.kotlin.isSubclassOf(AetheriumShard::class) && clazz.kotlin.findAnnotations(ShardInfo::class).isNotEmpty()) {
                    @Suppress("UNCHECKED_CAST")
                    when (loadPlugin(clazz.kotlin as KClass<out AetheriumShard>)) {
                        PluginLoadState.MANUAL -> {
                            pluginCounterInternalMan++
                        }

                        PluginLoadState.SUCCESSFUL -> {
                            pluginCounterInternal++
                        }

                        PluginLoadState.FAILED -> {
                            pluginCounterInternalErr++
                        }

                        else -> {}
                    }
                }
            } catch (e: Exception) {
                log.error(e.stackTraceToString())
                pluginCounterInternalErr++
                return@forEach
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
                    when (loadPlugin(entryClass)) {
                        PluginLoadState.MANUAL -> {
                            pluginCounterExternalMan++
                        }

                        PluginLoadState.SUCCESSFUL -> {
                            pluginCounterExternal++
                        }

                        PluginLoadState.FAILED -> {
                            pluginCounterExternalErr++
                        }

                        else -> {}
                    }
                }
            } catch (e: Exception) {
                log.error(e.stackTraceToString())
                pluginCounterExternalErr++
                return@forEach
            }
        }
    }

    run { // done with loading
        val total = pluginCounterInternal + pluginCounterExternal
        val pl = if (total == 1) "" else "s"
        log.info("")
        log.info("Done! (with $total plugin${pl} loaded)")
        log.info(" ├ Internal: [Load: $pluginCounterInternal | Err: $pluginCounterInternalErr | Manual: $pluginCounterInternalMan]")
        log.info(" └ External: [Load: $pluginCounterExternal | Err: $pluginCounterExternalErr | Manual: $pluginCounterExternalMan]")
        log.info("")
    }

    install(ContentNegotiation) {
        json(json)
    }
    routing {
        get("/destroy") {
            call.respondText("See you next time. ;)")
            exitProcess(0)
        }
        get("/connect") {
            val ua = (call.request.headers["User-Agent"]) ?: "Unknown/0.0"

            val (controllerName, controllerVersion) = Regex("""(\w+)/([\d.]+)""").find(ua)!!.destructured

            controllerBrand = ControllerBrand(controllerName, controllerVersion)
            call.respondText("Connected!\n\nController Brand:\t${controllerName}\nController Version:\t${controllerVersion}\n")
        }
        post("/generate") {
            try {
                if (controllerBrand.name == "Unknown") throw AetheriumHaventConnectException("Please connect first to submit controller brand")
                val recv = json.parseToJsonElement(call.receiveText()).jsonObject
                val id = recv["id"]!!.jsonPrimitive.content
                val jsonArgs = recv["args"]!!.jsonObject

                val (clazz, info) = lookUp(id)

                val shardArgs = mutableMapOf<String, ShardDigestion.Union>()
                info.items.forEach {
                    val union = ShardDigestion.Union()
                    when (it.type) {
                        ShardDigestionArgsInfo.Item.Type.STRING ->
                            union.string = jsonArgs[it.name]!!.jsonPrimitive.content

                        ShardDigestionArgsInfo.Item.Type.INT ->
                            union.int = jsonArgs[it.name]!!.jsonPrimitive.int

                        ShardDigestionArgsInfo.Item.Type.LONG ->
                            union.long = jsonArgs[it.name]!!.jsonPrimitive.long

                        ShardDigestionArgsInfo.Item.Type.DOUBLE ->
                            union.double = jsonArgs[it.name]!!.jsonPrimitive.double

                        ShardDigestionArgsInfo.Item.Type.BOOLEAN ->
                            union.boolean = jsonArgs[it.name]!!.jsonPrimitive.boolean

                        ShardDigestionArgsInfo.Item.Type.STRING_LIST ->
                            union.stringList = jsonArgs[it.name]!!.jsonArray.map { a ->
                                a.jsonPrimitive.content
                            }

                        ShardDigestionArgsInfo.Item.Type.INT_LIST ->
                            union.intList = jsonArgs[it.name]!!.jsonArray.map { a ->
                                a.jsonPrimitive.int
                            }

                        ShardDigestionArgsInfo.Item.Type.LONG_LIST ->
                            union.longList = jsonArgs[it.name]!!.jsonArray.map { a ->
                                a.jsonPrimitive.long
                            }

                        ShardDigestionArgsInfo.Item.Type.DOUBLE_LIST ->
                            union.doubleList = jsonArgs[it.name]!!.jsonArray.map { a ->
                                a.jsonPrimitive.double
                            }

                        ShardDigestionArgsInfo.Item.Type.BOOLEAN_LIST ->
                            union.booleanList = jsonArgs[it.name]!!.jsonArray.map { a ->
                                a.jsonPrimitive.boolean
                            }
                    }
                    shardArgs[it.name] = union
                }

                val shardDigestion = ShardDigestion(id, shardArgs)
                val shardDigestionArgs = shardDigestion.args

                if (!info.items.all {
                        shardDigestion.args.containsKey(it.name)
                    }
                ) {
                    throw MissingArgumentException()
                }

                if (clazz.isSubclassOf(AetheriumShard::class)) {
                    val shard: AetheriumShard = clazz.constructors.first().call()
                    val configFilePath = if (shard.isInternal) {
                        file(".", "plugins", "internal", "${id}.json")
                    } else {
                        file(".", "plugins", "${id}.json")
                    }
                    val configFile = configFilePath.apply {
                        if (!this.exists()) {
                            this.createNewFile()
                            this.writeText(Json.encodeToString(mapOf<String, String>()))
                        }
                    }

                    if (!shard.inited) shard.init(
                        id,
                        controllerBrand,
                        shardDigestionArgs,
                        configFile
                    )
                    call.respond(shard.generate().chart)
                } else {
                    throw NonAetheriumShardException("This is not a runnable Aetherium shard")
                }
            } catch (e: Exception) {
                call.respond(AetheriumError(e))
            }
        }
    }
}