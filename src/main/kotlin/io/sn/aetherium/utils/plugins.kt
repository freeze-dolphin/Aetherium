package io.sn.aetherium.utils

import com.akuleshov7.ktoml.Toml
import io.ktor.server.application.*
import io.sn.aetherium.objects.AetheriumCache
import io.sn.aetherium.objects.AetheriumShard
import io.sn.aetherium.objects.ShardInfo
import io.sn.aetherium.objects.exceptions.InvalidManifestException
import kotlinx.serialization.encodeToString
import java.io.File
import java.util.jar.JarFile
import java.util.jar.Manifest
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotations


fun file(vararg dirs: String): File = dirs.reduce { acc, next ->
    File(acc, next).path
}.let {
    File(it)
}

@Suppress("UNCHECKED_CAST")
fun loadClass(jarFile: File, clazzPath: String): Class<out AetheriumShard> {
    try {
        val loader = ClasspathHacker.add(jarFile.toPath())
        val rst = loader.loadClass(clazzPath) as Class<out AetheriumShard>
        return rst
    } catch (e: Exception) {
        throw e
    }
}


fun readAetheriumEntry(jarfile: JarFile): String {
    lateinit var manifest: Manifest
    try {
        manifest = jarfile.manifest

        return if (manifest.mainAttributes?.getValue("Aetherium-Entry") != null) {
            manifest.mainAttributes.getValue("Aetherium-Entry")
        } else {
            throw InvalidManifestException("The shard '${jarfile.name}' contains no valid manifest, ignored")
        }
    } catch (e: Exception) {
        throw e
    }
}

fun Application.loadPlugin(clazz: KClass<out AetheriumShard>): PluginLoadState {
    try {
        val annot = clazz.findAnnotations(ShardInfo::class)

        if (annot.isNotEmpty()) {
            val shardInfo = annot.first()
            val id = shardInfo.id
            val manual = shardInfo.manualLoad

            if (manual) return PluginLoadState.MANUAL
            val shard: AetheriumShard = AetheriumCache.getOrPutInstance(id, clazz.constructors.first().call())

            val chinternal = if (shard.isInternal) "internal " else " "

            log.info("Registering ${chinternal}plugin: $id")

            // TODO(there should be an active detection of id conflicting)
            AetheriumCache.register(id, shard.javaClass.kotlin, shard.digestionInfo)

            file(".", "config", "internal").let {
                if (!it.exists()) it.mkdirs()
            }

            if (!shardInfo.hasConfig) {
                if (!shard.inited) shard.init(id, null)
            } else {
                val configFilePath = if (shard.isInternal) {
                    file(".", "config", "internal", "${id}.toml")
                } else {
                    file(".", "config", "${id}.toml")
                }
                val configFile = configFilePath.apply {
                    if (!this.exists()) {
                        this.createNewFile()
                        this.writeText(Toml.encodeToString(mapOf<String, String>()))
                    }
                }
                if (!shard.inited) shard.init(id, configFile)
            }
            shard.onGenesis()
            return PluginLoadState.SUCCESSFUL
        } else {
            return PluginLoadState.IGNORED
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return PluginLoadState.FAILED
    }
}

enum class PluginLoadState {
    MANUAL, SUCCESSFUL, FAILED, IGNORED
}

data class PluginLoadCounter(
    private var pluginCounterInternal: Int = 0,
    private var pluginCounterExternal: Int = 0,
    private var pluginCounterInternalErr: Int = 0,
    private var pluginCounterExternalErr: Int = 0,
    private var pluginCounterInternalMan: Int = 0,
    private var pluginCounterExternalMan: Int = 0
) {
    fun add(state: PluginLoadState, isInternal: Boolean) {
        when (state) {
            PluginLoadState.MANUAL -> if (isInternal) pluginCounterInternalMan++ else pluginCounterExternalMan++
            PluginLoadState.SUCCESSFUL -> if (isInternal) pluginCounterInternal++ else pluginCounterExternal++
            PluginLoadState.FAILED -> if (isInternal) pluginCounterInternalErr++ else pluginCounterExternalErr++
            PluginLoadState.IGNORED -> {}
        }
    }

    private fun totalSuccessful(): Int {
        return pluginCounterInternal + pluginCounterExternal
    }

    val logInfo: Application.() -> Unit = {
        val total = totalSuccessful()
        val pl = if (total == 1) "" else "s"
        log.info("")
        log.info("Done! (with $total plugin${pl} loaded)")
        log.info(" ├ Internal: [Load: $pluginCounterInternal | Err: $pluginCounterInternalErr | Manual: $pluginCounterInternalMan]")
        log.info(" └ External: [Load: $pluginCounterExternal | Err: $pluginCounterExternalErr | Manual: $pluginCounterExternalMan]")
        log.info("")
    }

}