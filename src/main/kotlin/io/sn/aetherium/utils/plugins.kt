package io.sn.aetherium.utils

import io.ktor.server.application.*
import io.sn.aetherium.objects.AetheriumShard
import io.sn.aetherium.objects.ShardInfo
import io.sn.aetherium.objects.exceptions.InvalidManifestException
import io.sn.aetherium.objects.register
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
        with(jarfile) {
            manifest = this.manifest
        }

        val entry = if (manifest.mainAttributes?.getValue("Aetherium-Entry") != null) {
            manifest.mainAttributes.getValue("Aetherium-Entry")
        } else {
            throw InvalidManifestException("This shard contains invalid manifest, ignored")
        }

        return entry
    } catch (e: Exception) {
        throw e
    }
}

fun Application.loadPlugin(clazz: KClass<out AetheriumShard>): PluginLoadState {
    try {
        val annot = clazz.findAnnotations(ShardInfo::class)
        val id = annot.first().id
        val manual = annot.first().manualLoad


        if (annot.isNotEmpty()) {
            if (manual) return PluginLoadState.MANUAL
            val shard = clazz.constructors.first().call()

            log.info("Registering internal plugin: $id")
            register(id, shard.javaClass.kotlin, shard.digestionInfo)
            shard.onRegister()
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
