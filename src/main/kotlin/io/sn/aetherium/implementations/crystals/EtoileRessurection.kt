package io.sn.aetherium.implementations.crystals

import com.charleskorn.kaml.*
import com.tairitsu.compose.arcaea.Chart
import com.tairitsu.compose.arcaea.LocalizedString
import io.sn.aetherium.implementations.crystals.ArcpkgEntryType.PACK
import io.sn.aetherium.implementations.crystals.ArcpkgEntryType.valueOf
import io.sn.aetherium.utils.file
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.awt.Color
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.imageio.ImageIO
import kotlin.properties.Delegates
import kotlin.system.exitProcess

@Serializable
data class Songlist(
    val songs: List<SonglistEntry>
)

@Serializable
data class Packlist(
    val packs: List<PacklistEntry>
)

@Serializable
data class PacklistEntry(
    val id: String,
    val section: String = "sidestory",
    @SerialName("plus_character") val plusCharacter: Int,
    @SerialName("is_extend_pack") val isExtendPack: Boolean,
    @SerialName("custom_banner") val customBanner: Boolean,
    @SerialName("name_localized") val nameLocalized: LocalizedString,
    @SerialName("description_localized") val descriptionLocalized: LocalizedString
) {
    companion object {
        fun fromDefaultPacklistEntry(id: String, nameLocalized: LocalizedString, descriptionLocalized: LocalizedString) =
            PacklistEntry(
                id = id,
                plusCharacter = -1,
                isExtendPack = true,
                customBanner = false,
                nameLocalized = nameLocalized,
                descriptionLocalized = descriptionLocalized
            )
    }
}

class ArcpkgEntryTypeSerializer : KSerializer<ArcpkgEntryType> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("ArcpkgEntryType", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ArcpkgEntryType {
        return valueOf(decoder.decodeString().uppercase())
    }

    override fun serialize(encoder: Encoder, value: ArcpkgEntryType) {
        throw IllegalStateException("Not implemented")
    }

}

@Serializable(ArcpkgEntryTypeSerializer::class)
enum class ArcpkgEntryType {
    LEVEL, PACK;
}

@Serializable
data class IndexEntry(
    val directory: String, val identifier: String, val settingsFile: String, val version: Int = -1, val type: ArcpkgEntryType
)

@Serializable
data class DifficultyEntry(
    @Transient var audioPath: String? = null,
    @Transient var jacketPath: String? = null,
    @Transient var chartPath: String? = null,

    val ratingClass: Int,
    val chartDesigner: String,
    val jacketDesigner: String,
    val rating: Int,

    var ratingPlus: Boolean? = null,
    @SerialName("title_localized") var titleLocalized: LocalizedString? = null,
    var artist: String? = null,
    @SerialName("bpm") var bpmText: String? = null,
    @SerialName("bpm_base") var bpmBase: Float? = null,
    var jacketOverride: Boolean? = null,
    var audioOverride: Boolean? = null,
    var audioPreview: Long? = null,
    var audioPreviewEnd: Long? = null,
    var side: Int? = null,
    var bg: String? = null,
)

@Serializable
data class SonglistEntry(
    val id: String,
    @SerialName("title_localized") val titleLocalized: LocalizedString,
    val artist: String,
    @SerialName("bpm") val bpmText: String,
    @SerialName("bpm_base") val bpmBase: Float,
    val set: String,
    val purchase: String,
    val audioPreview: Long,
    val audioPreviewEnd: Long,
    val side: Int,
    val bg: String,
    val date: Long,
    val version: String,
    val difficulties: List<DifficultyEntry>
)

enum class ExportBgMode {
    SIMPLIFIED, PRECISE, OVERWRITE, AUTO_RENAME
}

data class ExportConfiguration(
    val exportSet: String = "single",
    val exportVersion: String = "1.0",
    val exportTime: Long = System.currentTimeMillis() / 1000L,
    val exportDirectory: File = file(".", "result"),
    val exportBgMode: ExportBgMode = ExportBgMode.SIMPLIFIED,
)

