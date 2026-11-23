package com.example.rpgsombrio.pixelstudio

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class PixelStudioCoreTest {
    @Test fun documentIsFixedAt64AndLayerPixelsAreDefensivelyCopied() {
        val source = IntArray(4096)
        val layer = PixelLayer(name = "Base", pixels = source)
        source[0] = -1
        assertEquals(0, layer.pixelsCopy()[0])
        val exported = layer.pixelsCopy(); exported[0] = -1
        assertEquals(0, layer.pixelsCopy()[0])
        assertThrows(IllegalArgumentException::class.java) { PixelDocument(width = 32) }
        assertThrows(IllegalArgumentException::class.java) { PixelLayer(name = "Inválida", pixels = IntArray(10)) }
    }

    @Test fun drawingOperationsDoNotMutateOriginalLayer() {
        val original = PixelLayer(name = "Base")
        val red = 0xFFFF0000.toInt()
        val edited = PixelDrawing.setPixel(original, 7, 9, red)
        assertEquals(red, edited.pixelsCopy()[9 * 64 + 7])
        assertEquals(0, original.pixelsCopy()[9 * 64 + 7])
        assertSame(original, PixelDrawing.setPixel(original, -1, 0, red))
    }

    @Test fun fillIsFourDirectionConnectedAndBounded() {
        val wall = 0xFF222222.toInt(); val blue = 0xFF2255AA.toInt()
        val pixels = IntArray(4096).also { p -> for (y in 0 until 64) p[y * 64 + 10] = wall }
        val layer = PixelLayer(name = "Base", pixels = pixels)
        val filled = PixelDrawing.fill(layer, 0, 0, blue)
        assertEquals(blue, filled.pixelsCopy()[0])
        assertEquals(wall, filled.pixelsCopy()[10])
        assertEquals(0, filled.pixelsCopy()[63])
    }

    @Test fun lineAndRectangleIncludeExpectedPixels() {
        val color = 0xFFFFFFFF.toInt(); val base = PixelLayer(name = "Base")
        val line = PixelDrawing.line(base, 2, 3, 8, 9, color)
        assertEquals(color, line.pixelsCopy()[3 * 64 + 2])
        assertEquals(color, line.pixelsCopy()[9 * 64 + 8])
        val outline = PixelDrawing.rectangle(base, 2, 3, 6, 7, color)
        assertEquals(color, outline.pixelsCopy()[3 * 64 + 2])
        assertEquals(0, outline.pixelsCopy()[5 * 64 + 4])
        val filled = PixelDrawing.rectangle(base, 2, 3, 6, 7, color, filled = true)
        assertEquals(color, filled.pixelsCopy()[5 * 64 + 4])
    }

    @Test fun brushMirrorAndEyedropperWork() {
        val color = 0xFFCC8844.toInt()
        val brush = PixelDrawing.brush(PixelLayer(name = "Base"), 10, 10, color, diameter = 3)
        assertEquals(9, brush.pixelsCopy().count { it == color })
        val mirrored = PixelDrawing.mirrorHorizontal(PixelDrawing.setPixel(brush, 3, 4, color))
        assertEquals(color, PixelDrawing.pick(mirrored, 60, 4))
        assertEquals(0, PixelDrawing.pick(mirrored, -1, 4))
    }

    @Test fun lockedLayerCannotBeEdited() {
        val layer = PixelLayer(name = "Bloqueada", locked = true)
        assertSame(layer, PixelDrawing.setPixel(layer, 1, 1, -1))
        assertSame(layer, PixelDrawing.fill(layer, 0, 0, -1))
    }

    @Test fun selectionCopyClearAndPasteNormalizeCoordinates() {
        val red = 0xFFFF0000.toInt()
        val layer = PixelDrawing.setPixel(PixelLayer(name = "Base"), 2, 3, red)
        val selection = PixelSelection(4, 5, 2, 3)
        val clip = PixelDrawing.copyRegion(layer, selection)
        assertEquals(3, clip.width); assertEquals(3, clip.height)
        val cleared = PixelDrawing.clearRegion(layer, selection)
        assertEquals(0, cleared.pixelsCopy()[3 * 64 + 2])
        val pasted = PixelDrawing.pasteRegion(cleared, clip, 10, 10)
        assertEquals(red, pasted.pixelsCopy()[10 * 64 + 10])
    }

    @Test fun compositeRespectsVisibilityAndAlpha() {
        val red = 0xFFFF0000.toInt()
        val halfBlue = 0x800000FF.toInt()
        val back = PixelLayer(name = "Fundo", pixels = IntArray(4096).also { it[0] = red })
        val front = PixelLayer(name = "Frente", pixels = IntArray(4096).also { it[0] = halfBlue })
        val result = PixelDocument(layers = listOf(back, front)).composite()[0]
        assertEquals(255, result ushr 24 and 0xFF)
        assertTrue((result ushr 16 and 0xFF) in 126..129)
        assertTrue((result and 0xFF) in 126..129)
        assertEquals(red, PixelDocument(layers = listOf(back, front.copy(visible = false))).composite()[0])
    }

    @Test fun undoRedoStoresWholeDocumentsAndClearsRedoAfterNewEdit() {
        val first = PixelDocument(name = "Teste")
        val history = PixelHistory(limit = 2, initial = first)
        val red = 0xFFFF0000.toInt()
        val layer = PixelDrawing.setPixel(first.layers.first(), 1, 1, red)
        val second = first.copy(layers = listOf(layer))
        assertTrue(history.commit(second))
        assertTrue(history.canUndo)
        assertEquals(first, history.undo())
        assertTrue(history.canRedo)
        assertEquals(second, history.redo())
        history.undo()
        history.commit(first.copy(name = "Outro"))
        assertFalse(history.canRedo)
    }

    @Test fun viewportMapsScreenCoordinatesTo64Grid() {
        assertEquals(PixelPoint(0, 0), PixelViewport.screenToPixel(10f, 20f, 10f, 20f, 640f))
        assertEquals(PixelPoint(63, 63), PixelViewport.screenToPixel(649.9f, 659.9f, 10f, 20f, 640f))
        assertNull(PixelViewport.screenToPixel(650f, 20f, 10f, 20f, 640f))
    }

    @Test fun projectCodecRoundTripsAllLayersAndFlagsAndDetectsCorruption() {
        val pixels = IntArray(4096).also { it[123] = 0xFFABCDEF.toInt() }
        val layer = PixelLayer(name = "Armadura", visible = false, locked = true, pixels = pixels)
        val doc = PixelDocument(name = "Cavaleiro", layers = listOf(layer), palette = listOf(0xFF123456.toInt()))
        val encoded = PixelProjectCodec.encode(doc)
        assertEquals(doc, PixelProjectCodec.decode(encoded))
        encoded[encoded.lastIndex - 9] = (encoded[encoded.lastIndex - 9].toInt() xor 0x01).toByte()
        assertThrows(IllegalArgumentException::class.java) { PixelProjectCodec.decode(encoded) }
    }

    @Test fun repositoryWritesValidProjectsAndKeepsRecoveryBackup() {
        val directory = java.nio.file.Files.createTempDirectory("pixel-studio-test").toFile()
        try {
            val repository = PixelProjectRepository(directory)
            val first = PixelDocument(id = "project-1", name = "Primeiro")
            val file = repository.save(first)
            assertEquals(first, repository.load(file))
            val second = first.copy(name = "Segundo")
            repository.save(second)
            assertEquals(second, repository.load(file))
            val backup = repository.backupFor(file)
            assertNotNull(backup)
            assertEquals(first, PixelProjectCodec.decode(backup!!.readBytes()))
            assertEquals(1, repository.list().size)
        } finally { directory.deleteRecursively() }
    }

    @Test fun humanoidTemplateHasPixelsAndUses64By64() {
        val template = PixelTemplates.humanoid()
        assertEquals(64, template.width); assertEquals(64, template.height)
        assertTrue(template.composite().any { it != 0 })
    }
}
