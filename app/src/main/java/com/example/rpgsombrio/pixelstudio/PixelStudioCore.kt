package com.example.rpgsombrio.pixelstudio

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.zip.CRC32

/** Camada RGBA imutável por API: toda entrada/saída de pixels é copiada. */
class PixelLayer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val visible: Boolean = true,
    val locked: Boolean = false,
    pixels: IntArray = IntArray(PixelDocument.PIXEL_COUNT)
) {
    private val data: IntArray = pixels.copyOf()

    init { require(data.size == PixelDocument.PIXEL_COUNT) { "Cada camada deve conter exatamente 4096 pixels." } }

    fun pixelsCopy(): IntArray = data.copyOf()
    internal fun pixelAt(index: Int): Int = data[index]
    internal fun copyPixels(): IntArray = data.copyOf()

    fun copy(
        id: String = this.id,
        name: String = this.name,
        visible: Boolean = this.visible,
        locked: Boolean = this.locked,
        pixels: IntArray = data
    ) = PixelLayer(id, name, visible, locked, pixels)

    fun duplicate(newName: String = "$name cópia"): PixelLayer = copy(id = UUID.randomUUID().toString(), name = newName)

    override fun equals(other: Any?): Boolean = other is PixelLayer &&
        id == other.id && name == other.name && visible == other.visible && locked == other.locked && data.contentEquals(other.data)

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + visible.hashCode()
        result = 31 * result + locked.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/** Documento de pixel art da V1. A resolução é fixa em 64x64. */
class PixelDocument(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Novo projeto",
    val width: Int = SIZE,
    val height: Int = SIZE,
    layers: List<PixelLayer> = listOf(PixelLayer(name = "Base")),
    palette: List<Int> = PixelPalettes.default
) {
    val layers: List<PixelLayer> = layers.toList()
    val palette: List<Int> = palette.toList()

    init {
        require(width == SIZE && height == SIZE) { "A V1 aceita somente documentos 64x64." }
        require(this.layers.isNotEmpty()) { "O documento precisa ter ao menos uma camada." }
        require(this.layers.size <= MAX_LAYERS) { "Limite de $MAX_LAYERS camadas excedido." }
        require(this.palette.size <= MAX_PALETTE_COLORS) { "Limite de $MAX_PALETTE_COLORS cores excedido." }
    }

    fun copy(
        id: String = this.id,
        name: String = this.name,
        width: Int = this.width,
        height: Int = this.height,
        layers: List<PixelLayer> = this.layers,
        palette: List<Int> = this.palette
    ) = PixelDocument(id, name, width, height, layers, palette)

    /** Composição alpha-over em ordem: primeira camada atrás, última na frente. */
    fun composite(): IntArray {
        val output = IntArray(PIXEL_COUNT)
        layers.filter { it.visible }.forEach { layer ->
            for (i in output.indices) {
                val source = layer.pixelAt(i)
                if ((source ushr 24) != 0) output[i] = alphaOver(output[i], source)
            }
        }
        return output
    }

    fun deepCopy(): PixelDocument = copy(layers = layers.map { it.copy(pixels = it.copyPixels()) }, palette = palette.toList())

    private fun alphaOver(destination: Int, source: Int): Int {
        val sourceAlpha = (source ushr 24) and 0xFF
        if (sourceAlpha == 255) return source
        if (sourceAlpha == 0) return destination
        val destinationAlpha = (destination ushr 24) and 0xFF
        val outAlphaNumerator = sourceAlpha * 255 + destinationAlpha * (255 - sourceAlpha)
        val outAlpha = (outAlphaNumerator + 127) / 255
        if (outAlpha == 0) return 0
        fun channel(shift: Int): Int {
            val s = (source ushr shift) and 0xFF
            val d = (destination ushr shift) and 0xFF
            val numerator = s * sourceAlpha * 255 + d * destinationAlpha * (255 - sourceAlpha)
            return ((numerator + outAlphaNumerator / 2) / outAlphaNumerator).coerceIn(0, 255)
        }
        return (outAlpha shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    override fun equals(other: Any?): Boolean = other is PixelDocument &&
        id == other.id && name == other.name && width == other.width && height == other.height &&
        layers == other.layers && palette == other.palette

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + layers.hashCode()
        result = 31 * result + palette.hashCode()
        return result
    }

    companion object {
        const val SIZE = 64
        const val PIXEL_COUNT = SIZE * SIZE
        const val MAX_LAYERS = 64
        const val MAX_PALETTE_COLORS = 64
    }
}

object PixelPalettes {
    val default: List<Int> = listOf(
        0xFF211A18.toInt(), 0xFFD6A477.toInt(), 0xFF526078.toInt(), 0xFF382A27.toInt(),
        0xFFB64B45.toInt(), 0xFFD2AE68.toInt(), 0xFFEEE0C2.toInt(), 0xFF6E8A67.toInt(),
        0xFF6B4C86.toInt(), 0xFF26364A.toInt(), 0xFF8B929B.toInt(), 0xFFFFFFFF.toInt()
    )
}

data class PixelPoint(val x: Int, val y: Int)
data class PixelSelection(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val normalizedLeft get() = minOf(left, right).coerceIn(0, 63)
    val normalizedRight get() = maxOf(left, right).coerceIn(0, 63)
    val normalizedTop get() = minOf(top, bottom).coerceIn(0, 63)
    val normalizedBottom get() = maxOf(top, bottom).coerceIn(0, 63)
    val width get() = normalizedRight - normalizedLeft + 1
    val height get() = normalizedBottom - normalizedTop + 1
}
data class PixelClipboard(val width: Int, val height: Int, val pixels: IntArray) {
    init { require(width in 1..64 && height in 1..64 && pixels.size == width * height) }
    fun pixelsCopy() = pixels.copyOf()
}

/** Operações puras: recebem uma camada e devolvem outra, sem alterar o original. */
object PixelDrawing {
    private fun valid(x: Int, y: Int) = x in 0 until 64 && y in 0 until 64

    fun setPixel(layer: PixelLayer, x: Int, y: Int, color: Int): PixelLayer {
        if (layer.locked || !valid(x, y)) return layer
        val next = layer.copyPixels()
        next[y * 64 + x] = color
        return layer.copy(pixels = next)
    }

    fun brush(layer: PixelLayer, x: Int, y: Int, color: Int, diameter: Int = 1): PixelLayer {
        if (layer.locked) return layer
        require(diameter in 1..16)
        val next = layer.copyPixels()
        val start = -(diameter / 2)
        for (dy in start until start + diameter) for (dx in start until start + diameter) {
            val px = x + dx; val py = y + dy
            if (valid(px, py)) next[py * 64 + px] = color
        }
        return layer.copy(pixels = next)
    }

    fun line(layer: PixelLayer, x0: Int, y0: Int, x1: Int, y1: Int, color: Int): PixelLayer {
        if (layer.locked) return layer
        val next = layer.copyPixels()
        var x = x0; var y = y0
        val dx = kotlin.math.abs(x1 - x0); val sx = if (x0 < x1) 1 else -1
        val dy = -kotlin.math.abs(y1 - y0); val sy = if (y0 < y1) 1 else -1
        var error = dx + dy
        while (true) {
            if (valid(x, y)) next[y * 64 + x] = color
            if (x == x1 && y == y1) break
            val twice = 2 * error
            if (twice >= dy) { error += dy; x += sx }
            if (twice <= dx) { error += dx; y += sy }
        }
        return layer.copy(pixels = next)
    }

    fun rectangle(layer: PixelLayer, x0: Int, y0: Int, x1: Int, y1: Int, color: Int, filled: Boolean = false): PixelLayer {
        if (layer.locked) return layer
        val left = minOf(x0, x1).coerceIn(0, 63); val right = maxOf(x0, x1).coerceIn(0, 63)
        val top = minOf(y0, y1).coerceIn(0, 63); val bottom = maxOf(y0, y1).coerceIn(0, 63)
        val next = layer.copyPixels()
        for (y in top..bottom) for (x in left..right) {
            if (filled || x == left || x == right || y == top || y == bottom) next[y * 64 + x] = color
        }
        return layer.copy(pixels = next)
    }

    fun fill(layer: PixelLayer, startX: Int, startY: Int, color: Int): PixelLayer {
        if (layer.locked || !valid(startX, startY)) return layer
        val start = startY * 64 + startX
        val target = layer.pixelAt(start)
        if (target == color) return layer
        val next = layer.copyPixels()
        val queue = IntArray(PixelDocument.PIXEL_COUNT)
        var head = 0; var tail = 0
        queue[tail++] = start; next[start] = color
        while (head < tail) {
            val p = queue[head++]
            val x = p % 64; val y = p / 64
            if (x > 0 && next[p - 1] == target) { next[p - 1] = color; queue[tail++] = p - 1 }
            if (x < 63 && next[p + 1] == target) { next[p + 1] = color; queue[tail++] = p + 1 }
            if (y > 0 && next[p - 64] == target) { next[p - 64] = color; queue[tail++] = p - 64 }
            if (y < 63 && next[p + 64] == target) { next[p + 64] = color; queue[tail++] = p + 64 }
        }
        return layer.copy(pixels = next)
    }

    fun mirrorHorizontal(layer: PixelLayer): PixelLayer {
        if (layer.locked) return layer
        val source = layer.copyPixels()
        val next = IntArray(PixelDocument.PIXEL_COUNT) { index ->
            val x = index % 64; val y = index / 64
            source[y * 64 + (63 - x)]
        }
        return layer.copy(pixels = next)
    }

    fun pick(layer: PixelLayer, x: Int, y: Int): Int = if (valid(x, y)) layer.pixelAt(y * 64 + x) else 0

    fun copyRegion(layer: PixelLayer, selection: PixelSelection): PixelClipboard {
        val l = selection.normalizedLeft; val t = selection.normalizedTop
        val w = selection.width; val h = selection.height
        val source = layer.copyPixels()
        val pixels = IntArray(w * h) { i -> source[(t + i / w) * 64 + l + i % w] }
        return PixelClipboard(w, h, pixels)
    }

    fun clearRegion(layer: PixelLayer, selection: PixelSelection): PixelLayer {
        if (layer.locked) return layer
        val next = layer.copyPixels()
        for (y in selection.normalizedTop..selection.normalizedBottom)
            for (x in selection.normalizedLeft..selection.normalizedRight) next[y * 64 + x] = 0
        return layer.copy(pixels = next)
    }

    fun pasteRegion(layer: PixelLayer, clipboard: PixelClipboard, x: Int, y: Int): PixelLayer {
        if (layer.locked) return layer
        val next = layer.copyPixels(); val source = clipboard.pixelsCopy()
        for (cy in 0 until clipboard.height) for (cx in 0 until clipboard.width) {
            val dx = x + cx; val dy = y + cy
            if (valid(dx, dy)) next[dy * 64 + dx] = source[cy * clipboard.width + cx]
        }
        return layer.copy(pixels = next)
    }
}

/** Histórico limitado: cada estado é uma cópia profunda e alterações novas limpam o refazer. */
class PixelHistory(private val limit: Int = 40, initial: PixelDocument) {
    private val undoStack = ArrayList<PixelDocument>()
    private val redoStack = ArrayList<PixelDocument>()
    var current: PixelDocument = initial.deepCopy()
        private set
    val canUndo get() = undoStack.isNotEmpty()
    val canRedo get() = redoStack.isNotEmpty()

    init { require(limit >= 1) }

    fun commit(next: PixelDocument): Boolean {
        if (next == current) return false
        undoStack.add(current.deepCopy())
        if (undoStack.size > limit) undoStack.removeAt(0)
        redoStack.clear()
        current = next.deepCopy()
        return true
    }

    fun undo(): PixelDocument? {
        if (!canUndo) return null
        redoStack.add(current.deepCopy())
        current = undoStack.removeAt(undoStack.lastIndex).deepCopy()
        return current.deepCopy()
    }

    fun redo(): PixelDocument? {
        if (!canRedo) return null
        undoStack.add(current.deepCopy())
        if (undoStack.size > limit) undoStack.removeAt(0)
        current = redoStack.removeAt(redoStack.lastIndex).deepCopy()
        return current.deepCopy()
    }
}

/** Converte coordenadas de tela para pixels sem interpolação. */
object PixelViewport {
    fun screenToPixel(
        x: Float, y: Float, left: Float, top: Float, displayedSize: Float
    ): PixelPoint? {
        if (displayedSize <= 0f || x < left || y < top || x >= left + displayedSize || y >= top + displayedSize) return null
        val px = ((x - left) / displayedSize * 64f).toInt().coerceIn(0, 63)
        val py = ((y - top) / displayedSize * 64f).toInt().coerceIn(0, 63)
        return PixelPoint(px, py)
    }
}

/** Formato binário versionado, compacto e validado por CRC32. */
object PixelProjectCodec {
    private const val MAGIC = 0x52504750 // RPGP
    private const val VERSION = 1
    private const val MAX_PAYLOAD_BYTES = 20 * 1024 * 1024

    fun encode(document: PixelDocument): ByteArray {
        val bodyBytes = ByteArrayOutputStream().use { buffer ->
            DataOutputStream(buffer).use { out ->
                out.writeUTF(document.id); out.writeUTF(document.name)
                out.writeInt(document.width); out.writeInt(document.height)
                out.writeInt(document.palette.size)
                document.palette.forEach(out::writeInt)
                out.writeInt(document.layers.size)
                document.layers.forEach { layer ->
                    out.writeUTF(layer.id); out.writeUTF(layer.name)
                    out.writeBoolean(layer.visible); out.writeBoolean(layer.locked)
                    layer.pixelsCopy().forEach(out::writeInt)
                }
            }
            buffer.toByteArray()
        }
        require(bodyBytes.size <= MAX_PAYLOAD_BYTES)
        val crc = CRC32().apply { update(bodyBytes) }.value
        return ByteArrayOutputStream().use { buffer ->
            DataOutputStream(buffer).use { out ->
                out.writeInt(MAGIC); out.writeInt(VERSION); out.writeInt(bodyBytes.size)
                out.write(bodyBytes); out.writeLong(crc)
            }
            buffer.toByteArray()
        }
    }

    fun decode(bytes: ByteArray): PixelDocument {
        require(bytes.size <= MAX_PAYLOAD_BYTES + 20) { "Arquivo de projeto muito grande." }
        DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            require(input.readInt() == MAGIC) { "Arquivo não reconhecido como projeto Pixel Studio." }
            val version = input.readInt()
            require(version == VERSION) { "Versão de projeto não suportada: $version." }
            val length = input.readInt()
            require(length in 1..MAX_PAYLOAD_BYTES && length == bytes.size - 20) { "Tamanho do arquivo inválido." }
            val body = ByteArray(length); input.readFully(body)
            val expectedCrc = input.readLong()
            val actualCrc = CRC32().apply { update(body) }.value
            require(expectedCrc == actualCrc) { "O arquivo está corrompido (verificação CRC falhou)." }
            DataInputStream(ByteArrayInputStream(body)).use { data ->
                val id = data.readUTF(); val name = data.readUTF()
                val width = data.readInt(); val height = data.readInt()
                require(width == 64 && height == 64) { "A V1 aceita apenas projetos 64x64." }
                val paletteCount = data.readInt()
                require(paletteCount in 1..PixelDocument.MAX_PALETTE_COLORS) { "Paleta inválida." }
                val palette = List(paletteCount) { data.readInt() }
                val layerCount = data.readInt()
                require(layerCount in 1..PixelDocument.MAX_LAYERS) { "Quantidade de camadas inválida." }
                val layers = List(layerCount) {
                    val layerId = data.readUTF(); val layerName = data.readUTF()
                    val visible = data.readBoolean(); val locked = data.readBoolean()
                    val pixels = IntArray(PixelDocument.PIXEL_COUNT) { data.readInt() }
                    PixelLayer(layerId, layerName, visible, locked, pixels)
                }
                require(data.available() == 0) { "Há dados extras ou inválidos no projeto." }
                return PixelDocument(id, name, width, height, layers, palette)
            }
        }
    }
}

/** Repositório local com gravação temporária, sincronização e cópia de recuperação. */
class PixelProjectRepository(private val directory: File) {
    init { if (!directory.exists() && !directory.mkdirs()) throw IllegalStateException("Não foi possível criar a pasta de projetos.") }

    @Synchronized
    fun save(document: PixelDocument): File {
        val target = File(directory, "${safeId(document.id)}.pixelproj")
        val temporary = File(directory, "${target.name}.tmp")
        val backup = File(directory, "${target.name}.bak")
        val bytes = PixelProjectCodec.encode(document)
        FileOutputStream(temporary).use { stream -> stream.write(bytes); stream.flush(); stream.fd.sync() }
        PixelProjectCodec.decode(temporary.readBytes()) // valida antes de tocar no arquivo anterior

        var movedOld = false
        try {
            if (target.exists()) {
                if (backup.exists() && !backup.delete()) throw IllegalStateException("Não foi possível atualizar o backup.")
                moveFile(target, backup)
                movedOld = true
            }
            moveFile(temporary, target)
        } catch (failure: Exception) {
            if (movedOld && !target.exists() && backup.exists()) runCatching { moveFile(backup, target) }
            throw IllegalStateException("Falha ao salvar o projeto; o arquivo anterior foi preservado quando possível.", failure)
        } finally {
            if (temporary.exists()) temporary.delete()
        }
        return target
    }

    fun load(file: File): PixelDocument {
        require(file.isFile && file.extension == "pixelproj") { "Arquivo de projeto inválido." }
        return PixelProjectCodec.decode(file.readBytes())
    }

    fun list(): List<File> = directory.listFiles { file -> file.isFile && file.extension == "pixelproj" }
        ?.sortedByDescending { it.lastModified() } ?: emptyList()

    fun backupFor(file: File): File? = File(directory, "${file.name}.bak").takeIf { it.isFile }

    fun restoreBackup(file: File): PixelDocument? {
        val backup = backupFor(file) ?: return null
        val document = PixelProjectCodec.decode(backup.readBytes())
        save(document)
        return document
    }

    fun delete(file: File) {
        require(file.parentFile?.canonicalFile == directory.canonicalFile) { "Arquivo fora da pasta de projetos." }
        if (file.exists() && !file.delete()) throw IllegalStateException("Não foi possível excluir o projeto.")
    }

    private fun safeId(value: String): String = value.replace(Regex("[^A-Za-z0-9_-]"), "_").take(80).ifBlank { UUID.randomUUID().toString() }

    private fun moveFile(source: File, target: File) {
        try {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            if (!source.renameTo(target)) throw IllegalStateException("Não foi possível mover ${source.name}.")
        }
    }
}

object PixelTemplates {
    fun humanoid(): PixelDocument {
        val p = IntArray(PixelDocument.PIXEL_COUNT)
        fun rect(x1: Int, y1: Int, x2: Int, y2: Int, color: Int) {
            for (y in y1..y2) for (x in x1..x2) p[y * 64 + x] = color
        }
        val outline = 0xFF211A18.toInt(); val skin = 0xFFD6A477.toInt()
        val cloth = 0xFF526078.toInt(); val boots = 0xFF382A27.toInt()
        rect(25, 7, 38, 21, outline); rect(27, 9, 36, 19, skin)
        rect(23, 20, 40, 40, outline); rect(25, 22, 38, 38, cloth)
        rect(17, 22, 24, 36, outline); rect(39, 22, 46, 36, outline)
        rect(19, 24, 22, 33, skin); rect(41, 24, 44, 33, skin)
        rect(24, 39, 31, 55, outline); rect(32, 39, 39, 55, outline)
        rect(26, 41, 29, 52, cloth); rect(34, 41, 37, 52, cloth)
        rect(22, 54, 31, 58, boots); rect(32, 54, 41, 58, boots)
        rect(29, 14, 30, 15, outline); rect(33, 14, 34, 15, outline)
        rect(25, 36, 38, 37, 0xFFD2AE68.toInt())
        return PixelDocument(name = "Base humanoide", layers = listOf(PixelLayer(name = "Corpo", pixels = p)))
    }
}