class ArcpkgConvert(
    private val arcpkgs: Array<String>,
    private val identifierPrefix: String,
    private val exportConfiguration: ExportConfiguration = ExportConfiguration()
) {

    private val json = Json {
        prettyPrint = true
    }

    private val yaml = Yaml.default

    companion object {

        fun readFileFromZip(zipFile: ZipFile, fileName: String): String {
            val entry: ZipEntry = zipFile.getEntry(fileName)
            val content = StringBuilder()

            zipFile.getInputStream(entry).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        content.append(line + System.lineSeparator())
                    }
                }
            }

            return content.toString()
        }

        fun extractFileFromZipPrecise(
            zipFile: ZipFile,
            fileName: String,
            destDir: File,
            overrideFileName: String? = null,
            resizeToBg1080p: Boolean = false
        ): Int {
            val zipEntry = zipFile.getEntry(fileName)

            if (zipEntry != null) {
                val exportFileName = overrideFileName ?: unifyBgName(File(fileName).name)
                val outputFile = File(destDir, exportFileName)
                if (!outputFile.parentFile.exists()) outputFile.parentFile.mkdirs()
                extractFileFromZip(zipFile, zipEntry, fileName, outputFile, resizeToBg1080p)
                return 0
            } else {
                return 1
            }
        }

        fun extractFileFromZipSimplified(
            zipFile: ZipFile,
            fileName: String,
            destDir: File,
            overrideFileName: String? = null,
            resizeToBg1080p: Boolean = false
        ): Int {
            val zipEntry = zipFile.getEntry(fileName)
            var rt = 0

            if (zipEntry != null) {
                val exportFileName = overrideFileName ?: unifyBgName(File(fileName).name)
                val outputFile = File(destDir, exportFileName)
                if (outputFile.exists()) {
                    rt = 2
                } else {
                    extractFileFromZip(zipFile, zipEntry, fileName, outputFile, resizeToBg1080p)
                }
            } else {
                rt = 1
            }
            return rt
        }

        fun extractFileFromZipOverwrite(
            zipFile: ZipFile,
            fileName: String,
            destDir: File,
            overrideFileName: String? = null,
            resizeToBg1080p: Boolean = false
        ): Int {
            val zipEntry = zipFile.getEntry(fileName)
            var rt = 0

            if (zipEntry != null) {
                val exportFileName = overrideFileName ?: unifyBgName(File(fileName).name)
                val outputFile = File(destDir, exportFileName)
                if (outputFile.exists()) {
                    rt = 2
                }
                extractFileFromZip(zipFile, zipEntry, fileName, outputFile, resizeToBg1080p)
            } else {
                rt = 1
            }
            return rt
        }

        fun extractFileFromZipAutoRename(
            zipFile: ZipFile,
            fileName: String,
            destDir: File,
            identifier: String,
            resizeToBg1080p: Boolean = false
        ): Pair<Int, String> {
            val renamed = unifyBgName(identifier + "_" + File(fileName).name)
            return extractFileFromZipOverwrite(zipFile, fileName, destDir, renamed, resizeToBg1080p) to renamed
        }

        private fun extractFileFromZip(
            zipFile: ZipFile,
            zipEntry: ZipEntry,
            fileName: String,
            outputFile: File,
            resizeToBg1080p: Boolean = false
        ) {
            zipFile.getInputStream(zipEntry).use { input ->
                FileOutputStream(outputFile).use { output ->
                    if (resizeToBg1080p) {
                        val img = ImageIO.read(input)
                        val width = 1920
                        val height = 1440

                        val newBufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
                        val resizedImg = resizeImage(img, 1920, 1440)

                        newBufferedImage.createGraphics().apply {
                            drawImage(resizedImg, 0, 0, Color.WHITE, null)
                            dispose()
                        }

                        if (!ImageIO.write(newBufferedImage, "jpg", output)) {
                            throw IllegalStateException("Unable to find a writer for file: ${outputFile.path}")
                        }

                        null
                    } else if (fileName.endsWith(".png") && outputFile.name.endsWith(".jpg")) {
                        val img = ImageIO.read(input)
                        val width = img.width
                        val height = img.height

                        val newBufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

                        newBufferedImage.createGraphics().apply {
                            drawImage(img, 0, 0, Color.WHITE, null)
                            dispose()
                        }

                        if (!ImageIO.write(newBufferedImage, "jpg", output)) {
                            throw IllegalStateException("Unable to find a writer for file: ${outputFile.path}")
                        }

                        null
                    } else {
                        input.copyTo(output)
                    }
                }
            }
        }


        infix fun YamlNode.content(path: String): String {
            return this.yamlMap.get<YamlNode>(path)!!.yamlScalar.content
        }

        infix fun YamlNode.nullableContent(path: String): String? {
            return this.yamlMap.get<YamlNode>(path)?.yamlScalar?.content
        }

        private val ratingClassRegex = Regex("""\b\d\b""")

        fun matchRatingClass(difficultyText: String): MatchResult? {
            return ratingClassRegex.find(difficultyText)
        }

        fun unifyBgName(raw: String): String {
            return raw.replace('(', '_')
                .replace(' ', '_')
                .replace(')', '_')
                .replace('\'', '_')
                .replace(".png", ".jpg")
                .lowercase()
        }

        private val unityRichTextTags = listOf(
            "align", "allcaps", "alpha", "b", "br", "color", "cspace", "font", "font-weight", "gradient", "i",
            "indent", "line-height", "line-indent", "link", "lowercase", "margin", "mark", "mspace", "nobr",
            "noparse", "page", "pos", "rotate", "s", "size", "smallcaps", "space", "sprite", "strikethrough",
            "style", "sub", "sup", "u", "uppercase", "voffset", "width", "color"
        )

        private val regexPattern = unityRichTextTags.joinToString(separator = "|", prefix = "</?(", postfix = ")=?[^>]*?>").toRegex()


        fun removeUnityRichTextTags(input: String): String {
            return input.replace(regexPattern, "")
        }

        fun resizeImage(originalImage: BufferedImage, targetWidth: Int, targetHeight: Int): BufferedImage {
            val resultingImage: Image = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH)
            val outputImage = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
            val g = outputImage.graphics
            g.drawImage(resultingImage, 0, 0, null)
            g.dispose()
            return outputImage
        }

    }

    private val exportDirBg = file(exportConfiguration.exportDirectory.path, "img", "bg")
    private val exportDirSongs = file(exportConfiguration.exportDirectory.path, "songs")
    private val exportDirPack = file(exportDirSongs.path, "pack")

    private val exportSonglist = file(exportDirSongs.path, "songlist")
    private val exportPacklist = file(exportDirSongs.path, "packlist")

    private fun createDirs() {
        if (!exportDirBg.exists()) exportDirBg.mkdirs()
        if (!exportDirSongs.exists()) exportDirSongs.mkdirs()
        if (!exportDirPack.exists()) exportDirPack.mkdirs()
    }

    private fun processSongs(
        logPrefix: String,
        index: List<IndexEntry>,
        arcpkgZipFile: ZipFile,
        songlist: MutableList<SonglistEntry>,
        set: String
    ) {
        index.forEachIndexed { procIdx, entry ->
            val procTextRaw = "[${procIdx + 1}/${index.size}]"
            val procText = logPrefix + procTextRaw
            val procIndent = logPrefix + " ".repeat(procTextRaw.length)
            val settings = readFileFromZip(arcpkgZipFile, "${entry.directory}/${entry.settingsFile}").let { yaml.parseToYamlNode(it) }

            val charts = settings.yamlMap.get<YamlList>("charts")!!

            val difficulties = mutableListOf<DifficultyEntry>()
            lateinit var baseDifficulty: DifficultyEntry

            val id = entry.identifier.removePrefix(identifierPrefix)
            val idName = "[id=${entry.identifier}]"

            println("$procText Processing: ${entry.directory} $idName")

            charts.items.forEach { chart ->
                val difficultyText = chart content "difficulty"

                val ratingClassRaw = chart content "chartPath"
                var ratingClassDigit: Int? = null
                var ratingClass by Delegates.notNull<Int>()
                if (ratingClassRaw.endsWith(".aff") && ratingClassRaw.removeSuffix(".aff").toIntOrNull()
                        .also { ratingClassDigit = it } != null && (0..4).contains(ratingClassDigit!!)
                ) {
                    ratingClass = ratingClassDigit!!
                } else {
                    ratingClass = when {
                        difficultyText.startsWith("Eternal") -> 4
                        difficultyText.startsWith("Beyond") -> 3
                        difficultyText.startsWith("Future") -> 2
                        difficultyText.startsWith("Present") -> 1
                        difficultyText.startsWith("Past") -> 0

                        else -> throw IllegalStateException("Unable to detect ratingClass $idName")
                    }
                }
                val audioPath = chart content "audioPath"
                val jacketPath = chart content "jacketPath"
                val baseBpm = (chart content "baseBpm").toFloat()
                val bpmText = chart content "bpmText"
                val title = LocalizedString(chart content "title")
                val artist = (chart nullableContent "composer") ?: ""
                val charter = removeUnityRichTextTags((chart nullableContent "alias") ?: "")
                val jacketDesigner = (chart nullableContent "illustrator") ?: ""

                var rating by Delegates.notNull<Int>()
                var ratingPlus by Delegates.notNull<Boolean>()
                if (chart.yamlMap.getKey("chartConstant") != null) {
                    val constant = (chart content "chartConstant").toDouble()
                    rating = constant.toInt()
                    ratingPlus = constant - rating >= 0.7
                } else {
                    val matchRst = matchRatingClass(difficultyText)
                        ?: throw IllegalStateException("Invalid difficulty information for ${entry.directory}/${entry.settingsFile} $idName")
                    rating = matchRst.value.toInt()
                    ratingPlus = difficultyText.endsWith("+")
                }

                var sideString: String
                val side = chart.yamlMap.get<YamlMap>("skin").let { skin ->
                    if (skin == null) {
                        sideString = "light"
                        0
                    } else {
                        val side = skin.get<YamlNode>("side")
                        sideString = side?.yamlScalar?.content ?: "light"
                        when (sideString) {
                            "light" -> 0
                            "conflict" -> 1
                            "colorless" -> 2
                            else -> {
                                println("$procIndent Unable to parse side: $sideString, defaulting to 0")
                                0
                            }
                        }
                    }
                }

                var extractBg = true

                val bgRaw: String? = chart nullableContent "backgroundPath"
                var bg: String
                val bgBundled = when {
                    sideString == "colorless" -> "epilogue"
                    else -> "base_$sideString"
                }

                if (bgRaw == null) {
                    extractBg = false
                    bg = bgBundled
                } else {
                    bg = unifyBgName(bgRaw.let {
                        if (it.endsWith(".jpg")) {
                            it.removeSuffix(".jpg")
                        } else if (it.endsWith(".png")) {
                            it.removeSuffix(".png")
                        } else {
                            throw IllegalStateException("Invalid background image: $it $idName")
                        }
                    })
                }

                val audioPreview: Long = chart.yamlMap.get<YamlNode>("previewStart")?.yamlScalar?.content?.toLong() ?: 0
                val audioPreviewEnd: Long = chart.yamlMap.get<YamlNode>("previewEnd")?.yamlScalar?.content?.toLong() ?: 0

                val difficultyEntry = DifficultyEntry(
                    audioPath = audioPath,
                    jacketPath = jacketPath,
                    chartPath = ratingClassRaw,

                    ratingClass = ratingClass,
                    chartDesigner = charter,
                    jacketDesigner = jacketDesigner,
                    rating = rating,
                )

                if (extractBg) {
                    val bgPath = "${entry.directory}/$bgRaw"
                    println("$procIndent Extracting bg: $bgPath")

                    when (exportConfiguration.exportBgMode) {
                        ExportBgMode.SIMPLIFIED -> {
                            val rt = extractFileFromZipSimplified(arcpkgZipFile, bgPath, exportDirBg, null, true)
                            when (rt) {
                                2 -> println("$procIndent | Already exists: ${file(exportDirBg.path, File(bgPath).name).path}, ignoring...")
                                1 -> {
                                    println("$procIndent | Unable to extract: $bgPath, alter to use bundled bg: $bgBundled")
                                    bg = bgBundled
                                }
                            }
                        }

                        ExportBgMode.PRECISE -> {
                            if (extractFileFromZipPrecise(arcpkgZipFile, bgPath, exportDirBg, null, true) != 0) {
                                println("$procIndent | Unable to extract: $bgPath, alter to use bundled bg: $bgBundled")
                                bg = bgBundled
                            }
                        }

                        ExportBgMode.OVERWRITE -> {
                            val rt = extractFileFromZipOverwrite(arcpkgZipFile, bgPath, exportDirBg, null, true)
                            when (rt) {
                                2 -> println(
                                    "$procIndent | Already exists: ${
                                        file(
                                            exportDirBg.path,
                                            File(bgPath).name
                                        ).path
                                    }, overwriting..."
                                )

                                1 -> {
                                    println("$procIndent | Unable to extract: $bgPath, alter to use bundled bg: $bgBundled")
                                    bg = bgBundled
                                }
                            }
                        }

                        ExportBgMode.AUTO_RENAME -> {
                            val (rt, bgQualified) = extractFileFromZipAutoRename(
                                arcpkgZipFile,
                                bgPath,
                                exportDirBg,
                                entry.identifier,
                                true
                            )
                            when (rt) {
                                2 -> {
                                    println(
                                        "$procIndent | Already exists: ${
                                            file(
                                                exportDirBg.path,
                                                File(bgPath).name
                                            ).path
                                        }, overwriting..."
                                    )
                                    bg = bgQualified
                                }

                                1 -> {
                                    println("$procIndent | Unable to extract: $bgPath, alter to use bundled bg: $bgBundled")
                                    bg = bgBundled
                                }

                                0 -> {
                                    bg = bgQualified
                                }

                            }
                        }
                    }
                } else {
                    println("$procIndent Using bundled bg: $bg")
                }

                if (bg != bgBundled) bg = bg.removeSuffix(".jpg")

                if (ratingPlus) difficultyEntry.ratingPlus = true

                if (difficulties.size == 0) {
                    baseDifficulty = difficultyEntry.copy(
                        titleLocalized = title,
                        artist = artist,
                        bpmText = bpmText,
                        bpmBase = baseBpm,
                        side = side,
                        audioPreview = audioPreview,
                        audioPreviewEnd = audioPreviewEnd,
                        bg = bg
                    )
                } else {
                    if (title != baseDifficulty.titleLocalized) difficultyEntry.titleLocalized = title
                    if (artist != baseDifficulty.artist) difficultyEntry.artist = artist
                    if (bpmText != baseDifficulty.bpmText) difficultyEntry.bpmText = bpmText
                    if (baseBpm != baseDifficulty.bpmBase) difficultyEntry.bpmBase = baseBpm
                    if (side != baseDifficulty.side) difficultyEntry.side = side
                    if (audioPath != baseDifficulty.audioPath) difficultyEntry.audioOverride = true
                    if (jacketPath != baseDifficulty.jacketPath) difficultyEntry.jacketOverride = true
                    if (audioPreview != baseDifficulty.audioPreview) difficultyEntry.audioPreview = audioPreview
                    if (audioPreviewEnd != baseDifficulty.audioPreviewEnd) difficultyEntry.audioPreviewEnd = audioPreviewEnd
                    if (bg != baseDifficulty.bg) difficultyEntry.bg = bg
                }

                difficulties.add(difficultyEntry)
            }

            val songDir = File(exportDirSongs, id)
            if (!songDir.exists()) songDir.mkdirs()

            extractFileFromZipOverwrite(arcpkgZipFile, "${entry.directory}/${baseDifficulty.audioPath}", songDir, "base.ogg")
            extractFileFromZipOverwrite(arcpkgZipFile, "${entry.directory}/${baseDifficulty.jacketPath}", songDir, "base.jpg")

            val fileBase = File(songDir, "base.jpg")
            val origImg = ImageIO.read(fileBase)
            var is1080 = false
            val (resized256, resizedBase) = if (origImg.width >= 768) {
                is1080 = true
                resizeImage(origImg, 384, 384) to resizeImage(origImg, 768, 768)
            } else {
                resizeImage(origImg, 256, 256) to resizeImage(origImg, 512, 512)
            }

            val imgPrefix = if (is1080) "1080_" else ""

            ImageIO.write(resized256, "jpg", File(songDir, "${imgPrefix}base_256.jpg"))
            if (is1080) {
                ImageIO.write(resizedBase, "jpg", File(songDir, "${imgPrefix}base.jpg"))
                fileBase.delete()
            }

            difficulties.forEach { diffEntry ->
                val aff = readFileFromZip(arcpkgZipFile, "${entry.directory}/${diffEntry.chartPath!!}")
                File(songDir, "${diffEntry.ratingClass}.aff").writeText(Chart.fromAcf(aff).first.serializeForArcaea())
            }

            (0..2).forEach { ratingClass ->
                if (difficulties.none {
                        it.ratingClass == ratingClass
                    }) {
                    difficulties.add(
                        DifficultyEntry(
                            ratingClass = ratingClass,
                            chartDesigner = "",
                            jacketDesigner = "",
                            rating = -1
                        )
                    )
                }
            }

            difficulties.sortBy {
                it.ratingClass
            }

            songlist.add(
                SonglistEntry(
                    id = id,
                    titleLocalized = baseDifficulty.titleLocalized!!,
                    artist = baseDifficulty.artist!!,
                    bpmText = baseDifficulty.bpmText!!,
                    bpmBase = baseDifficulty.bpmBase!!,
                    set = set,
                    purchase = "",
                    audioPreview = baseDifficulty.audioPreview!!,
                    audioPreviewEnd = baseDifficulty.audioPreviewEnd!!,
                    side = baseDifficulty.side!!,
                    bg = baseDifficulty.bg!!,
                    date = exportConfiguration.exportTime,
                    version = exportConfiguration.exportVersion,
                    difficulties = difficulties
                )
            )
        }
    }

    fun exec() {
        createDirs()

        val songlist = mutableListOf<SonglistEntry>()
        val packlist = mutableListOf<PacklistEntry>()

        arcpkgs.forEach { arcpkgFilename ->
            val zipFile = ZipFile(arcpkgFilename)
            val index = readFileFromZip(zipFile, "index.yml").let { yaml.decodeFromString(ListSerializer(IndexEntry.serializer()), it) }

            index.filter { entry ->
                entry.type == PACK
            }.let { packEntries ->
                if (packEntries.isEmpty()) {
                    // alter to use `exportSet`
                    processSongs("", index, zipFile, songlist, exportConfiguration.exportSet)
                } else {
                    // batch process for each pack
                    packEntries.forEachIndexed { procIdx, packEntry ->
                        val procText = "{${procIdx + 1}/${packEntries.size}}"
                        val procIndent = " ".repeat(procText.length)
                        val settings = readFileFromZip(
                            zipFile,
                            "${packEntry.directory}/${packEntry.settingsFile}"
                        ).let { yaml.parseToYamlNode(it) }

                        println("$procText Processing pack: ${packEntry.directory} [id=${packEntry.identifier}]")

                        val packCover = settings content "imagePath"
                        val songIds = settings.yamlMap.get<YamlList>("levelIdentifiers")!!.items.map { node ->
                            node.yamlScalar.content
                        }
                        val packName = settings content "packName"
                        val packId = packEntry.identifier.lowercase().replace('.', '_')

                        extractFileFromZipSimplified(zipFile, "${packEntry.directory}/$packCover", exportDirPack, "select_$packId.png")

                        packlist.add(
                            PacklistEntry.fromDefaultPacklistEntry(
                                id = packId,
                                nameLocalized = LocalizedString(packName),
                                descriptionLocalized = LocalizedString("")
                            )
                        )
                        processSongs(
                            "$procIndent ",
                            index.filter { entry ->
                                songIds.contains(entry.identifier)
                            }, zipFile, songlist, packId
                        )
                    }
                }
            }
        }

        json.encodeToString(Songlist.serializer(), Songlist(songlist)).let {
            exportSonglist.writeText(it)
        }

        if (packlist.isNotEmpty()) json.encodeToString(Packlist.serializer(), Packlist(packlist)).let {
            exportPacklist.writeText(it)
        }
    }

}

fun main(args: Array<String>) {
    val usage: Array<String> = arrayOf(
        "Etoile Resurrection",
        "USAGE: java -cp <PATH TO Aetherium.jar> io.sn.aetherium.implementations.crystals.EtoileRessurectionKt <PREFIX> <MODE> [arcpkgs]",
        "Available Modes:",
        " - SIMPLIFIED: extract backgrounds, ignore if existed",
        " - PRECISE: use tree structure",
        " - OVERWRITE: extract backgrounds, overwrite if existed",
        " - AUTO_RENAME: add prefix to filenames to avoid conflicts",
        "The convert result will be in `\$PWD/result/`"
    )

    if (args.size < 3) {
        usage.forEach(System.out::println)
        exitProcess(0)
    }

    val arcpkgs = args.sliceArray(2 until args.size)
    val prefix = args[0]
    val mode = ExportBgMode.valueOf(args[1])
    ArcpkgConvert(arcpkgs, prefix, ExportConfiguration(exportBgMode = mode)).exec()
}
