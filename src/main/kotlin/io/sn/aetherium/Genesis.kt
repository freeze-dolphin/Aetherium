package io.sn.aetherium

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.sn.aetherium.objects.*
import io.sn.aetherium.objects.exceptions.AetheriumError
import io.sn.aetherium.objects.exceptions.AetheriumHaventConnectException
import io.sn.aetherium.objects.exceptions.MissingArgumentException
import io.sn.aetherium.objects.exceptions.NonAetheriumShardException
import io.sn.aetherium.utils.*
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

    run { // done with loading
        counter.logInfo()
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

                val (clazz, info) = lookUpShard(id)

                // Assign parameter
                val shardArgs = mutableMapOf<String, ShardDigestion.Union>()
                info.items.forEach {
                    val union = ShardDigestion.Union()
                    when (it.type) {
                        ShardDigestionArgsInfo.Item.Type.STRING -> union.string = jsonArgs[it.name]!!.jsonPrimitive.content
                        ShardDigestionArgsInfo.Item.Type.INT -> union.int = jsonArgs[it.name]!!.jsonPrimitive.int
                        ShardDigestionArgsInfo.Item.Type.LONG -> union.long = jsonArgs[it.name]!!.jsonPrimitive.long
                        ShardDigestionArgsInfo.Item.Type.DOUBLE -> union.double = jsonArgs[it.name]!!.jsonPrimitive.double
                        ShardDigestionArgsInfo.Item.Type.BOOLEAN -> union.boolean = jsonArgs[it.name]!!.jsonPrimitive.boolean
                        ShardDigestionArgsInfo.Item.Type.STRING_LIST -> union.stringList =
                            jsonArgs[it.name]!!.jsonArray.map { a -> a.jsonPrimitive.content }

                        ShardDigestionArgsInfo.Item.Type.INT_LIST -> union.intList =
                            jsonArgs[it.name]!!.jsonArray.map { a -> a.jsonPrimitive.int }

                        ShardDigestionArgsInfo.Item.Type.LONG_LIST -> union.longList =
                            jsonArgs[it.name]!!.jsonArray.map { a -> a.jsonPrimitive.long }

                        ShardDigestionArgsInfo.Item.Type.DOUBLE_LIST -> union.doubleList =
                            jsonArgs[it.name]!!.jsonArray.map { a -> a.jsonPrimitive.double }

                        ShardDigestionArgsInfo.Item.Type.BOOLEAN_LIST -> union.booleanList =
                            jsonArgs[it.name]!!.jsonArray.map { a -> a.jsonPrimitive.boolean }
                    }
                    shardArgs[it.name] = union
                }

                val shardDigestion = ShardDigestion(id, shardArgs)
                val shardDigestionArgs = shardDigestion.args

                // Check if there's any missing key
                if (!info.items.all {
                        shardDigestion.args.containsKey(it.name)
                    }) {
                    throw MissingArgumentException()
                }

                // TODO(cleanup the inactive instances in the pool)
                val shard: AetheriumShard = getOrPutInstance(id, clazz.constructors.first().call())

                shard.feed(shardDigestionArgs)
                call.respond(shard.generate().chart)
            } catch (e: Exception) {
                call.respond(AetheriumError(e))
            }
        }
    }
}