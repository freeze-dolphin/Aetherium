package io.sn.aetherium

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.sn.aetherium.implementations.shards.ArcTapJumpShard
import io.sn.aetherium.objects.*
import io.sn.aetherium.objects.exceptions.AetheriumError
import io.sn.aetherium.objects.exceptions.AetheriumHaventConnectException
import io.sn.aetherium.objects.exceptions.MissingArgumentException
import io.sn.aetherium.objects.exceptions.NonAetheriumShardException
import kotlinx.serialization.json.Json
import org.reflections.Reflections
import org.reflections.scanners.Scanners
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
    run { // start loading plugins
        val reflection = Reflections("io.sn.aetherium.implementations.shards")

        val allClasses = reflection.getAll(Scanners.SubTypes).map { Class.forName(it) }
        allClasses.forEach { clazz ->
            if (clazz.kotlin.isSubclassOf(AetheriumShard::class)) {
                val annot = clazz.kotlin.findAnnotations(ShardInfo::class)
                if (annot.isNotEmpty()) {
                    val id = annot.first().id
                    val manual = annot.first().manualLoad

                    if (manual) return@forEach
                    val shard = clazz.constructors.first().newInstance() as AetheriumShard

                    log.info("Registering internal plugin: $id")
                    register(id, shard.javaClass.kotlin, shard.digestionInfo)
                    shard.load()
                }
            }
        }

        // end loading plugins
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
                val shardDigestion = call.receive<ShardDigestion>()
                val shardDigestionArgs = shardDigestion.args

                val (clazz, info) = lookUp(shardDigestion.id)

                if (!info.items.all {
                        shardDigestion.args.containsKey(it.name)
                    }
                ) {
                    throw MissingArgumentException()
                }

                if (clazz.isSubclassOf(AetheriumShard::class)) {
                    val shard: AetheriumShard = clazz.constructors.first().call()
                    shard.init(controllerBrand, shardDigestionArgs)
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